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
import java.util.ArrayList;
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
import com.orange.oss.matomocfservice.web.domain.POperationStatus.OpCode;
import com.orange.oss.matomocfservice.web.domain.PPlatform;
import com.orange.oss.matomocfservice.web.domain.Parameters;
import com.orange.oss.matomocfservice.web.repository.PMatomoInstanceRepository;

import io.jsonwebtoken.lang.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author P. Déchamboux
 *
 */
@Service
public class MatomoInstanceService extends OperationStatusService {
	private final static Logger LOGGER = LoggerFactory.getLogger(MatomoInstanceService.class);
	@Autowired
	private PMatomoInstanceRepository miRepo;
	@Autowired
	private CloudFoundryMgr cfMgr;
	@Autowired
	private InstanceIdMgr instanceIdMgr;
	@Autowired
	private CloudFoundryMgrProperties properties;
	@Autowired
	private PlatformService pfs;
	private boolean sharedReady = false;
	private final InstIds NOPEINSTIDS = new InstIds(null, null);
	private final static String SHAREDINSTANCENAME = "MCFS-SharedMatomoInstance";
	private final static String SHAREDINSTANCEINITUUID = SHAREDINSTANCENAME;
	private final static int SHAREDDEFAULTNBAPPINSTANCE = 3;
	private final static int SHAREDDEFAULTMEMSIZE = 512;

	private class InstIds implements Runnable {
		String id;
		String pfid;

		InstIds(String id, String pfid) {
			this.id = id;
			this.pfid = pfid;
		}

		public void run() {
			if (id == null) {
				return;
			}
			unlockForAtomicOperation(this.id, this.pfid);
		}
	}

	private void observeInstanceForUpgrade(InstIds instids) {
		LOGGER.debug("SERV::MatomoInstanceService: observeInstance({}, {})", instids.id, instids.pfid);
		lockForAtomicOperation(instids.id, instids.pfid);
		PMatomoInstance pmi = getMatomoInstance(instids.id, instids.pfid);
		if (!MatomoReleases.isHigherVersion(MatomoReleases.getLatestReleaseName(), pmi.getInstalledVersion())) {
			unlockForAtomicOperation(instids.id, instids.pfid);
			return;
		}
		LOGGER.debug("Upgrade Matomo instance from {} to {}.", pmi.getInstalledVersion(), MatomoReleases.getLatestReleaseName());
		updateMatomoInstanceActual(pmi, instids, new Parameters()
				.autoVersionUpgrade(pmi.getAutomaticVersionUpgrade())
				.cfInstances(pmi.getInstances())
				.memorySize(pmi.getMemorySize())
				.timeZone(null)
				.version(MatomoReleases.getLatestReleaseName()))
		.subscribe();
	}

