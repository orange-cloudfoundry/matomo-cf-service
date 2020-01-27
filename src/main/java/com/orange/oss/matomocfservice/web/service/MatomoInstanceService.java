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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.stereotype.Service;

import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgr;
import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgrProperties;
import com.orange.oss.matomocfservice.servicebroker.ServiceCatalogConfiguration;
import com.orange.oss.matomocfservice.web.domain.PMatomoInstance;
import com.orange.oss.matomocfservice.web.domain.PMatomoInstance.PlatformKind;
import com.orange.oss.matomocfservice.web.domain.POperationStatus;
import com.orange.oss.matomocfservice.web.domain.PPlatform;
import com.orange.oss.matomocfservice.web.domain.Parameters;
import com.orange.oss.matomocfservice.web.repository.PMatomoInstanceRepository;

import io.jsonwebtoken.lang.Assert;
import reactor.core.publisher.Mono;

/**
 * @author P. Déchamboux
 *
 */
@Service
public class MatomoInstanceService extends OperationStatusService {
	private final static Logger LOGGER = LoggerFactory.getLogger(MatomoInstanceService.class);
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

	public PMatomoInstance getMatomoInstance(String instanceId, String platformId) {
		Assert.notNull(platformId, "platform id mustn't be null");
		Assert.notNull(instanceId, "instance id mustn't be null");
		LOGGER.debug("SERV::getMatomoInstance: instanceId={}, platformId={}", instanceId, platformId);
		Optional<PMatomoInstance> opmi = miRepo.findById(instanceId);
		if (! opmi.isPresent()) {
			LOGGER.debug("Matomo Instance with ID=" + instanceId + " not known in Platform with ID=" + platformId);
			return null;
		}
		PMatomoInstance pmi = opmi.get();
		if (!pmi.getPlatform().getId().equals(platformId)) {
			LOGGER.error("Wrong platform with ID=" + platformId + " for Service Instance with ID=" + instanceId);
			return null;
		}
		return pmi;
	}

	public List<PMatomoInstance> findMatomoInstance(String platformId) {
		Assert.notNull(platformId, "platform id mustn't be null");
		LOGGER.debug("SERV::findMatomoInstance: platformId={}", platformId);
		getPPlatform(platformId);
		return miRepo.findByPlatform(getPPlatform(platformId));
	}

	public String getInstanceUrl(String mi_uuid) {
		Assert.notNull(mi_uuid, "p matomo instance mustn't be null");
		return cfMgr.getInstanceUrl(mi_uuid);
	}

