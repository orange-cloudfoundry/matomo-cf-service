/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.web.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.orange.oss.matomocfservice.api.model.MatomoInstance;
import com.orange.oss.matomocfservice.api.model.OpCode;
import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgr;
import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgr.AppConfHolder;
import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgrProperties;
import com.orange.oss.matomocfservice.cfmgr.MatomoReleases;
import com.orange.oss.matomocfservice.config.ApplicationConfiguration;
import com.orange.oss.matomocfservice.config.ServiceCatalogConfiguration;
import com.orange.oss.matomocfservice.web.domain.PMatomoInstance;
import com.orange.oss.matomocfservice.web.domain.PPlatform;
import com.orange.oss.matomocfservice.web.repository.PMatomoInstanceRepository;
import com.orange.oss.matomocfservice.web.repository.PPlatformRepository;

import reactor.core.scheduler.Schedulers;

/**
 * @author P. Déchamboux
 *
 */
@Service
public class MatomoInstanceService {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private PPlatformRepository pfRepo;
	@Autowired
	private PMatomoInstanceRepository miRepo;
	@Autowired
	private PlatformService platformService;
	@Autowired
	private CloudFoundryMgr cfMgr;
	@Autowired
	private InstanceIdMgr instanceIdMgr;
	@Autowired
	private CloudFoundryMgrProperties properties;
	@Autowired
	private MatomoReleases matomoReleases;
	@Autowired
	private ApplicationConfiguration appConf;

	/**
	 * Initialize CF manager and especially create the shared database for the dev flavor of
	 * Matomo service instances.
	 */
	public void initialize() {
		LOGGER.debug("SERV::MatomoInstanceService:initialize");
		for (PMatomoInstance pmi : miRepo.findAll()) {
			if (pmi.getConfigFileContent() != null) {
				LOGGER.debug("SERV::initialize: reactivate instance {}", pmi.getIdUrlStr());
				matomoReleases.activateVersionPath(pmi.getIdUrlStr(), pmi.getInstalledVersion());
				matomoReleases.setConfigIni(pmi.getIdUrlStr(), pmi.getInstalledVersion(), pmi.getConfigFileContent());
			}
		}
	}

	public MatomoInstance createMatomoInstance(MatomoInstance matomoInstance) {
		LOGGER.debug("SERV::createMatomoInstance: matomoInstance={}", matomoInstance.toString());
		String instversion = matomoReleases.getDefaultReleaseName();
		PPlatform ppf = getPPlatform(matomoInstance.getPlatformId());
		for (PMatomoInstance pmi : miRepo.findByPlatformAndLastOperation(ppf, OpCode.DELETE.toString())) {
			if (pmi.getId().equals(matomoInstance.getUuid())) {
				throw new EntityExistsException("Matomo Instance with ID=" + matomoInstance.getUuid() + " already exists in Platform with ID=" + matomoInstance.getPlatformId());
			}
		}
		PMatomoInstance pmi = new PMatomoInstance(
				matomoInstance.getUuid(),
				instanceIdMgr.allocateInstanceId(),
				matomoInstance.getServiceDefinitionId(),
				matomoInstance.getName(),
				matomoInstance.getPlatformKind(),
				matomoInstance.getPlatformApiLocation(),
				matomoInstance.getPlanId(),
				ppf,
				instversion);
		savePMatomoInstance(pmi);
		if (pmi.getPlanId().equals(ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID)) {
			cfMgr.deployMatomoCfAppBindToGlobalSharedDb(pmi.getIdUrlStr(), instversion, pmi.getId())
			.doOnError(t -> {
				LOGGER.debug("Async create app instance (phase 1) \"" + pmi.getId() + "\" failed -> " + t.getMessage());
				pmi.setLastOperation(OpCode.CREATE);
				pmi.setLastOperationState(OperationState.FAILED);
				savePMatomoInstance(pmi);
			})
			.doOnSuccess(vv -> {
				LOGGER.debug("Async create app instance (phase 1) \"" + pmi.getId() + "\" succeeded");
				initializeMatomoInstance(pmi.getIdUrlStr(), pmi.getId());
				cfMgr.getInstanceConfigFile(pmi.getIdUrlStr(), instversion)
				.doOnError(t -> {})
				.doOnSuccess(ach -> {
					pmi.setConfigFileContent(ach.fileContent);
					cfMgr.deployMatomoCfAppBindToGlobalSharedDb(pmi.getIdUrlStr(), instversion, pmi.getId())
					.doOnError(t -> {
						LOGGER.debug("Async create app instance (phase 2) \"" + pmi.getId() + "\" failed -> " + t.getMessage());
						pmi.setLastOperation(OpCode.CREATE);
						pmi.setLastOperationState(OperationState.FAILED);
						savePMatomoInstance(pmi);
					})
					.doOnSuccess(vvv -> {
						LOGGER.debug("Async create app instance (phase 2) \"" + pmi.getId() + "\" succeeded");
						pmi.setLastOperation(OpCode.CREATE);
						pmi.setLastOperationState(OperationState.SUCCEEDED);
						savePMatomoInstance(pmi);
					})
					.subscribe();
				})
				.subscribe();
			})
			.subscribe();
		} else if (pmi.getPlanId().equals(ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID)) {
			// TODO
		} else if (pmi.getPlanId().equals(ServiceCatalogConfiguration.PLANDEDICATEDDB_UUID)) {
			// TODO
		} else {
			LOGGER.error("SERV::createMatomoInstance: unknown plan=" + pmi.getPlanId());
			throw new IllegalArgumentException("Unkown plan when creating service instance");
		}
		return toApiModel(pmi);
	}

