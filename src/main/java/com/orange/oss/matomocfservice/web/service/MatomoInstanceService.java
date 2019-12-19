/*
 * Copyright 2019 Orange and the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

//import com.orange.oss.matomocfservice.api.model.MatomoInstance;
//import com.orange.oss.matomocfservice.api.model.MiParameters;
//import com.orange.oss.matomocfservice.api.model.OpCode;
import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgr;
import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgrProperties;
import com.orange.oss.matomocfservice.servicebroker.ServiceCatalogConfiguration;
import com.orange.oss.matomocfservice.web.domain.PMatomoInstance;
import com.orange.oss.matomocfservice.web.domain.PMatomoInstance.PlatformKind;
import com.orange.oss.matomocfservice.web.domain.POperationStatus;
import com.orange.oss.matomocfservice.web.domain.PPlatform;
import com.orange.oss.matomocfservice.web.domain.Parameters;
import com.orange.oss.matomocfservice.web.repository.PMatomoInstanceRepository;

import reactor.core.publisher.Mono;

/**
 * @author P. Déchamboux
 *
 */
@Service
public class MatomoInstanceService extends OperationStatusService {
	private final static Logger LOGGER = LoggerFactory.getLogger(MatomoInstanceService.class);
	private final static String MATOMOINSTANCE_ROOTUSER = "admin";
	private final static String NOTINSTALLED = "NOT_INST";
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
		LOGGER.debug("SERV::MatomoInstanceService:initialize - latestVersion={}", matomoReleases.getLatestReleaseName());
		for (PMatomoInstance pmi : miRepo.findAll()) {
			if (!pmi.getAutomaticVersionUpgrade()) {
				continue;
			}
			if (pmi.getConfigFileContent() != null) {
				LOGGER.debug("SERV::initialize: reactivate instance {}, version={}", pmi.getIdUrlStr(), pmi.getInstalledVersion());
				if (matomoReleases.isHigherVersion(matomoReleases.getLatestReleaseName(), pmi.getInstalledVersion())) {
					// need to upgrade to latest version
					LOGGER.debug("Upgrade Matomo instance from {} to {}.", pmi.getInstalledVersion(), matomoReleases.getLatestReleaseName());
					updateMatomoInstanceActual(pmi, new Parameters()
							.autoVersionUpgrade(pmi.getAutomaticVersionUpgrade())
							.cfInstances(pmi.getInstances())
							.memorySize(pmi.getMemorySize())
							.timeZone(null)
							.version(matomoReleases.getLatestReleaseName()));
				}
			}
		}
	}

	public PMatomoInstance getMatomoInstance(String platformId, String instanceId) {
		LOGGER.debug("SERV::getMatomoInstance: platformId={} instanceId={}", platformId, instanceId);
		PPlatform ppf = getPPlatform(platformId);
		Optional<PMatomoInstance> opmi = miRepo.findById(instanceId);
		if (! opmi.isPresent()) {
			LOGGER.error("Matomo Instance with ID=" + instanceId + " not known in Platform with ID=" + platformId);
			return null;
		}
		PMatomoInstance pmi = opmi.get();
		if (pmi.getPlatform() != ppf) {
			LOGGER.error("Wrong platform with ID=" + platformId + " for Service Instance with ID=" + instanceId);
			return null;
		}
		return pmi;
	}

	public List<PMatomoInstance> findMatomoInstance(String platformId) {
		LOGGER.debug("SERV::findMatomoInstance: platformId={}", platformId);
		return miRepo.findByPlatform(getPPlatform(platformId));
	}

	public String getInstanceUrl(PMatomoInstance pmi) {
		return cfMgr.getInstanceUrl(pmi.getUuid());
	}

	public PMatomoInstance createMatomoInstance(
			String uuid,
			String definitionid,
			String instname,
			String tenantid,
			String subtenantid,
			PlatformKind pfkind,
			String apiinfolocation,
			String planid,
			String pfid,
			Parameters parameters) {
		LOGGER.debug("SERV::createMatomoInstance: matomoInstance={}", uuid);
		PPlatform ppf = getPPlatform(pfid);
		for (PMatomoInstance pmi : miRepo.findByPlatformAndLastOperation(ppf, POperationStatus.OpCode.DELETE_SERVICE_INSTANCE.toString())) {
			if (pmi.getUuid().equals(uuid)) {
				LOGGER.error("Matomo Instance with ID=" + uuid
						+ " already exists in Platform with ID=" + pfid);
				return null;
			}
		}
		PMatomoInstance pmi = new PMatomoInstance(uuid, instanceIdMgr.allocateInstanceId(),
				definitionid, instname, pfkind,
				apiinfolocation, planid, ppf, parameters);
		savePMatomoInstance(pmi);
		if (!cfMgr.isSmtpReady()) {
			LOGGER.warn("Cannot create any kind of instance: service unavailable (retry later on)");
			pmi.setLastOperationState(OperationState.FAILED);
		} else if (planid.equals(ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID) && !cfMgr.isGlobalSharedReady()) {
			LOGGER.warn("Cannot create an instance with plan <" + ServiceCatalogConfiguration.PLANGLOBSHARDB_NAME + ">: unavailable (retry later on)");
			pmi.setLastOperationState(OperationState.FAILED);
		} else if (planid.equals(ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID) && !cfMgr.isMatomoSharedReady()) {
			LOGGER.warn("Cannot create an instance with plan <" + ServiceCatalogConfiguration.PLANMATOMOSHARDB_NAME + ">: unavailable (retry later on)");
			pmi.setLastOperationState(OperationState.FAILED);
		}
		if (pmi.getLastOperationState() == OperationState.FAILED) {
			pmi.setInstalledVersion(NOTINSTALLED);
			return savePMatomoInstance(pmi);
		}
		matomoReleases.createLinkedTree(parameters.getVersion(), pmi.getIdUrlStr());
		Mono<Void> createdb = (properties.getDbCreds(pmi.getPlanId()).isDedicatedDb()) ? cfMgr.createDedicatedDb(pmi.getIdUrlStr()) : Mono.empty();
		createdb
		.timeout(Duration.ofMinutes(CloudFoundryMgr.CREATEDBSERV_TIMEOUT))
		.doOnError(t -> {
			LOGGER.error("Create dedicated DB for instance \"" + pmi.getUuid() + "\" failed.", t);
			pmi.setLastOperationState(OperationState.FAILED);
			savePMatomoInstance(pmi);
		})
		.doOnSuccess(v -> {
			LOGGER.debug("Create dedicated DB for instance \"" + pmi.getUuid() + "\" succeeded.");
			cfMgr.deployMatomoCfApp(pmi.getIdUrlStr(), pmi.getUuid(), pmi.getPlanId(), parameters, 256, 1)
			.doOnError(t -> {
				LOGGER.error("Async create app instance (phase 1) \"" + pmi.getUuid() + "\" failed.", t);
				matomoReleases.deleteLinkedTree(pmi.getIdUrlStr());
				pmi.setLastOperationState(OperationState.FAILED);
				savePMatomoInstance(pmi);
			})
			.doOnSuccess(vv -> {
				LOGGER.debug("Async create app instance (phase 1) \"" + pmi.getUuid() + "\" succeeded");
				deleteAssociatedDbSchema(pmi); // make sure the DB situation is clean
				if (!initializeMatomoInstance(pmi.getIdUrlStr(), pmi.getUuid(), pmi.getPassword())) {
					matomoReleases.deleteLinkedTree(pmi.getIdUrlStr());
					pmi.setLastOperationState(OperationState.FAILED);
					savePMatomoInstance(pmi);
				} else {
					cfMgr.getInstanceConfigFile(pmi.getIdUrlStr(), parameters.getVersion(), pmi.getClusterMode())
					.doOnError(t -> {
						LOGGER.debug("Cannot retrieve config file from Matomo instance.", t);
						matomoReleases.deleteLinkedTree(pmi.getIdUrlStr());
						pmi.setLastOperationState(OperationState.FAILED);
						savePMatomoInstance(pmi);
					})
					.doOnSuccess(ach -> {
						pmi.setConfigFileContent(ach.fileContent);
						savePMatomoInstance(pmi);
						settleMatomoInstance(pmi, parameters, true, properties.getDbCreds(pmi.getPlanId())).subscribe();
					}).subscribe();
				}
			}).subscribe();				
		}).subscribe();
		return pmi;
	}

	public String deleteMatomoInstance(String platformId, String instanceId) {
		LOGGER.debug("SERV::deleteMatomoInstance: platformId={} instanceId={}", platformId, instanceId);
		PPlatform ppf = getPPlatform(platformId);
		Optional<PMatomoInstance> opmi = miRepo.findById(instanceId);
		if (!opmi.isPresent()) {
			LOGGER.warn("SERV::deleteMatomoInstance: KO -> does not exist.");
			return null;
		}
		PMatomoInstance pmi = opmi.get();
		pmi.setLastOperation(POperationStatus.OpCode.DELETE_SERVICE_INSTANCE);
		if (pmi.getPlatform() != ppf) {
			LOGGER.error("SERV::deleteMatomoInstance: KO -> wrong platform.");
			pmi.setLastOperationState(OperationState.FAILED);
			savePMatomoInstance(pmi);
			return "Error: wrong platform with ID=" + platformId + " for Matomo service instance with ID=" + instanceId + ".";
		}
		if (pmi.getLastOperationState() == OperationState.IN_PROGRESS) {
			LOGGER.debug("SERV::deleteMatomoInstance: KO -> operation in progress.");
			return "Error: cannot delete Matomo service instance with ID=" + pmi.getUuid() + ": operation already in progress.";
		}
		pmi.setLastOperationState(OperationState.IN_PROGRESS);
		savePMatomoInstance(pmi);
		if (pmi.getInstalledVersion().equals(NOTINSTALLED)) {
			pmi.setLastOperationState(OperationState.SUCCEEDED);
			savePMatomoInstance(pmi);
			return "Nothing to delete for instance with ID=" + pmi.getUuid();
		}
		// delete data associated with the instance under deletion
		deleteAssociatedDbSchema(pmi);
		cfMgr.deleteMatomoCfApp(pmi.getIdUrlStr(), pmi.getPlanId())
		.doOnError(t -> {
			LOGGER.debug("Async delete app instance \"" + pmi.getUuid() + "\" failed -> " + t.getMessage());
			pmi.setLastOperationState(OperationState.FAILED);
			savePMatomoInstance(pmi);
		})
		.doOnSuccess(v -> {
			LOGGER.debug("Async delete app instance \"" + pmi.getUuid() + "\" succeeded");
			if (properties.getDbCreds(pmi.getPlanId()).isDedicatedDb()) {
				cfMgr.deleteDedicatedDb(pmi.getIdUrlStr())
				.doOnError(tt -> {
					LOGGER.error("Delete dedicated DB for instance \"{}\" failed: please delete manually service instance {}.", 
							pmi.getUuid(),
							properties.getDbCreds(pmi.getPlanId()));
					tt.printStackTrace();
					pmi.setLastOperationState(OperationState.FAILED);
					savePMatomoInstance(pmi);
				})
				.doOnSuccess(vv -> {
					LOGGER.debug("Delete dedicated DB for instance \"" + pmi.getUuid() + "\" succeeded.");
					instanceIdMgr.freeInstanceId(pmi.getIdUrl());
					pmi.setLastOperationState(OperationState.SUCCEEDED);
					pmi.setConfigFileContent(null);
					savePMatomoInstance(pmi);					
				})
				.subscribe();
			} else {
				instanceIdMgr.freeInstanceId(pmi.getIdUrl());
				pmi.setLastOperationState(OperationState.SUCCEEDED);
				pmi.setConfigFileContent(null);
				savePMatomoInstance(pmi);
			}
		})
		.subscribe();
		return "Delete launched for instance with ID=" + pmi.getUuid();
	}

	public PMatomoInstance updateMatomoInstance(String uuid, String pfid, Parameters parameters) {
		LOGGER.debug("SERV::updateMatomoInstance: matomoInstance={}", uuid);
		PPlatform ppf = getPPlatform(pfid);
		Optional<PMatomoInstance> opmi = miRepo.findById(uuid);
		if (!opmi.isPresent()) {
			LOGGER.error("SERV::updateMatomoInstance: KO -> does not exist.");
			return null;
		}
		PMatomoInstance pmi = opmi.get();
		if (pmi.getPlatform() != ppf) {
			LOGGER.error("SERV::updateMatomoInstance: KO -> wrong platform.");
			return null;
		}
		if (pmi.getLastOperationState() == OperationState.IN_PROGRESS) {
			LOGGER.debug("SERV::updateMatomoInstance: KO -> operation in progress.");
			return pmi;
		}
		pmi.setLastOperation(POperationStatus.OpCode.UPDATE_SERVICE_INSTANCE);
		pmi.setLastOperationState(OperationState.IN_PROGRESS);
		savePMatomoInstance(pmi);
		if (pmi.getInstalledVersion().equals(NOTINSTALLED)) {
			pmi.setLastOperationState(OperationState.SUCCEEDED);
			LOGGER.warn("Nothing to update (not installed) for instance with ID=" + pmi.getUuid());
			return savePMatomoInstance(pmi);
		}
		if (pmi.getAutomaticVersionUpgrade() != parameters.isAutoVersionUpgrade()) {
			pmi.setAutomaticVersionUpgrade(parameters.isAutoVersionUpgrade());
			savePMatomoInstance(pmi);
			if (parameters.isAutoVersionUpgrade()) {
				parameters.setVersion(matomoReleases.getLatestReleaseName());
			}
		}
		if (pmi.getTimeZone().equals(parameters.getTimeZone())) {
			parameters.setTimeZone(null);
		} else {
			LOGGER.debug("Change Matomo instance timezone from {} to {}.", pmi.getTimeZone(), parameters.getTimeZone());
			pmi.setTimeZone(parameters.getTimeZone());
			savePMatomoInstance(pmi);
		}
		if (pmi.getInstances() == parameters.getCfInstances()) {
			parameters.setCfInstances(-1);
		} else {
			LOGGER.debug("Upgrade Matomo instance nodes from {} to {}.", pmi.getInstances(), parameters.getCfInstances());
			pmi.setIntances(parameters.getCfInstances());
			savePMatomoInstance(pmi);			
		}
		if (pmi.getMemorySize() == parameters.getMemorySize()) {
			parameters.setMemorySize(-1);
		} else {
			LOGGER.debug("Upgrade Matomo instance nodes memory from {}MB to {}MB.", pmi.getMemorySize(), parameters.getMemorySize());
			pmi.setMemorySize(parameters.getMemorySize());
			savePMatomoInstance(pmi);			
		}
		if (matomoReleases.isHigherVersion(parameters.getVersion(), pmi.getInstalledVersion())) {
			LOGGER.debug("Upgrade Matomo instance from version {} to {}.", pmi.getInstalledVersion(), parameters.getVersion());
			updateMatomoInstanceActual(pmi, parameters);
		} else if (parameters.getTimeZone() != null) {
			LOGGER.debug("Change Matomo instance timezone from {} to {}.", pmi.getTimeZone(), parameters.getTimeZone());
			updateMatomoInstanceActual(pmi, parameters);
		} else if ((parameters.getCfInstances() != -1) || (parameters.getMemorySize() != -1)) {
			// only scale app nodes and/or memory size
			cfMgr.scaleMatomoCfApp(pmi.getIdUrlStr(), pmi.getInstances(), pmi.getMemorySize())
			.doOnError(t -> {
				pmi.setLastOperationState(OperationState.FAILED);
				savePMatomoInstance(pmi);
			})
			.doOnSuccess(v -> {
				pmi.setLastOperationState(OperationState.SUCCEEDED);
				savePMatomoInstance(pmi);
			}).subscribe();
		} else {
			// finally nothing to do
			pmi.setLastOperationState(OperationState.SUCCEEDED);
			savePMatomoInstance(pmi);
		}
		return pmi;
	}

	// PRIVATE METHODS --------------------------------------------------------------------------------
	
	private void updateMatomoInstanceActual(PMatomoInstance pmi, Parameters mip) {
		LOGGER.debug("SERV::updateMatomoInstanceActual: matomoInstance={}, newVersion={}", pmi.getUuid(), mip.getVersion());
		Mono.create(sink -> {
			matomoReleases.createLinkedTree(mip.getVersion(), pmi.getIdUrlStr());
			matomoReleases.setConfigIni(mip.getVersion(), pmi.getIdUrlStr(), pmi.getConfigFileContent());
			cfMgr.deployMatomoCfApp(pmi.getIdUrlStr(), pmi.getUuid(), pmi.getPlanId(), mip, 256, 1)
			.doOnError(t -> {sink.error(t);})
			.doOnSuccess(vvv -> {sink.success();})
			.subscribe();
		})
		.doOnError(t -> {
			LOGGER.debug("Async upgrade app instance \"" + pmi.getUuid() + "\" failed.", t);
			matomoReleases.deleteLinkedTree(pmi.getIdUrlStr());
			pmi.setLastOperationState(OperationState.FAILED);
			savePMatomoInstance(pmi);						
		})
		.doOnSuccess(v -> {
			LOGGER.debug("Async upgrade app instance (phase 1) \"" + pmi.getUuid() + "\" succeeded");
			if (!upgradeMatomoInstance(pmi.getIdUrlStr(), pmi.getUuid())) {
				matomoReleases.deleteLinkedTree(pmi.getIdUrlStr());
				pmi.setLastOperationState(OperationState.FAILED);
				savePMatomoInstance(pmi);
			} else {
				pmi.setInstalledVersion(mip.getVersion());
				savePMatomoInstance(pmi);
				settleMatomoInstance(pmi, mip, false, properties.getDbCreds(pmi.getPlanId())).subscribe();
			}

		})
		.subscribe();
	}

	@SuppressWarnings("unchecked")
	private Mono<Void> settleMatomoInstance(PMatomoInstance pmi, Parameters mip, boolean retrievetoken, CloudFoundryMgrProperties.DbCreds dbCreds) {
		return cfMgr.deployMatomoCfApp(pmi.getIdUrlStr(), pmi.getUuid(), pmi.getPlanId(), mip, pmi.getMemorySize(), pmi.getInstances())
				.doOnError(t -> {
					LOGGER.debug("Async settle app instance (phase 2.1) \"" + pmi.getUuid() + "\" failed.", t);
					matomoReleases.deleteLinkedTree(pmi.getIdUrlStr());
					pmi.setLastOperationState(OperationState.FAILED);
					savePMatomoInstance(pmi);
				})
				.doOnSuccess(vvv -> {
					matomoReleases.deleteLinkedTree(pmi.getIdUrlStr());
					if (retrievetoken) {
						LOGGER.debug("Get Matomo Instance API Credentials");
						cfMgr.getApplicationEnv(pmi.getIdUrlStr())
						.doOnError(t -> {
							LOGGER.debug("Async settle app instance (phase 2.2) \"" + pmi.getUuid() + "\" failed.", t);
							t.printStackTrace();
							pmi.setLastOperationState(OperationState.FAILED);
							savePMatomoInstance(pmi);
						})
						.doOnSuccess(env -> {
							// dirty: fetch token_auth from the database as I can't succeed to do it with
							// the API :-(
							pmi.setDbCred(dbCreds.getJdbcUrl((Map<String, Object>)env.get("VCAP_SERVICES")));
							String token = getApiAccessToken(pmi.getDbCred(), pmi.getIdUrlStr());
							if (token == null) {
								pmi.setLastOperationState(OperationState.FAILED);
							} else {
								pmi.setTokenAuth(token);
								pmi.setLastOperationState(OperationState.SUCCEEDED);
							}
							savePMatomoInstance(pmi);
							LOGGER.debug("Async settle app instance (phase 2) \"" + pmi.getUuid() + "\" succeeded");
						}).subscribe();
					} else {
						pmi.setLastOperationState(OperationState.SUCCEEDED);
						savePMatomoInstance(pmi);
						LOGGER.debug("Async settle app instance (phase 2) \"" + pmi.getUuid() + "\" succeeded");
					}
				});
	}

	private PMatomoInstance savePMatomoInstance(PMatomoInstance pmi) {
		miRepo.save(pmi);
		return pmi;
	}

	/**
	 * 
	 * @param appcode
	 * @param nuri
	 * @param pwd
	 * @return		true if the instance has been intialized correctly
	 */
	private boolean initializeMatomoInstance(String appcode, String nuri, String pwd) {
		LOGGER.debug("SERV::initializeMatomoInstance: appCode={}", appcode);
		RestTemplate restTemplate = new RestTemplate();
		try {
			URI uri = new URI("https://" + nuri + "." + properties.getDomain()), calluri;
			LOGGER.debug("Base URI: {}", uri.toString());
			String res = restTemplate.getForObject(calluri = uri, String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			res = restTemplate.getForObject(calluri = URI.create(uri.toString() + "/index.php?action=systemCheck"),
					String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			res = restTemplate.getForObject(calluri = URI.create(uri.toString() + "/index.php?action=databaseSetup"),
					String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			Document d = Jsoup.parse(res);
			if (d == null) {
				LOGGER.error("SERV::initializeMatomoInstance: error while decoding JSON from GET databaseSetup.");
				return false;
			}
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
					mbb.build(), String.class);
			LOGGER.debug("After POST on <{}>", calluri.toString());
			res = restTemplate.getForObject(
					calluri = URI.create(uri.toString() + "/index.php?action=tablesCreation&module=Installation"),
					String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			res = restTemplate.getForObject(
					calluri = URI.create(uri.toString() + "/index.php?action=setupSuperUser&module=Installation"),
					String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			mbb = new MultipartBodyBuilder();
			mbb.part("login", MATOMOINSTANCE_ROOTUSER);
			mbb.part("password", pwd);
			mbb.part("password_bis", pwd);
			mbb.part("email", "piwik@orange.com");
			mbb.part("subscribe_newsletter_piwikorg", "0");
			mbb.part("subscribe_newsletter_professionalservices", "0");
			mbb.part("submit", "Suivant+%C2%BB");
			res = restTemplate.postForObject(
					calluri = URI.create(uri.toString() + "/index.php?action=setupSuperUser&module=Installation"),
					mbb.build(), String.class);
			LOGGER.debug("After POST on <{}>", calluri.toString());
			mbb = new MultipartBodyBuilder();
			mbb.part("siteName", appcode);
			mbb.part("url", uri.toASCIIString());
			mbb.part("timezone", "Europe/Paris");
			mbb.part("ecommerce", "0");
			mbb.part("submit", "Suivant+%C2%BB");
			res = restTemplate.postForObject(
					calluri = URI.create(uri.toString() + "/index.php?action=firstWebsiteSetup&module=Installation"),
					mbb.build(), String.class);
			LOGGER.debug("After POST on <{}>", calluri.toString());
			res = restTemplate.getForObject(calluri = URI.create(
					uri.toString() + "/index.php?action=finished" + "&clientProtocol=https" + "&module=Installation"
							+ "&site_idSite=4" + "&site_name=" + cfMgr.getAppUrlPrefix(appcode)),
					String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			mbb = new MultipartBodyBuilder();
			mbb.part("do_not_track", "1");
			mbb.part("anonymise_ip", "1");
			mbb.part("submit", "Continuer+vers+Matomo+%C2%BB");
			res = restTemplate.postForObject(calluri = URI.create(uri.toString()
					+ "/index.php?action=finished&clientProtocol=https&module=Installation&site_idSite=4&site_name="
					+ cfMgr.getAppUrlPrefix(appcode)), mbb.build(), String.class);
			LOGGER.debug("After POST on <{}>", calluri.toString());
			res = restTemplate.getForObject(calluri = uri, String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
		} catch (RestClientException e) {
			e.printStackTrace();
			LOGGER.error("SERV::initializeMatomoInstance: error while calling service instance through HTTP.", e);
			return false;
		} catch (URISyntaxException e) {
			LOGGER.error("SERV::initializeMatomoInstance: wrong URI for calling service instance through HTTP.", e);
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param appcode
	 * @param nuri
	 * @return		true if the instance has been upgraded correctly
	 */
	private boolean upgradeMatomoInstance(String appcode, String nuri) {
		LOGGER.debug("SERV::upgradeMatomoInstance: appCode={}", appcode);
		RestTemplate restTemplate = new RestTemplate();
		try {
			URI uri = new URI("https://" + nuri + "." + properties.getDomain()), calluri;
			LOGGER.debug("Base URI: {}", uri.toString());
			restTemplate.getForObject(calluri = uri, String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			restTemplate.getForObject(calluri = URI.create(uri.toString() + "/index.php?updateCorePlugins=1"),
					String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
		} catch (RestClientException e) {
			e.printStackTrace();
			LOGGER.error("SERV::upgradeMatomoInstance: error while calling service instance through HTTP.", e);
			return false;
		} catch (URISyntaxException e) {
			LOGGER.error("SERV::upgradeMatomoInstance: wrong URI for calling service instance through HTTP.", e);
			return false;
		}
		return true;
	}

	private String getApiAccessToken(String dbcred, String instid) {
		String token = null;
		Connection conn = null;
		Statement stmt = null;
		try {
			Class.forName("org.mariadb.jdbc.Driver");
			conn = DriverManager.getConnection(dbcred);
			stmt = conn.createStatement();
			stmt.execute("SELECT token_auth FROM " + cfMgr.getAppUrlPrefix(instid) + "_user WHERE login='" + MATOMOINSTANCE_ROOTUSER + "'");
			if (!stmt.getResultSet().first()) {
				LOGGER.error("Cannot retrieve the credentials of the admin user (resultset issue).");
			}
			token = stmt.getResultSet().getString(1);
		} catch (ClassNotFoundException e) {
			LOGGER.error("Cannot retrieve the credentials of the admin user (driver not found).", e);
		} catch (SQLException e) {
			LOGGER.error("Cannot retrieve the credentials of the admin user (sql problem).", e);
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException e) {
				LOGGER.error("Problem while retrieving the credentials of the admin user (at connexion close): " + e.getMessage());
			}
		}
		return token;
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
//			e.printStackTrace();
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
//		return ta;
//	}

	private void deleteAssociatedDbSchema(PMatomoInstance pmi) {
		LOGGER.debug("SERV::deleteAssociatedDbSchema: appCode={}", pmi.getIdUrlStr());
		if (pmi.getDbCred() == null) {
			return;
		}
        Connection conn = null;
        Statement stmt = null;
		try {
			Class.forName("org.mariadb.jdbc.Driver");
			//LOGGER.debug("SERV::deleteAssociatedDbSchema: jdbcUrl={}", pmi.getDbCred());
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
			LOGGER.error("SERV::deleteAssociatedDbSchema: cannot find JDBC driver.");
			e.printStackTrace();
			throw new RuntimeException("Cannot remove tables of deleted Matomo service instance (DRIVER).", e);
		} catch (SQLException e) {
			LOGGER.error("SERV::deleteAssociatedDbSchema: SQL problem -> {}", e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Cannot remove tables of deleted Matomo service instance (SQL EXEC).", e);
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException e) {
				LOGGER.error("SERV::deleteAssociatedDbSchema: SQL problem while closing connexion -> {}", e.getMessage());
				e.printStackTrace();
				throw new RuntimeException("Cannot remove tables of deleted Matomo service instance (CNX CLOSE).", e);
			}
		}
	}
}