	public String createMatomoInstance(String uuid, String instname, PlatformKind pfkind, String apiinfolocation,
			String planid, String pfid, Parameters parameters) {
		Assert.notNull(uuid, "instance uuid mustn't be null");
		Assert.notNull(instname, "instance name mustn't be null");
		Assert.notNull(pfkind, "platform kind mustn't be null");
		Assert.notNull(apiinfolocation, "api location mustn't be null");
		Assert.notNull(planid, "planid mustn't be null");
		Assert.notNull(parameters, "parameters mustn't be null");
		LOGGER.debug("SERV::createMatomoInstance: instanceId={}, platformId={}, instName={}", uuid, pfid, instname);
		if (getMatomoInstance(uuid, pfid) != null) {
			LOGGER.error("Matomo Instance with ID=" + uuid + " already exists in Platform with ID=" + pfid);
			return null;
		}
		EntityManager em = beginTx();
		PMatomoInstance pmi;
		try {
			PPlatform ppf = getPPlatform(pfid);
			pmi = new PMatomoInstance(uuid, instanceIdMgr.allocateInstanceId(), instname, pfkind,
					apiinfolocation, planid, ppf, parameters);
		} catch (Exception e) {
			commitTx(em);
			throw e;
		}
		savePMatomoInstance(pmi, OperationState.IN_PROGRESS);
		if (!cfMgr.isSmtpReady()
				|| (planid.equals(ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID) && !cfMgr.isGlobalSharedReady())) {
			LOGGER.warn("Cannot create any kind of instance: service unavailable (retry later on)");
			pmi.setInstalledVersion(NOTINSTALLED);
			savePMatomoInstance(pmi, OperationState.FAILED);
			commitTx(em);
			return uuid;
		}
		matomoReleases.createLinkedTree(parameters.getVersion(), pmi.getIdUrlStr());
		commitTx(em);
		Mono<Void> createdb = (properties.getDbCreds(planid).isDedicatedDb())
				? cfMgr.createDedicatedDb(pmi.getIdUrlStr(), planid)
				: Mono.empty();
		createdb.timeout(Duration.ofMinutes(CloudFoundryMgr.CREATEDBSERV_TIMEOUT))
		.doOnError(t -> {
			EntityManager nem = beginTx();
			PMatomoInstance npmi = miRepo.getOne(uuid);
			nem.unwrap(Session.class).update(npmi);
			LOGGER.error("Create dedicated DB for instance \"" + npmi.getUuid() + "\" failed.", t);
			savePMatomoInstance(npmi, OperationState.FAILED);
			commitTx(nem);
		}).doOnSuccess(v -> {
			LOGGER.debug("Create dedicated DB phase for \"" + uuid + "\" succeeded.");
			cfMgr.deployMatomoCfApp(pmi.getIdUrlStr(), uuid, planid, parameters, 256, 1)
			.doOnError(tt -> {
				EntityManager nem = beginTx();
				PMatomoInstance npmi = miRepo.getOne(uuid);
				nem.unwrap(Session.class).update(npmi);
				LOGGER.error("Async create app instance (phase 1) \"" + uuid + "\" failed.", tt);
				matomoReleases.deleteLinkedTree(pmi.getIdUrlStr());
				savePMatomoInstance(npmi, OperationState.FAILED);
				commitTx(nem);
			}).doOnSuccess(vv -> {
				EntityManager nem = beginTx();
				PMatomoInstance npmi = miRepo.getOne(uuid);
				nem.unwrap(Session.class).update(npmi);
				if (npmi.getLastOperationState() == OperationState.FAILED) {
					LOGGER.debug("Async create app too late (phase 1) \"" + uuid + "\": already failed");
				} else {
					LOGGER.debug("Async create app instance (phase 1) \"" + uuid + "\" succeeded");
					cfMgr.deleteAssociatedDbSchema(npmi); // make sure the DB situation is clean
					if (!cfMgr.initializeMatomoInstance(npmi.getIdUrlStr(), uuid, npmi.getPassword(),
							npmi.getPlanId())) {
						matomoReleases.deleteLinkedTree(npmi.getIdUrlStr());
						savePMatomoInstance(npmi, OperationState.FAILED);
						commitTx(nem);
					} else {
						commitTx(nem);
						cfMgr.getInstanceConfigFile(pmi.getIdUrlStr(), parameters.getVersion(), pmi.getClusterMode())
								.doOnError(ttt -> {
									EntityManager nnem = beginTx();
									PMatomoInstance nnpmi = miRepo.getOne(uuid);
									nnem.unwrap(Session.class).update(nnpmi);
									LOGGER.debug("Cannot retrieve config file from Matomo instance.", ttt);
									matomoReleases.deleteLinkedTree(nnpmi.getIdUrlStr());
									savePMatomoInstance(nnpmi, OperationState.FAILED);
									commitTx(nnem);
								}).doOnSuccess(ach -> {
									EntityManager nnem = beginTx();
									PMatomoInstance nnpmi = miRepo.getOne(uuid);
									nnem.unwrap(Session.class).update(nnpmi);
									nnpmi.setConfigFileContent(ach.fileContent);
									savePMatomoInstance(nnpmi, null);
									commitTx(nnem);
									settleMatomoInstance(nnpmi, parameters, true,
											properties.getDbCreds(nnpmi.getPlanId())).subscribe();
								}).subscribe();

					}
				}
			}).subscribe();
		}).subscribe();
		return uuid;
	}