	public MatomoInstance getMatomoInstance(String platformId, String instanceId) {
		LOGGER.debug("SERV::getMatomoInstance: platformId={} instanceId={}", platformId, instanceId);
		PPlatform ppf = getPPlatform(platformId);
		Optional<PMatomoInstance> opmi = miRepo.findById(instanceId);
		if (! opmi.isPresent()) {
			throw new EntityNotFoundException("Matomo Instance with ID=" + instanceId + " not known in Platform with ID=" + platformId);
		}
		PMatomoInstance pmi = opmi.get();
		if (pmi.getPlatform() != ppf) {
			throw new IllegalArgumentException("Wrong platform with ID=" + platformId + " for Service Instance with ID=" + instanceId);
		}
		MatomoInstance mi = toApiModel(pmi);
		return mi;
	}

	public void fillLastOperationAndState(String platformId, String instanceId, OperationAndState opandst2fill) {
		LOGGER.debug("SERV::getLastOperationAndState: platformId={} instanceId={}", platformId, instanceId);
		PPlatform ppf = getPPlatform(platformId);
		Optional<PMatomoInstance> opmi = miRepo.findById(instanceId);
		if (! opmi.isPresent()) {
			throw new EntityNotFoundException("Matomo Instance with ID=" + instanceId + " not known in Platform with ID=" + platformId);
		}
		PMatomoInstance pmi = opmi.get();
		if (pmi.getPlatform() != ppf) {
			throw new IllegalArgumentException("Wrong platform with ID=" + platformId + " for Service Instance with ID=" + instanceId);
		}
		opandst2fill.setOperation(pmi.getLastOperation());
		opandst2fill.setState(pmi.getLastOperationState());
	}

	public List<MatomoInstance> findMatomoInstance(String platformId) {
		LOGGER.debug("SERV::findMatomoInstance: platformId={}", platformId);
		List<MatomoInstance> instances = new ArrayList<MatomoInstance>();
		for (PMatomoInstance pmi : miRepo.findByPlatform(getPPlatform(platformId))) {
			instances.add(toApiModel(pmi));
		}
		return instances;
	}