	/**
	 * Initialize Matomo service instance manager.
	 */
	public void initialize() {
		LOGGER.debug("SERV::MatomoInstanceService:initialize - latestVersion={}", MatomoReleases.getLatestReleaseName());
		List<InstIds> inst2process = new ArrayList<InstIds>();
		EntityManager em = beginTx();
		try {
			for (PMatomoInstance pmi : miRepo.findAll()) {
				if (!pmi.getAutomaticVersionUpgrade()) {
					if (MatomoReleases.isVersionAvailable(pmi.getInstalledVersion())) {
						continue;						
					}
				}
				if ((pmi.getConfigFileContent() != null) && (pmi.getLastOperationState() == OperationState.SUCCEEDED)) {
					LOGGER.debug("SERV::initialize: reactivate instance {}, version={}", pmi.getIdUrlStr(), pmi.getInstalledVersion());
					if (MatomoReleases.isHigherVersion(MatomoReleases.getLatestReleaseName(), pmi.getInstalledVersion())) {
						// need to upgrade to latest version
						inst2process.add(new InstIds(pmi.getUuid(), pmi.getPlatform().getId()));
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Problem while looking at the Matomo instances to be automatically upgraded at service startup", e);
		} finally {
			commitTx(em);
		}
		// start upgrading service instance (automatic upgrade mode) to the most recent
		// version in background, one at a time each five minutes
		Flux.interval(Duration.ZERO, Duration.ofMinutes(5))
		.map(tick ->  inst2process.get(tick.intValue()))
		.doOnNext(this::observeInstanceForUpgrade)
		.take(inst2process.size())
		.subscribe();
		Optional<PMatomoInstance> opmi = miRepo.findByName(SHAREDINSTANCENAME);
		if (opmi.isPresent()) {
			LOGGER.debug("Matomo Instance for shared plan already exist");
			sharedReady = true;
			return;
		}
		// Create the Matomo service instance for the shared plan
		// TODO : formalize constants in following create...
		createMatomoInstance(SHAREDINSTANCEINITUUID, SHAREDINSTANCENAME, PlatformKind.CLOUDFOUNDRY, "",
				ServiceCatalogConfiguration.PLANDEDICATEDDB_UUID, pfs.getUnknownPlatformId(),
				new Parameters().autoVersionUpgrade(true).cfInstances(SHAREDDEFAULTNBAPPINSTANCE).memorySize(SHAREDDEFAULTMEMSIZE));
	}

	public PMatomoInstance getMatomoInstance(String instanceId, String platformId) {
		Assert.notNull(instanceId, "instance id mustn't be null");
		Assert.notNull(platformId, "platform id mustn't be null");
		LOGGER.debug("SERV::getMatomoInstance: instanceId={}, platformId={}", instanceId, platformId);
		Optional<PMatomoInstance> opmi = miRepo.findById(instanceId);
		if (!opmi.isPresent()) {
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
		Assert.notNull(pfkind, "platform kind mustn't be null");
		Assert.notNull(planid, "planid mustn't be null");
		Assert.notNull(parameters, "parameters mustn't be null");
		if (instname == null) {
			instname = "";
		}
		if (apiinfolocation == null) {
			apiinfolocation = "";
		}
		PMatomoInstance pmi;
		// Shared service bootstrap: test if we want to create the shared Matomo instance
		// which already exist and associate it with a relevant UUID
		if (instname.equals(SHAREDINSTANCENAME) && !uuid.equals(SHAREDINSTANCEINITUUID)) {
			EntityManager em = beginTx();
			try {
				pmi = getMatomoInstance(SHAREDINSTANCEINITUUID, pfs.getUnknownPlatformId());
				if (pmi == null) {
					LOGGER.warn("Shared Matomo instance has already been associated with the CF platform that host the service: can just do it once, ignored");
					return "Shared Matomo instance has already been associated with the CF platform that host the service: can just do it once, ignored";
				}
				pmi.setUuid(uuid);
				pmi.setPlatformApiLocation(apiinfolocation);
				savePMatomoInstance(pmi, pmi.getLastOperationState());
				LOGGER.info("Shared Matomo instance has been associated with the CF platform that host the service");
				return "Shared Matomo instance has been associated with the CF platform that host the service";
			} catch (Exception e) {
				LOGGER.error("Error while associating shared Matomo instance: " + e.getMessage());
				return "Error while associating shared Matomo instance: " + e.getMessage();
			} finally {
				commitTx(em);
			}
		}
		
		LOGGER.debug("SERV::createMatomoInstance: instanceId={}, platformId={}, instName={}, apiInfoLoc={}", uuid, pfid, instname, apiinfolocation);
		if (!cfMgr.isSmtpReady()) {
			LOGGER.error("Cannot create any kind of instance: service unavailable / no SMTP (retry later on)");
			return "Cannot create any kind of instance: service unavailable / no SMTP (retry later on)";
		}
		if (planid.equals(ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID) && !cfMgr.isGlobalSharedReady()) {
			LOGGER.error("Cannot create instance (PLANGLOBSHARDB_UUID): service unavailable / no global shared DB (retry later on)");
			return "Cannot create any kind of instance: service unavailable / no global shared DB (retry later on)";
		}
		if (planid.equals(ServiceCatalogConfiguration.PLANSHARED_UUID) && !sharedReady) {
			LOGGER.error("Cannot create instance (PLANSHARED_UUID): service unavailable / no Matomo shared instance (retry later on)");
			return "Cannot create any kind of instance: service unavailable / no Matomo shared instance (retry later on)";
		}
		synchronized (this) {
			EntityManager em = beginTx();
			try {
				if (getMatomoInstance(uuid, pfid) != null) {
					LOGGER.error("Matomo Instance with ID=" + uuid + " already exists in Platform with ID=" + pfid);
					return "Matomo Instance with ID=" + uuid + " already exists in Platform with ID=" + pfid;
				}
				PPlatform ppf = getPPlatform(pfid);
				if (planid.equals(ServiceCatalogConfiguration.PLANSHARED_UUID)) {
					pmi = new PMatomoInstance(uuid, instanceIdMgr.allocateInstanceId(), instname, pfkind, apiinfolocation,
							planid, ppf, parameters, miRepo.findByName(SHAREDINSTANCENAME).get());
					savePMatomoInstance(pmi, OperationState.SUCCEEDED);
					return null;
				} else {
					pmi = new PMatomoInstance(uuid, instanceIdMgr.allocateInstanceId(), instname, pfkind, apiinfolocation,
							planid, ppf, parameters);
					savePMatomoInstance(pmi, OperationState.IN_PROGRESS);
				}
			} catch (Exception e) {
				return "Error while initializing creation: " + e.getMessage();
			} finally {
				commitTx(em);
			}
		}
		final String idurlstr = pmi.getIdUrlStr();
		final boolean clustmode = pmi.getClusterMode();
		MatomoReleases.createLinkedTree(parameters.getVersion(), idurlstr);
		Mono<Void> createdb = uuid.equals(SHAREDINSTANCEINITUUID)
				? cfMgr.createDedicatedDb(idurlstr, ServiceCatalogConfiguration.PLANSHARED_UUID)
						: (properties.getDbCreds(planid).isDedicatedDb())
						? cfMgr.createDedicatedDb(idurlstr, planid)
								: Mono.empty();
		createdb.timeout(Duration.ofMinutes(CloudFoundryMgr.CREATEDBSERV_TIMEOUT))
		.doOnError(t -> {
			EntityManager em = beginTx();
			PMatomoInstance npmi = miRepo.getOne(uuid);
			em.unwrap(Session.class).update(npmi);
			LOGGER.error("Create dedicated DB for instance \"" + npmi.getUuid() + "\" failed.", t);
			savePMatomoInstance(npmi, OperationState.FAILED);
			commitTx(em);
		}).doOnSuccess(v -> {
			LOGGER.debug("Create dedicated DB phase for \"" + uuid + "\" succeeded.");
			cfMgr.deployMatomoCfApp(idurlstr, uuid, planid, parameters, Parameters.MINMEMORYSIZE, 1)
				.doOnError(tt -> {
					EntityManager nem = beginTx();
					PMatomoInstance npmi = miRepo.getOne(uuid);
					nem.unwrap(Session.class).update(npmi);
					LOGGER.error("Async create app instance (phase 1) \"" + uuid + "\" failed.", tt);
					MatomoReleases.deleteLinkedTree(idurlstr);
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
						if (!cfMgr.initializeMatomoInstance(idurlstr, uuid, npmi.getPassword(),
								npmi.getPlanId())) {
							MatomoReleases.deleteLinkedTree(idurlstr);
							savePMatomoInstance(npmi, OperationState.FAILED);
							commitTx(nem);
						} else {
							commitTx(nem);
							cfMgr.getInstanceConfigFile(idurlstr, parameters.getVersion(), clustmode)
							.doOnError(ttt -> {
								EntityManager nnem = beginTx();
								PMatomoInstance nnpmi = miRepo.getOne(uuid);
								nnem.unwrap(Session.class).update(nnpmi);
								LOGGER.debug("Cannot retrieve config file from Matomo instance.", ttt);
								MatomoReleases.deleteLinkedTree(idurlstr);
								savePMatomoInstance(nnpmi, OperationState.FAILED);
								commitTx(nnem);
							}).doOnSuccess(ach -> {
								EntityManager nnem = beginTx();
								PMatomoInstance nnpmi = miRepo.getOne(uuid);
								nnem.unwrap(Session.class).update(nnpmi);
								nnpmi.setConfigFileContent(ach.fileContent);
								savePMatomoInstance(nnpmi, null);
								commitTx(nnem);
								settleMatomoInstance(nnpmi, NOPEINSTIDS, parameters, true,
										properties.getDbCreds(nnpmi.getPlanId())).subscribe();
							}).subscribe();
						}
					}
				}).subscribe();
		}).subscribe();
		return null;
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
			if (pmi.isSharedPlan()) {
				pmi.setConfigFileContent(null);
				savePMatomoInstance(pmi, OperationState.SUCCEEDED);
				return "Delete completed for instance with ID=" + uuid;
			}
			savePMatomoInstance(pmi, OperationState.IN_PROGRESS);
			// delete data associated with the instance under deletion
			cfMgr.deleteAssociatedDbSchema(pmi);
		} catch (Exception e) {
			LOGGER.warn("SERV::deleteMatomoInstance: KO -> Exception: " + e.getMessage());
			return null;
		} finally {
			commitTx(em);
		}
		cfMgr.deleteMatomoCfApp(pmi.getIdUrlStr(), pmi.getPlanId())
		.doOnError(t -> {
			EntityManager nem = beginTx();
			PMatomoInstance npmi = miRepo.getOne(uuid);
			nem.unwrap(Session.class).update(npmi);
			LOGGER.debug("Async delete app instance \"{}\" failed -> {}", npmi.getUuid(), t.getMessage());
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
		PMatomoInstance pmi;
		try {
			Optional<PMatomoInstance> opmi = miRepo.findById(uuid);
			if (!opmi.isPresent()) {
				LOGGER.error("SERV::updateMatomoInstance: KO -> does not exist.");
				return "KO -> instance does not exist.";
			}
			pmi = opmi.get();
			if (!pmi.getPlatform().getId().equals(pfid)) {
				LOGGER.error("SERV::updateMatomoInstance: KO -> wrong platform.");
				return "KO -> wrong platform.";
			}
			if (pmi.getLastOperationState() == OperationState.IN_PROGRESS) {
				LOGGER.debug("SERV::updateMatomoInstance: KO -> operation in progress.");
				return null;
			}
			if (pmi.isSharedPlan()) {
				return null;
			}
			pmi.setLastOperation(POperationStatus.OpCode.UPDATE_SERVICE_INSTANCE);
			savePMatomoInstance(pmi, OperationState.IN_PROGRESS);
			if (pmi.getAutomaticVersionUpgrade() != parameters.isAutoVersionUpgrade()) {
				pmi.setAutomaticVersionUpgrade(parameters.isAutoVersionUpgrade());
				savePMatomoInstance(pmi, null);
				if (parameters.isAutoVersionUpgrade()) {
					parameters.setVersion(MatomoReleases.getLatestReleaseName());
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
			} else if (pmi.getPlanId().equals(ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID)) {
				LOGGER.warn("Cannot scale service instance for that plan.");
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
		} catch (Exception e) {
			return "KO -> Exception: " + e.getMessage();
		} finally {
			commitTx(em);
		}
		if (MatomoReleases.isHigherVersion(parameters.getVersion(), pmi.getInstalledVersion())) {
			LOGGER.debug("Upgrade Matomo instance from version {} to {}.", pmi.getInstalledVersion(), parameters.getVersion());
			updateMatomoInstanceActual(pmi, NOPEINSTIDS, parameters).subscribe();
		} else if (parameters.getTimeZone() != null) {
			LOGGER.debug("Change Matomo instance timezone from {} to {}.", pmi.getTimeZone(), parameters.getTimeZone());
			updateMatomoInstanceActual(pmi, NOPEINSTIDS, parameters).subscribe();
		} else if ((parameters.getCfInstances() != -1) || (parameters.getMemorySize() != -1)) {
			// only scale app nodes and/or memory size
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
			em = beginTx();
			savePMatomoInstance(pmi, OperationState.SUCCEEDED);
			commitTx(em);
		}
		return null;
	}

	// PRIVATE METHODS --------------------------------------------------------------------------------
	
	private Mono<Object> updateMatomoInstanceActual(PMatomoInstance pmi, InstIds instids, Parameters mip) {
		String uuid = pmi.getUuid();
		LOGGER.debug("SERV::updateMatomoInstanceActual: matomoInstance={}, newVersion={}", pmi.getUuid(), mip.getVersion());
		if (pmi.getLastOperation() != OpCode.UPDATE_SERVICE_INSTANCE) {
			pmi.setLastOperation(POperationStatus.OpCode.UPDATE_SERVICE_INSTANCE);
			savePMatomoInstance(pmi, OperationState.IN_PROGRESS);
		}
		return Mono.create(sink -> {
			MatomoReleases.createLinkedTree(mip.getVersion(), pmi.getIdUrlStr());
			MatomoReleases.setConfigIni(mip.getVersion(), pmi.getIdUrlStr(), pmi.getConfigFileContent());
			cfMgr.deployMatomoCfApp(pmi.getIdUrlStr(), pmi.getUuid(), pmi.getPlanId(), mip, Parameters.MINMEMORYSIZE, 1)
			.doOnError(t -> {sink.error(t);})
			.doOnSuccess(v -> {sink.success();})
			.subscribe();
		})
		.doOnError(t -> {
			EntityManager nem = beginTx();
			PMatomoInstance npmi = miRepo.getOne(uuid);
			nem.unwrap(Session.class).update(npmi);
			LOGGER.debug("Async upgrade app instance \"" + npmi.getUuid() + "\" failed.", t);
			MatomoReleases.deleteLinkedTree(npmi.getIdUrlStr());
			savePMatomoInstance(npmi, OperationState.FAILED);
			commitTx(nem);
			instids.run();
		})
		.doOnSuccess(v -> {
			EntityManager nem = beginTx();
			PMatomoInstance npmi = miRepo.getOne(uuid);
			nem.unwrap(Session.class).update(npmi);
			LOGGER.debug("Async upgrade app instance (phase 1) \"" + npmi.getUuid() + "\" succeeded");
			if (!cfMgr.upgradeMatomoInstance(npmi.getIdUrlStr(), npmi.getUuid())) {
				MatomoReleases.deleteLinkedTree(npmi.getIdUrlStr());
				savePMatomoInstance(npmi, OperationState.FAILED);
				commitTx(nem);
				instids.run();
			} else {
				npmi.setInstalledVersion(mip.getVersion());
				savePMatomoInstance(npmi, null);
				commitTx(nem);
				settleMatomoInstance(npmi, instids, mip, false, properties.getDbCreds(npmi.getPlanId())).subscribe();
			}
		});
	}

	@SuppressWarnings("unchecked")
	private Mono<Void> settleMatomoInstance(PMatomoInstance pmi, InstIds instids, Parameters mip, boolean retrievetoken, CloudFoundryMgrProperties.DbCreds dbCreds) {
		String uuid = pmi.getUuid(), instname = pmi.getName();
		return cfMgr.deployMatomoCfApp(pmi.getIdUrlStr(), pmi.getUuid(), pmi.getPlanId(), mip, pmi.getMemorySize(), pmi.getInstances())
				.doOnError(t -> {
					EntityManager nem = beginTx();
					PMatomoInstance npmi = miRepo.getOne(uuid);
					nem.unwrap(Session.class).update(npmi);
					LOGGER.debug("Async settle app instance (phase 2.1) \"" + npmi.getUuid() + "\" failed.", t);
					MatomoReleases.deleteLinkedTree(npmi.getIdUrlStr());
					savePMatomoInstance(npmi, OperationState.FAILED);
					commitTx(nem);
					instids.run();
				})
				.doOnSuccess(v -> {
					EntityManager nem = beginTx();
					PMatomoInstance npmi = miRepo.getOne(uuid);
					nem.unwrap(Session.class).update(npmi);
					MatomoReleases.deleteLinkedTree(npmi.getIdUrlStr());
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
								if (instname.equals(SHAREDINSTANCENAME) && uuid.equals(SHAREDINSTANCEINITUUID)) {
									sharedReady = true;
								}
								LOGGER.debug("Async settle app instance (phase 2) \"" + nnpmi.getUuid() + "\" succeeded");
							}
							commitTx(nnem);
						})
						.doOnTerminate(instids)
						.subscribe();
					} else {
						savePMatomoInstance(npmi, OperationState.SUCCEEDED);
						LOGGER.debug("Async settle app instance (phase 2) \"" + npmi.getUuid() + "\" succeeded");
						commitTx(nem);
						instids.run();
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