	public String deleteMatomoInstance(String uuid, String platformId) {
		LOGGER.debug("SERV::deleteMatomoInstance: instanceId={}, platformId={}", uuid, platformId);
		EntityManager em = beginTx();
		PMatomoInstance pmi = null;
		try {
			Optional<PMatomoInstance> opmi = miRepo.findById(uuid);
			if (!opmi.isPresent()) {
				LOGGER.warn("SERV::deleteMatomoInstance: KO -> does not exist.");
				return null;
			}
			pmi = opmi.get();
			if (!pmi.getPlatform().getId().equals(platformId)) {
				LOGGER.warn("SERV::deleteMatomoInstance: KO -> wrong platform.");
				return null;
			}
			if ((pmi.getLastOperation() == POperationStatus.OpCode.DELETE_SERVICE_INSTANCE) &&
					(pmi.getLastOperationState() == OperationState.FAILED)) {
				LOGGER.error("SERV::deleteMatomoInstance: deletion already failed -> force delete.");
				instanceIdMgr.freeInstanceId(pmi.getIdUrl());
				pmi.setConfigFileContent(null);
				savePMatomoInstance(pmi, OperationState.SUCCEEDED);
				return "Error: deletion already failed on platform with ID=" + platformId + " for Matomo service instance with ID=" + uuid + ": force deletion!";
			}
			if (pmi.getLastOperationState() == OperationState.IN_PROGRESS) {
				LOGGER.debug("SERV::deleteMatomoInstance: KO -> operation in progress.");
				return "Error: cannot delete Matomo service instance with ID=" + uuid + ": operation already in progress.";
			}
			pmi.setLastOperation(POperationStatus.OpCode.DELETE_SERVICE_INSTANCE);
			savePMatomoInstance(pmi, OperationState.IN_PROGRESS);
			if (pmi.getInstalledVersion().equals(NOTINSTALLED)) {
				savePMatomoInstance(pmi, OperationState.SUCCEEDED);
				instanceIdMgr.freeInstanceId(pmi.getIdUrl());
				pmi.setConfigFileContent(null);
				return "Nothing to delete for instance with ID=" + pmi.getUuid();
			}
			// delete data associated with the instance under deletion
			cfMgr.deleteAssociatedDbSchema(pmi);
		} catch (Exception e) {
			LOGGER.error("Problem in first step of deletion", e);
			throw e;
		} finally {
			commitTx(em);
		}
		cfMgr.deleteMatomoCfApp(pmi.getIdUrlStr(), pmi.getPlanId())
		.doOnError(t -> {
			EntityManager nem = beginTx();
			PMatomoInstance npmi = miRepo.getOne(uuid);
			nem.unwrap(Session.class).update(npmi);
			LOGGER.debug("Async delete app instance \"" + npmi.getUuid() + "\" failed -> " + t.getMessage());
			savePMatomoInstance(npmi, OperationState.FAILED);
			commitTx(nem);
		})
		.doOnSuccess(v -> {
			EntityManager nem = beginTx();
			PMatomoInstance npmi = miRepo.getOne(uuid);
			nem.unwrap(Session.class).update(npmi);
			LOGGER.debug("Async delete app instance \"" + npmi.getUuid() + "\" succeeded");
			if (properties.getDbCreds(npmi.getPlanId()).isDedicatedDb()) {
				cfMgr.deleteDedicatedDb(npmi.getIdUrlStr(), npmi.getPlanId())
				.doOnError(tt -> {
					EntityManager nnem = beginTx();
					PMatomoInstance nnpmi = miRepo.getOne(uuid);
					nnem.unwrap(Session.class).update(nnpmi);
					LOGGER.error("Delete dedicated DB for instance \"{}\" failed: please delete manually service instance {}.", 
							nnpmi.getUuid(),
							properties.getDbCreds(nnpmi.getPlanId()));
					tt.printStackTrace();
					savePMatomoInstance(nnpmi, OperationState.FAILED);
					commitTx(nnem);
				})
				.doOnSuccess(vv -> {
					EntityManager nnem = beginTx();
					PMatomoInstance nnpmi = miRepo.getOne(uuid);
					nnem.unwrap(Session.class).update(nnpmi);
					LOGGER.debug("Delete dedicated DB for instance \"" + nnpmi.getUuid() + "\" succeeded.");
					instanceIdMgr.freeInstanceId(nnpmi.getIdUrl());
					nnpmi.setConfigFileContent(null);
					savePMatomoInstance(nnpmi, OperationState.SUCCEEDED);					
					commitTx(nnem);
				})
				.subscribe();
			} else {
				instanceIdMgr.freeInstanceId(npmi.getIdUrl());
				npmi.setConfigFileContent(null);
				savePMatomoInstance(npmi, OperationState.SUCCEEDED);
			}
			commitTx(nem);
		})
		.subscribe();
		return "Delete launched for instance with ID=" + uuid;
	}