	public MatomoInstance deleteMatomoInstance(String platformId, String instanceId) {
		LOGGER.debug("SERV::deleteMatomoInstance: platformId={} instanceId={}", platformId, instanceId);
		PPlatform ppf = getPPlatform(platformId);
		Optional<PMatomoInstance> opmi = miRepo.findById(instanceId);
		if (!opmi.isPresent()) {
			return new MatomoInstance().uuid("").name("").planId("").tenantId("").subtenantId("");
		}
		PMatomoInstance pmi = opmi.get();
		if (pmi.getPlatform() != ppf) {
			throw new IllegalArgumentException("Wrong platform with ID=" + platformId + " for Service Instance with ID=" + instanceId);
		}
		if (pmi.getLastOperationState() == OperationState.IN_PROGRESS) {
			throw new RuntimeException("Cannot delete instance with ID=" + pmi.getId() + ": operation already in progress");
		}
		if (pmi.getPlanId().equals(ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID)) {
			cfMgr.deleteMatomoCfAppBindToGlobalSharedDb(pmi.getIdUrlStr())
			.doOnError(t -> {
				LOGGER.debug("Async delete app instance \"" + pmi.getId() + "\" failed -> " + t.getMessage());
				pmi.setLastOperation(OpCode.DELETE);
				pmi.setLastOperationState(OperationState.FAILED);
				savePMatomoInstance(pmi);
			})
			.doOnSuccess(v -> {
				LOGGER.debug("Async delete app instance \"" + pmi.getId() + "\" succeeded");
				pmi.setLastOperation(OpCode.DELETE);
				pmi.setLastOperationState(OperationState.SUCCEEDED);
				instanceIdMgr.freeInstanceId(pmi.getIdUrl());
				deleteAssociatedDbSchema(pmi.getIdUrlStr());
				savePMatomoInstance(pmi);
			})
			.subscribe();
		} else if (pmi.getPlanId().equals(ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID)) {
			// TODO
		} else if (pmi.getPlanId().equals(ServiceCatalogConfiguration.PLANDEDICATEDDB_UUID)) {
			// TODO
		} else {
			LOGGER.error("SERV::deleteMatomoInstance: unknown plan=" + pmi.getPlanId());
			throw new IllegalArgumentException("Unkown plan when deleting service instance");
		}
		return toApiModel(pmi);
	}

	public MatomoInstance updateMatomoInstance(MatomoInstance mi) {
		LOGGER.debug("SERV::updateMatomoInstance: platformId={} instanceId={} matomoInstance={}", mi.getPlatformId(), mi.getUuid());
		// TODO: not implemented
		throw new UnsupportedOperationException("Update of Matomo instance is not currently supported");
	}

	public String getDashboardUrl(PMatomoInstance pmi) {
		return cfMgr.getInstanceUrl(pmi.getIdUrlStr(), pmi.getId());
	}

