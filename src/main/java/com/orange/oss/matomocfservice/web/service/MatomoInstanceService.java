/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.web.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.client.RestTemplate;

import com.orange.oss.matomocfservice.api.model.MatomoInstance;
import com.orange.oss.matomocfservice.api.model.OpCode;
import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgr;
import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgrProperties;
import com.orange.oss.matomocfservice.cfmgr.MatomoReleases;
import com.orange.oss.matomocfservice.config.ServiceCatalogConfiguration;
import com.orange.oss.matomocfservice.web.domain.PMatomoInstance;
import com.orange.oss.matomocfservice.web.domain.PPlatform;
import com.orange.oss.matomocfservice.web.repository.PMatomoInstanceRepository;

/**
 * @author P. Déchamboux
 *
 */
@Service
public class MatomoInstanceService extends OperationStatusService {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	private final String MATOMOINSTANCE_ROOTUSER = "admin";
	@Autowired
	private PMatomoInstanceRepository miRepo;
	@Autowired
	private CloudFoundryMgr cfMgr;
	@Autowired
	private InstanceIdMgr instanceIdMgr;
	@Autowired
	private CloudFoundryMgrProperties properties;
	@Autowired
	private MatomoReleases matomoReleases;

	/**
	 * Initialize Matomo service instance manager.
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

	@SuppressWarnings("unchecked")
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
				t.printStackTrace();
				pmi.setLastOperation(OpCode.CREATE);
				pmi.setLastOperationState(OperationState.FAILED);
				savePMatomoInstance(pmi);
			})
			.doOnSuccess(vv -> {
				LOGGER.debug("Async create app instance (phase 1) \"" + pmi.getId() + "\" succeeded");
				initializeMatomoInstance(pmi.getIdUrlStr(), pmi.getId(), pmi.getPassword(), matomoInstance.getName());
				cfMgr.getInstanceConfigFile(pmi.getIdUrlStr(), instversion)
				.doOnError(t -> {})
				.doOnSuccess(ach -> {
					pmi.setConfigFileContent(ach.fileContent);
					cfMgr.deployMatomoCfAppBindToGlobalSharedDb(pmi.getIdUrlStr(), instversion, pmi.getId())
					.doOnError(t -> {
						LOGGER.debug("Async create app instance (phase 2.1) \"" + pmi.getId() + "\" failed -> " + t.getMessage());
						pmi.setLastOperation(OpCode.CREATE);
						pmi.setLastOperationState(OperationState.FAILED);
						savePMatomoInstance(pmi);
					})
					.doOnSuccess(vvv -> {
						LOGGER.debug("Get Matomo Instance API Credentials");
						cfMgr.getApplicationEnv(pmi.getIdUrlStr())
						.doOnError(t -> {
							LOGGER.debug("Async create app instance (phase 2.2) \"" + pmi.getId() + "\" failed -> " + t.getMessage());
						})
						.doOnSuccess(env -> {
							// dirty: fetch token_auth from the database as I can't succeed to do it with the API :-( 
							Map<String, Object> jres = (Map<String, Object>) env.get("VCAP_SERVICES");
							pmi.setDbCred((String) ((Map<String, Object>) ((Map<String, Object>) ((List<Object>) ((Map<String, Object>) jres).get("p-mysql")).get(0)).get("credentials")).get("jdbcUrl"));
					        Connection conn = null;
					        Statement stmt = null;
							try {
								Class.forName("org.mariadb.jdbc.Driver");
								conn = DriverManager.getConnection(pmi.getDbCred());
								stmt = conn.createStatement();
								stmt.execute("SELECT token_auth FROM " + cfMgr.getAppUrlPrefix(pmi.getIdUrlStr()) + "_user WHERE login='" + MATOMOINSTANCE_ROOTUSER + "'");
								if (! stmt.getResultSet().first()) {
									throw new RuntimeException("Cannot retrieve the credentials of the admin user.");
								}
								pmi.setTokenAuth(stmt.getResultSet().getString(1));
							} catch (ClassNotFoundException e) {
								throw new RuntimeException("Cannot retrieve the credentials of the admin user.", e);
							} catch (SQLException e) {
								throw new RuntimeException("Cannot retrieve the credentials of the admin user.", e);
							} finally {
								try {
									if (stmt != null)  {
										stmt.close();
									}
									if (conn != null) {
										conn.close();
									}
								} catch (SQLException e) {
									throw new RuntimeException("Cannot retrieve the credentials of the admin user.", e);
								}
							}
							pmi.setLastOperation(OpCode.CREATE);
							pmi.setLastOperationState(OperationState.SUCCEEDED);
							savePMatomoInstance(pmi);
							LOGGER.debug("Async create app instance (phase 2) \"" + pmi.getId() + "\" succeeded");
						}).subscribe();
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

	public List<MatomoInstance> findMatomoInstance(String platformId) {
		LOGGER.debug("SERV::findMatomoInstance: platformId={}", platformId);
		List<MatomoInstance> instances = new ArrayList<MatomoInstance>();
		for (PMatomoInstance pmi : miRepo.findByPlatform(getPPlatform(platformId))) {
			instances.add(toApiModel(pmi));
		}
		return instances;
	}

	public String deleteMatomoInstance(String platformId, String instanceId) {
		LOGGER.debug("SERV::deleteMatomoInstance: platformId={} instanceId={}", platformId, instanceId);
		PPlatform ppf = getPPlatform(platformId);
		Optional<PMatomoInstance> opmi = miRepo.findById(instanceId);
		if (!opmi.isPresent()) {
			return "Error: Matomo service instance does not exist.";
		}
		PMatomoInstance pmi = opmi.get();
		if (pmi.getPlatform() != ppf) {
			return "Error: wrong platform with ID=" + platformId + " for Matomo service instance with ID=" + instanceId + ".";
		}
		if (pmi.getLastOperationState() == OperationState.IN_PROGRESS) {
			return "Error: cannot delete Matomo service instance with ID=" + pmi.getId() + ": operation already in progress.";
		}
		pmi.setLastOperation(OpCode.DELETE);
		pmi.setLastOperationState(OperationState.IN_PROGRESS);
		savePMatomoInstance(pmi);
		deleteAssociatedDbSchema(pmi);
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
				savePMatomoInstance(pmi);
			})
			.subscribe();
		} else if (pmi.getPlanId().equals(ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID)) {
			// TODO
		} else if (pmi.getPlanId().equals(ServiceCatalogConfiguration.PLANDEDICATEDDB_UUID)) {
			// TODO
		} else {
			LOGGER.error("SERV::deleteMatomoInstance: unknown plan=" + pmi.getPlanId());
			return "Error: unkown plan when deleting Matomo service instance.";
		}
		return null;
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

	private MatomoInstance savePMatomoInstance(PMatomoInstance pmi) {
		MatomoInstance mi = toApiModel(pmi);
		miRepo.save(pmi);
		return mi;
	}

	private void initializeMatomoInstance(String appcode, String nuri, String pwd, String sname) {
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
			mbb.part("login", MATOMOINSTANCE_ROOTUSER);
			mbb.part("password", pwd);
			mbb.part("password_bis", pwd);
			mbb.part("email", "piwik@orange.com");
			mbb.part("subscribe_newsletter_piwikorg", "0");
			mbb.part("subscribe_newsletter_professionalservices", "0");
			mbb.part("submit", "Suivant+%C2%BB");
			res = restTemplate.postForObject(calluri = URI.create(uri.toString() + "/index.php?action=setupSuperUser&module=Installation"),
					mbb.build(),
					String.class);
			LOGGER.debug("After POST on <{}>", calluri.toString());
			mbb = new MultipartBodyBuilder();
			mbb.part("siteName", sname);
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
		} catch (URISyntaxException e) {
			throw new RuntimeException("Fail to initialize new Matomo instance.", e);
		}
	}

	/**
	 * Retrieve token_auth through Matomo API -> failed to make it effective
	 * @param appcode	Service instance internal app code
	 * @param instid	GUID for the service instance
	 * @param pwd		Password for instance admin
	 * @return	The token to enable API access to this Matomo instance
	 */
//	private String getApiAccessToken(String appcode, String instid, String pwd) {
//		String ta = null;
//		LOGGER.debug("SERV::initializeApiAccess: appCode={}, instId={}, pwd={}", appcode, instid, pwd);
//		try {
//			RestTemplate restTemplate = new RestTemplate();
//			URI uri = new URI("https://" + instid + "." + properties.getDomain()), calluri;
//			MultipartBodyBuilder mbb = new MultipartBodyBuilder();
//			mbb.part("module", "API");
//			mbb.part("method", "UsersManager.getTokenAuth");
//			mbb.part("userLogin", MATOMOINSTANCE_ROOTUSER);
//			String md5pw = DatatypeConverter.printHexBinary(MessageDigest.getInstance("MD5").digest(pwd.getBytes("UTF-8")));
//			LOGGER.debug("MD5 PW: " + md5pw);
//			mbb.part("md5Password", md5pw);
//			mbb.part("format", "json");
//			String res = restTemplate.postForObject(calluri = URI.create(uri.toString() + "/index.php"),
//					mbb.build(),
//					String.class);
//			LOGGER.debug("After POST on <{}>", calluri.toString());
//			LOGGER.debug("RES -> " + res);
//			JSONObject jres = new JSONObject(res);
//			ta = (String) jres.get("value");
////			Document d = Jsoup.parse(res);
////			ta = d.getElementsByTag("result").first().text();
//			LOGGER.debug("Token_auth=" + ta);
//		} catch (URISyntaxException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (NoSuchAlgorithmException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return ta;
//	}

	private void deleteAssociatedDbSchema(PMatomoInstance pmi) {
		LOGGER.debug("SERV::deleteAssociatedDbSchema: appCode={}", pmi.getIdUrlStr());
        Connection conn = null;
        Statement stmt = null;
		try {
			Class.forName("org.mariadb.jdbc.Driver");
			conn = DriverManager.getConnection(pmi.getDbCred());
			DatabaseMetaData m = conn.getMetaData();
			ResultSet tables = m.getTables(null, null, "%", null);
			while (tables.next()) {
				if (! tables.getString(3).startsWith(cfMgr.getAppUrlPrefix(pmi.getIdUrlStr()))) {
					continue;
				}
				stmt = conn.createStatement();
				stmt.execute("DROP TABLE " + tables.getString(3));
				stmt.close();
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Cannot remove tables of deleted Matomo service instance.", e);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot remove tables of deleted Matomo service instance.", e);
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException e) {
				throw new RuntimeException("Cannot remove tables of deleted Matomo service instance.", e);
			}
		}

	}
}