	public String updateMatomoInstance(String uuid, String pfid, Parameters parameters) {
		LOGGER.debug("SERV::updateMatomoInstance: matomoInstance={}", uuid);
		EntityManager em = beginTx();
		PPlatform ppf = getPPlatform(pfid);
		Optional<PMatomoInstance> opmi = miRepo.findById(uuid);
		if (!opmi.isPresent()) {
			LOGGER.error("SERV::updateMatomoInstance: KO -> does not exist.");
			commitTx(em);
			return null;
		}
		PMatomoInstance pmi = opmi.get();
		if (pmi.getPlatform() != ppf) {
			LOGGER.error("SERV::updateMatomoInstance: KO -> wrong platform.");
			commitTx(em);
			return null;
		}
		if (pmi.getLastOperationState() == OperationState.IN_PROGRESS) {
			LOGGER.debug("SERV::updateMatomoInstance: KO -> operation in progress.");
			commitTx(em);
			return uuid;
		}
		pmi.setLastOperation(POperationStatus.OpCode.UPDATE_SERVICE_INSTANCE);
		savePMatomoInstance(pmi, OperationState.IN_PROGRESS);
		if (pmi.getInstalledVersion().equals(NOTINSTALLED)) {
			LOGGER.warn("Nothing to update (not installed) for instance with ID=" + pmi.getUuid());
			savePMatomoInstance(pmi, OperationState.SUCCEEDED).getUuid();
			commitTx(em);
			return uuid;
		}
		if (pmi.getAutomaticVersionUpgrade() != parameters.isAutoVersionUpgrade()) {
			pmi.setAutomaticVersionUpgrade(parameters.isAutoVersionUpgrade());
			savePMatomoInstance(pmi, null);
			if (parameters.isAutoVersionUpgrade()) {
				parameters.setVersion(matomoReleases.getLatestReleaseName());
			}
		}
		if (pmi.getTimeZone().equals(parameters.getTimeZone())) {
			parameters.setTimeZone(null);
		} else {
			LOGGER.debug("Change Matomo instance timezone from {} to {}.", pmi.getTimeZone(), parameters.getTimeZone());
			pmi.setTimeZone(parameters.getTimeZone());
			savePMatomoInstance(pmi, null);
		}
		if (pmi.getInstances() == parameters.getCfInstances()) {
			parameters.setCfInstances(-1);
		} else {
			LOGGER.debug("Upgrade Matomo instance nodes from {} to {}.", pmi.getInstances(), parameters.getCfInstances());
			pmi.setIntances(parameters.getCfInstances());
			savePMatomoInstance(pmi, null);			
		}
		if (pmi.getMemorySize() == parameters.getMemorySize()) {
			parameters.setMemorySize(-1);
		} else {
			LOGGER.debug("Upgrade Matomo instance nodes memory from {}MB to {}MB.", pmi.getMemorySize(), parameters.getMemorySize());
			pmi.setMemorySize(parameters.getMemorySize());
			savePMatomoInstance(pmi, null);			
		}
		if (matomoReleases.isHigherVersion(parameters.getVersion(), pmi.getInstalledVersion())) {
			LOGGER.debug("Upgrade Matomo instance from version {} to {}.", pmi.getInstalledVersion(), parameters.getVersion());
			commitTx(em);
			updateMatomoInstanceActual(pmi, parameters);
		} else if (parameters.getTimeZone() != null) {
			LOGGER.debug("Change Matomo instance timezone from {} to {}.", pmi.getTimeZone(), parameters.getTimeZone());
			commitTx(em);
			updateMatomoInstanceActual(pmi, parameters);
		} else if ((parameters.getCfInstances() != -1) || (parameters.getMemorySize() != -1)) {
			// only scale app nodes and/or memory size
			commitTx(em);
			cfMgr.scaleMatomoCfApp(pmi.getIdUrlStr(), pmi.getInstances(), pmi.getMemorySize())
			.doOnError(t -> {
				EntityManager nem = beginTx();
				PMatomoInstance npmi = miRepo.getOne(uuid);
				nem.unwrap(Session.class).update(npmi);
				savePMatomoInstance(npmi, OperationState.FAILED);
				commitTx(nem);
			})
			.doOnSuccess(v -> {
				EntityManager nem = beginTx();
				PMatomoInstance npmi = miRepo.getOne(uuid);
				nem.unwrap(Session.class).update(npmi);
				savePMatomoInstance(npmi, OperationState.SUCCEEDED);
				commitTx(nem);
			}).subscribe();
		} else {
			// finally nothing to do
			savePMatomoInstance(pmi, OperationState.SUCCEEDED);
			commitTx(em);
		}
		return uuid;
	}

