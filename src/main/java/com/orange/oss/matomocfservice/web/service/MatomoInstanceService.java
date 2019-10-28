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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.orange.oss.matomocfservice.api.model.MatomoInstance;
import com.orange.oss.matomocfservice.api.model.OpCode;
import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgr;
import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgr.AppConfHolder;
import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgrProperties;
import com.orange.oss.matomocfservice.cfmgr.MatomoReleases;
import com.orange.oss.matomocfservice.web.domain.PMatomoInstance;
import com.orange.oss.matomocfservice.web.domain.PPlatform;
import com.orange.oss.matomocfservice.web.repository.PMatomoInstanceRepository;

import reactor.core.publisher.Mono;

/**
 * @author P. Déchamboux
 *
 */
@Service
public class MatomoInstanceService extends OperationStatusService {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	private final String PARAM_VERSION = "matomoVersion";
	private final String PARAM_TZ = "matomoTimeZone";
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
		LOGGER.debug("SERV::MatomoInstanceService:initialize - latestVersion={}", matomoReleases.getLatestReleaseName());
		for (PMatomoInstance pmi : miRepo.findAll()) {
			if (pmi.getConfigFileContent() != null) {
				LOGGER.debug("SERV::initialize: reactivate instance {}, version={}", pmi.getIdUrlStr(), pmi.getInstalledVersion());
				if (matomoReleases.isHigherVersion(matomoReleases.getLatestReleaseName(), pmi.getInstalledVersion())) {
					// need to upgrade to latest version
					LOGGER.debug("Upgrade Matomo instance from {} to {}.", pmi.getInstalledVersion(), matomoReleases.getLatestReleaseName());
					updateMatomoInstanceActual(pmi, matomoReleases.getLatestReleaseName());
				}
			}
		}
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

	public String getDashboardUrl(PMatomoInstance pmi) {
		return cfMgr.getInstanceUrl(pmi.getIdUrlStr(), pmi.getId());
	}

	public List<MatomoInstance> findMatomoInstance(String platformId) {
		LOGGER.debug("SERV::findMatomoInstance: platformId={}", platformId);
		List<MatomoInstance> instances = new ArrayList<MatomoInstance>();
		for (PMatomoInstance pmi : miRepo.findByPlatform(getPPlatform(platformId))) {
			instances.add(toApiModel(pmi));
		}
		return instances;
	}

	@SuppressWarnings("unchecked")
	public MatomoInstance createMatomoInstance(MatomoInstance matomoInstance, Map<String, Object> parameters) {
		LOGGER.debug("SERV::createMatomoInstance: matomoInstance={}", matomoInstance.toString());
		String instversion = getVersion(parameters);
		String tz = getTimeZone(parameters);
		PPlatform ppf = getPPlatform(matomoInstance.getPlatformId());
		for (PMatomoInstance pmi : miRepo.findByPlatformAndLastOperation(ppf, OpCode.DELETE.toString())) {
			if (pmi.getId().equals(matomoInstance.getUuid())) {
				throw new EntityExistsException("Matomo Instance with ID=" + matomoInstance.getUuid()
						+ " already exists in Platform with ID=" + matomoInstance.getPlatformId());
			}
		}
		PMatomoInstance pmi = new PMatomoInstance(matomoInstance.getUuid(), instanceIdMgr.allocateInstanceId(),
				matomoInstance.getServiceDefinitionId(), matomoInstance.getName(), matomoInstance.getPlatformKind(),
				matomoInstance.getPlatformApiLocation(), matomoInstance.getPlanId(), ppf, instversion);
		savePMatomoInstance(pmi);
		matomoReleases.createLinkedTree(pmi.getIdUrlStr(), instversion);
		Mono<Void> createdb = (properties.getDbCreds(pmi.getPlanId()).isDedicatedDb()) ? cfMgr.createDedicatedDb(pmi.getIdUrlStr()) : Mono.empty();
		createdb.doOnError(t -> {
			LOGGER.error("Create dedicated DB for instance \"" + pmi.getId() + "\" failed.", t);
			pmi.setLastOperationState(OperationState.FAILED);
			savePMatomoInstance(pmi);
		})
		.doOnSuccess(v -> {
			LOGGER.debug("Create dedicated DB for instance \"" + pmi.getId() + "\" succeeded.");
			cfMgr.deployMatomoCfApp(pmi.getIdUrlStr(), instversion, pmi.getId(), pmi.getPlanId(), tz)
			.doOnError(t -> {
				LOGGER.error("Async create app instance (phase 1) \"" + pmi.getId() + "\" failed.", t);
				matomoReleases.deleteLinkedTree(pmi.getIdUrlStr());
				pmi.setLastOperationState(OperationState.FAILED);
				savePMatomoInstance(pmi);
			})
			.doOnSuccess(vv -> {
				LOGGER.debug("Async create app instance (phase 1) \"" + pmi.getId() + "\" succeeded");
				deleteAssociatedDbSchema(pmi); // make sure the DB situation is clean
				if (!initializeMatomoInstance(pmi.getIdUrlStr(), pmi.getId(), pmi.getPassword())) {
					matomoReleases.deleteLinkedTree(pmi.getIdUrlStr());
					pmi.setLastOperationState(OperationState.FAILED);
					savePMatomoInstance(pmi);
				} else {
					settleMatomoInstance(pmi, instversion, tz, true, properties.getDbCreds(pmi.getPlanId())).subscribe();
				}
			}).subscribe();				
		}).subscribe();
		return toApiModel(pmi);
	}