	private MatomoInstance toApiModel(PMatomoInstance pmi) {
		return new MatomoInstance()
				.uuid(pmi.getId())
				.serviceDefinitionId(pmi.getServiceDefinitionId())
				.name(pmi.getName())
				.createTime(pmi.getCreateTime().format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
				.updateTime(pmi.getUpdateTime().format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
				.platformKind(pmi.getPlatformKind())
				.platformApiLocation(pmi.getPlatformApiLocation())
				.planId(pmi.getPlanId())
				.lastOperation(pmi.getLastOperation())
				.lastOperationState(pmi.getLastOperationState().getValue())
				.matomoVersion(pmi.getInstalledVersion())
				.dashboardUrl(getDashboardUrl(pmi));
	}

	private PPlatform getPPlatform(String platformId) {
		String id = platformId == null ? platformService.getUnknownPlatformId() : platformId;
		Optional<PPlatform> oppf = pfRepo.findById(id);
		if (oppf.isPresent()) {
			return oppf.get();
		}
		throw new EntityNotFoundException("Platform with ID=" + id + " not known");
	}

	private MatomoInstance savePMatomoInstance(PMatomoInstance pmi) {
		MatomoInstance mi = toApiModel(pmi);
		miRepo.save(pmi);
		return mi;
	}

	public static class OperationAndState {
		private OpCode opCode;
		private OperationState opState;

		public OperationAndState() {
			opCode = null;
			opState = null;
		}

		public OpCode getOperation() {
			return opCode;
		}

		public String getOperationMessage() {
			if (opCode.equals(OpCode.CREATE)) {
				return "Create Matomo Service Instance";
			} else if (opCode.equals(OpCode.READ)) {
				return "Read Matomo Service Instance";
			} else if (opCode.equals(OpCode.UPDATE)) {
				return "Update Matomo Service Instance";
			} else {
				return "Delete Matomo Service Instance";
			}
		}

		void setOperation(OpCode opCode) {
			this.opCode = opCode;
		}

		public OperationState getState() {
			return opState;
		}

		void setState(OperationState opState) {
			this.opState = opState;
		}
	}

	private void initializeMatomoInstance(String appcode, String nuri) {
		LOGGER.debug("SERV::initializeMatomoInstance: appCode={}", appcode);
		RestTemplate restTemplate = new RestTemplate();
		try {
			URI uri = new URI("https://" + nuri + "." + properties.getDomain()), calluri;
			String res = restTemplate.getForObject(calluri = uri, String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			res = restTemplate.getForObject(calluri = URI.create(uri.toString() + "/index.php?action=systemCheck"), String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			res = restTemplate.getForObject(calluri = URI.create(uri.toString() + "/index.php?action=databaseSetup"), String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			Document d = Jsoup.parse(res);
			MultipartBodyBuilder mbb = new MultipartBodyBuilder();
			mbb.part("type", d.getElementById("type-0").attr("value"));
			mbb.part("host", d.getElementById("host-0").attr("value"));
			mbb.part("username", d.getElementById("username-0").attr("value"));
			mbb.part("password", d.getElementById("password-0").attr("value"));
			mbb.part("dbname", d.getElementById("dbname-0").attr("value"));
			mbb.part("tables_prefix", cfMgr.getAppUrlPrefix(appcode) + "_");
			mbb.part("adapter", "PDO\\MYSQL");
			mbb.part("submit", "Suivant+%C2%BB");
			res = restTemplate.postForObject(calluri = URI.create(uri.toString() + "/index.php?action=databaseSetup"),
					mbb.build(),
					String.class);
			LOGGER.debug("After POST on <{}>", calluri.toString());
			res = restTemplate.getForObject(calluri = URI.create(uri.toString() + "/index.php?action=tablesCreation&module=Installation"), String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			res = restTemplate.getForObject(calluri = URI.create(uri.toString() + "/index.php?action=setupSuperUser&module=Installation"), String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			mbb = new MultipartBodyBuilder();
			mbb.part("login", "piwik");
			mbb.part("password", "piwikpw");
			mbb.part("password_bis", "piwikpw");
			mbb.part("email", "piwik@orange.com");
			mbb.part("subscribe_newsletter_piwikorg", "0");
			mbb.part("subscribe_newsletter_professionalservices", "0");
			mbb.part("submit", "Suivant+%C2%BB");
			res = restTemplate.postForObject(calluri = URI.create(uri.toString() + "/index.php?action=setupSuperUser&module=Installation"),
					mbb.build(),
					String.class);
			LOGGER.debug("After POST on <{}>", calluri.toString());
			mbb = new MultipartBodyBuilder();
			mbb.part("siteName", cfMgr.getAppUrlPrefix(appcode));
			mbb.part("url", uri.toASCIIString());
			mbb.part("timezone", "Europe/Paris");
			mbb.part("ecommerce", "0");
			mbb.part("submit", "Suivant+%C2%BB");
			res = restTemplate.postForObject(calluri = URI.create(uri.toString() + "/index.php?action=firstWebsiteSetup&module=Installation"),
					mbb.build(),
					String.class);
			LOGGER.debug("After POST on <{}>", calluri.toString());
			res = restTemplate.getForObject(calluri = URI.create(uri.toString() + "/index.php?action=finished"
					+ "&clientProtocol=https"
					+ "&module=Installation"
					+ "&site_idSite=4"
					+ "&site_name=" + cfMgr.getAppUrlPrefix(appcode)), String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			mbb = new MultipartBodyBuilder();
			mbb.part("do_not_track", "1");
			mbb.part("anonymise_ip", "1");
			mbb.part("submit", "Continuer+vers+Matomo+%C2%BB");
			res = restTemplate.postForObject(calluri = URI.create(uri.toString() + "/index.php?action=finished&clientProtocol=https&module=Installation&site_idSite=4&site_name=" + cfMgr.getAppUrlPrefix(appcode)),
					mbb.build(),
					String.class);
			LOGGER.debug("After POST on <{}>", calluri.toString());
			res = restTemplate.getForObject(calluri = uri, String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
		} catch (RestClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void deleteAssociatedDbSchema(String appcode) {
		LOGGER.debug("SERV::initializeMatomoInstance: appCode={}", appcode);
	}
}