	// PRIVATE METHODS --------------------------------------------------------------------------------
	
	private void updateMatomoInstanceActual(PMatomoInstance pmi, Parameters mip) {
		String uuid = pmi.getUuid();
		LOGGER.debug("SERV::updateMatomoInstanceActual: matomoInstance={}, newVersion={}", pmi.getUuid(), mip.getVersion());
		Mono.create(sink -> {
			matomoReleases.createLinkedTree(mip.getVersion(), pmi.getIdUrlStr());
			matomoReleases.setConfigIni(mip.getVersion(), pmi.getIdUrlStr(), pmi.getConfigFileContent());
			cfMgr.deployMatomoCfApp(pmi.getIdUrlStr(), pmi.getUuid(), pmi.getPlanId(), mip, 256, 1)
			.doOnError(t -> {sink.error(t);})
			.doOnSuccess(v -> {sink.success();})
			.subscribe();
		})
		.doOnError(t -> {
			EntityManager nem = beginTx();
			PMatomoInstance npmi = miRepo.getOne(uuid);
			nem.unwrap(Session.class).update(npmi);
			LOGGER.debug("Async upgrade app instance \"" + npmi.getUuid() + "\" failed.", t);
			matomoReleases.deleteLinkedTree(npmi.getIdUrlStr());
			savePMatomoInstance(npmi, OperationState.FAILED);
			commitTx(nem);
		})
		.doOnSuccess(v -> {
			EntityManager nem = beginTx();
			PMatomoInstance npmi = miRepo.getOne(uuid);
			nem.unwrap(Session.class).update(npmi);
			LOGGER.debug("Async upgrade app instance (phase 1) \"" + npmi.getUuid() + "\" succeeded");
			if (!cfMgr.upgradeMatomoInstance(npmi.getIdUrlStr(), npmi.getUuid())) {
				matomoReleases.deleteLinkedTree(npmi.getIdUrlStr());
				savePMatomoInstance(npmi, OperationState.FAILED);
				commitTx(nem);
			} else {
				npmi.setInstalledVersion(mip.getVersion());
				savePMatomoInstance(npmi, null);
				commitTx(nem);
				settleMatomoInstance(npmi, mip, false, properties.getDbCreds(npmi.getPlanId())).subscribe();
			}
		})
		.subscribe();
	}