	public void deleteMatomoInstance(String platformId, String instanceId) {
		LOGGER.debug("SERV::deleteMatomoInstance: platformId={} instanceId={}", platformId, instanceId);
		PPlatform ppf = getPPlatform(platformId);
		Optional<PMatomoInstance> opmi = miRepo.findById(instanceId);
		if (!opmi.isPresent()) {
			LOGGER.error("SERV::deleteMatomoInstance: KO -> does not exist.");
			throw new RuntimeException("Error: Matomo service instance does not exist.");
		}
		PMatomoInstance pmi = opmi.get();
		if (pmi.getPlatform() != ppf) {
			LOGGER.error("SERV::deleteMatomoInstance: KO -> wrong platform.");
			throw new RuntimeException("Error: wrong platform with ID=" + platformId + " for Matomo service instance with ID=" + instanceId + ".");
		}
		if (pmi.getLastOperationState() == OperationState.IN_PROGRESS) {
			LOGGER.debug("SERV::deleteMatomoInstance: KO -> operation in progress.");
			throw new RuntimeException("Error: cannot delete Matomo service instance with ID=" + pmi.getId() + ": operation already in progress.");
		}
		pmi.setLastOperation(OpCode.DELETE);
		pmi.setLastOperationState(OperationState.IN_PROGRESS);
		savePMatomoInstance(pmi);
		// delete data associated with the instance under deletion
		deleteAssociatedDbSchema(pmi);
		cfMgr.deleteMatomoCfApp(pmi.getIdUrlStr(), pmi.getPlanId())
		.doOnError(t -> {
			LOGGER.debug("Async delete app instance \"" + pmi.getId() + "\" failed -> " + t.getMessage());
			t.printStackTrace();
			pmi.setLastOperationState(OperationState.FAILED);
			savePMatomoInstance(pmi);
		})
		.doOnSuccess(v -> {
			LOGGER.debug("Async delete app instance \"" + pmi.getId() + "\" succeeded");
			if (properties.getDbCreds(pmi.getPlanId()).isDedicatedDb()) {
				cfMgr.deleteDedicatedDb(pmi.getIdUrlStr())
				.doOnError(tt -> {
					LOGGER.error("Delete dedicated DB for instance \"{}\" failed: please delete manually service instance {}.", 
							pmi.getId(),
							properties.getDbCreds(pmi.getPlanId()));
					pmi.setLastOperationState(OperationState.FAILED);
					savePMatomoInstance(pmi);
				})
				.doOnSuccess(vv -> {
					LOGGER.debug("Delete dedicated DB for instance \"" + pmi.getId() + "\" succeeded.");
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
	}

	public MatomoInstance updateMatomoInstance(MatomoInstance mi, Map<String, Object> parameters) {
		LOGGER.debug("SERV::updateMatomoInstance: matomoInstance={}", mi.toString());
		String newversion = getVersion(parameters);
		PPlatform ppf = getPPlatform(mi.getPlatformId());
		Optional<PMatomoInstance> opmi = miRepo.findById(mi.getUuid());
		if (!opmi.isPresent()) {
			LOGGER.error("SERV::updateMatomoInstance: KO -> does not exist.");
			return mi;
		}
		PMatomoInstance pmi = opmi.get();
		if (pmi.getPlatform() != ppf) {
			LOGGER.error("SERV::updateMatomoInstance: KO -> wrong platform.");
			return mi;
		}
		if (pmi.getLastOperationState() == OperationState.IN_PROGRESS) {
			LOGGER.debug("SERV::updateMatomoInstance: KO -> operation in progress.");
			return mi;
		}
		if (matomoReleases.isHigherVersion(newversion, pmi.getInstalledVersion())) {
			LOGGER.debug("Upgrade Matomo instance from {} to {}.", pmi.getInstalledVersion(), newversion);
			updateMatomoInstanceActual(pmi, newversion);
		}
		return toApiModel(pmi);
	}

	// PRIVATE METHODS --------------------------------------------------------------------------------
	
	private void updateMatomoInstanceActual(PMatomoInstance pmi, String newversion) {
		LOGGER.debug("SERV::updateMatomoInstanceActual: matomoInstance={}, newVersion={}", pmi.getId(), newversion);
		pmi.setLastOperation(OpCode.UPDATE);
		pmi.setLastOperationState(OperationState.IN_PROGRESS);
		savePMatomoInstance(pmi);
		Mono.create(sink -> {
			matomoReleases.createLinkedTree(pmi.getIdUrlStr(), newversion);
			matomoReleases.setConfigIni(pmi.getIdUrlStr(), newversion, pmi.getConfigFileContent());
			cfMgr.deployMatomoCfApp(pmi.getIdUrlStr(), newversion, pmi.getId(), pmi.getPlanId(), null)
			.doOnError(t -> {sink.error(t);})
			.doOnSuccess(vvv -> {sink.success();})
			.subscribe();
		})
		.doOnError(t -> {
			LOGGER.debug("Async upgrade app instance \"" + pmi.getId() + "\" failed.", t);
			matomoReleases.deleteLinkedTree(pmi.getIdUrlStr());
			pmi.setLastOperationState(OperationState.FAILED);
			savePMatomoInstance(pmi);						
		})
		.doOnSuccess(v -> {
			LOGGER.debug("Async upgrade app instance (phase 1) \"" + pmi.getId() + "\" succeeded");
			if (!upgradeMatomoInstance(pmi.getIdUrlStr(), pmi.getId())) {
				matomoReleases.deleteLinkedTree(pmi.getIdUrlStr());
				pmi.setLastOperationState(OperationState.FAILED);
				savePMatomoInstance(pmi);
			} else {
				pmi.setInstalledVersion(newversion);
				savePMatomoInstance(pmi);
				settleMatomoInstance(pmi, newversion, null, false, properties.getDbCreds(pmi.getPlanId())).subscribe();
			}

		})
		.subscribe();
	}

	private Mono<AppConfHolder> settleMatomoInstance(PMatomoInstance pmi, String version, String timezone, boolean retrievetoken, CloudFoundryMgrProperties.DbCreds dbCreds) {
		return cfMgr.getInstanceConfigFile(pmi.getIdUrlStr(), version)
				.doOnError(t -> {
					LOGGER.debug("Cannot retrieve config file from Matomo instance.", t);
					matomoReleases.deleteLinkedTree(pmi.getIdUrlStr());
					pmi.setLastOperationState(OperationState.FAILED);
					savePMatomoInstance(pmi);
				})
				.doOnSuccess(ach -> {
					pmi.setConfigFileContent(ach.fileContent);
					cfMgr.deployMatomoCfApp(pmi.getIdUrlStr(), version, pmi.getId(), pmi.getPlanId(), timezone)
					.doOnError(t -> {
						LOGGER.debug("Async settle app instance (phase 2.1) \"" + pmi.getId() + "\" failed.", t);
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
								LOGGER.debug("Async settle app instance (phase 2.2) \"" + pmi.getId() + "\" failed.", t);
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
								LOGGER.debug("Async settle app instance (phase 2) \"" + pmi.getId() + "\" succeeded");
							}).subscribe();
						} else {
							pmi.setLastOperationState(OperationState.SUCCEEDED);
							savePMatomoInstance(pmi);
							LOGGER.debug("Async settle app instance (phase 2) \"" + pmi.getId() + "\" succeeded");
						}
					}).subscribe();
				});
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

	private String getVersion(Map<String, Object> parameters) {
		LOGGER.debug("SERV::getVersion");
		String instversion = (String) parameters.get(PARAM_VERSION);
		if (instversion == null) {
			instversion = matomoReleases.getDefaultReleaseName();
		} else if (instversion.equals("latest")) {
			instversion = matomoReleases.getLatestReleaseName();
		} else if (! matomoReleases.isVersionAvailable(instversion)) {
				LOGGER.warn("SERV::getVersion: version {} is not supported -> switch to default one.", instversion);
				throw new RuntimeException("Version <" + instversion + "> is not supported by this Matomo CF Service!!");
		}
		return instversion;
	}

	private String getTimeZone(Map<String, Object> parameters) {
		LOGGER.debug("SERV::getTimeZone");
		String tz = (String) parameters.get(PARAM_TZ);
		if (tz == null) {
			tz = "Europe/Paris";
		}
		return tz;
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