	@SuppressWarnings("unchecked")
	private Mono<Void> settleMatomoInstance(PMatomoInstance pmi, Parameters mip, boolean retrievetoken, CloudFoundryMgrProperties.DbCreds dbCreds) {
		String uuid = pmi.getUuid();
		return cfMgr.deployMatomoCfApp(pmi.getIdUrlStr(), pmi.getUuid(), pmi.getPlanId(), mip, pmi.getMemorySize(), pmi.getInstances())
				.doOnError(t -> {
					EntityManager nem = beginTx();
					PMatomoInstance npmi = miRepo.getOne(uuid);
					nem.unwrap(Session.class).update(npmi);
					LOGGER.debug("Async settle app instance (phase 2.1) \"" + npmi.getUuid() + "\" failed.", t);
					matomoReleases.deleteLinkedTree(npmi.getIdUrlStr());
					savePMatomoInstance(npmi, OperationState.FAILED);
					commitTx(nem);
				})
				.doOnSuccess(v -> {
					EntityManager nem = beginTx();
					PMatomoInstance npmi = miRepo.getOne(uuid);
					nem.unwrap(Session.class).update(npmi);
					matomoReleases.deleteLinkedTree(npmi.getIdUrlStr());
					if (retrievetoken) {
						LOGGER.debug("Get Matomo Instance API Credentials");
						commitTx(nem);
						cfMgr.getApplicationEnv(npmi.getIdUrlStr())
						.doOnError(t -> {
							EntityManager nnem = beginTx();
							PMatomoInstance nnpmi = miRepo.getOne(uuid);
							nnem.unwrap(Session.class).update(nnpmi);
							LOGGER.debug("Async settle app instance (phase 2.2) \"" + nnpmi.getUuid() + "\" failed.", t);
							t.printStackTrace();
							savePMatomoInstance(nnpmi, OperationState.FAILED);
							commitTx(nnem);
						})
						.doOnSuccess(env -> {
							// dirty: fetch token_auth from the database as I can't succeed to do it with
							// the API :-(
							EntityManager nnem = beginTx();
							PMatomoInstance nnpmi = miRepo.getOne(uuid);
							nnem.unwrap(Session.class).update(nnpmi);
							nnpmi.setDbCred(dbCreds.getJdbcUrl((Map<String, Object>)env.get("VCAP_SERVICES")));
							String token = cfMgr.getApiAccessToken(nnpmi.getDbCred(), nnpmi.getIdUrlStr(), nnpmi.getPlanId());
							if (token == null) {
								savePMatomoInstance(nnpmi, OperationState.FAILED);
								LOGGER.debug("Async settle app instance (phase 2) \"" + nnpmi.getUuid() + "\" failed");
							} else {
								nnpmi.setTokenAuth(token);
								savePMatomoInstance(nnpmi, OperationState.SUCCEEDED);
								LOGGER.debug("Async settle app instance (phase 2) \"" + nnpmi.getUuid() + "\" succeeded");
							}
							commitTx(nnem);
						}).subscribe();
					} else {
						savePMatomoInstance(npmi, OperationState.SUCCEEDED);
						LOGGER.debug("Async settle app instance (phase 2) \"" + npmi.getUuid() + "\" succeeded");
						commitTx(nem);
					}
				});
	}

	private PMatomoInstance savePMatomoInstance(PMatomoInstance pmi, OperationState os) {
		if (os == null) {
			miRepo.save(pmi);
		} else if (os == OperationState.IN_PROGRESS) {
			pmi.setLastOperationState(os);
			miRepo.save(pmi);
		} else if ((pmi.getLastOperationState() == OperationState.IN_PROGRESS)
				|| (pmi.getLastOperationState() == OperationState.FAILED)) {
			pmi.setLastOperationState(os);
			miRepo.save(pmi);
		}
		return pmi;
	}
}
