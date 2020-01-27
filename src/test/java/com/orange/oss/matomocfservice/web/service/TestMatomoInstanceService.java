/**
 * Copyright 2019-2020 Orange and the original author or authors.
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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.instance.OperationState;

import com.orange.oss.matomocfservice.cfmgr.CfMgr4TResponseMask;
import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgr;
import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgr4Test;
import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgrProperties;
import com.orange.oss.matomocfservice.servicebroker.ServiceCatalogConfiguration;
import com.orange.oss.matomocfservice.web.domain.PMatomoInstance;
import com.orange.oss.matomocfservice.web.domain.POperationStatus;
import com.orange.oss.matomocfservice.web.domain.PMatomoInstance.PlatformKind;
import com.orange.oss.matomocfservice.web.domain.Parameters;
import com.orange.oss.matomocfservice.web.service.OperationStatusService.OperationAndState;

/**
 * @author P. Déchamboux
 *
 */
@SpringBootTest
public class TestMatomoInstanceService {
	private final static Logger LOGGER = LoggerFactory.getLogger(TestMatomoInstanceService.class);
	@Autowired
	CloudFoundryMgrProperties cfMgrProp;
	private int max_instid = -1;
	@Autowired
	InstanceIdMgr instIdMgr;
	@Autowired
	ApplicationInformation applicationInformation;
	@Autowired
	MatomoInstanceService miService;
	@Autowired
	PlatformService pfService;
	@Autowired
	CloudFoundryMgrProperties properties;
	@Autowired
	CloudFoundryMgr cfMgr;
	CloudFoundryMgr4Test cfMgr4T = null;
	private final static String ID_NO_PF = UUID.randomUUID().toString();
	private final static String ID_NO_INST = UUID.randomUUID().toString();
	private String unknownPfId = null;

	@BeforeEach
	void intializedService() {
		LOGGER.debug("intializedService");
		if (unknownPfId == null) {
			unknownPfId = pfService.getUnknownPlatformId();
		}
		if (cfMgr4T == null) {
			cfMgr4T = (CloudFoundryMgr4Test)cfMgr;
		}
		if (max_instid == -1) {
			max_instid = cfMgrProp.getMaxServiceInstances();
		}
		LOGGER.debug("Inst allocated: {}", instIdMgr.getNbAllocatedInstanceId());
	}

	@Test
	void testGetLastOperationAndStateKONoId() {
		LOGGER.debug("testGetLastOperationAndStateKONoId");
		try {
			OperationAndState oas = miService.getLastOperationAndState(ID_NO_INST, unknownPfId);
			Assertions.assertNull(oas);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testGetLastOperationAndStateKOWrongPF() {
		LOGGER.debug("testGetLastOperationAndStateKOWrongPF");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
			miService.createMatomoInstance(instid, "m0", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			OperationAndState oas = miService.getLastOperationAndState(instid, ID_NO_PF);
			Assertions.assertNull(oas);
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testGetLastOperationAndStateKOInProgressTimeout() {
		LOGGER.debug("testGetLastOperationAndStateKOInProgressTimeout");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask().setDelayDeployCfApp(applicationInformation.getTimeoutFrozenInProgress() + 4));
			miService.createMatomoInstance(instid, "m1", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			TimeUnit.SECONDS.sleep(applicationInformation.getTimeoutFrozenInProgress() + 2);
			OperationAndState oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertNotNull(oas);
			Assertions.assertEquals(POperationStatus.OpCode.CREATE_SERVICE_INSTANCE, oas.getOperation());
			Assertions.assertEquals(OperationState.FAILED, oas.getState());
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			e.printStackTrace();
			Assertions.fail(e);
		}
	}

	@Test
	void testGetInstKOWrongPF() {
		LOGGER.debug("testGetInstKOWrongPF");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
			String uuid = miService.createMatomoInstance(instid, "m2", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			PMatomoInstance pmi = miService.getMatomoInstance(instid, ID_NO_PF);
			Assertions.assertNull(pmi);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testGetInstUnexistentInstance() {
		LOGGER.debug("testGetInstUnexistentInstance");
		Assertions.assertNull(miService.getMatomoInstance(ID_NO_INST, unknownPfId));
	}

	@Test
	void testGetInstURL() {
		LOGGER.debug("testGetInstURL");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
			String uuid = miService.createMatomoInstance(instid, "m3", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			Assertions.assertEquals("https://" + instid + "." + properties.getDomain(), miService.getInstanceUrl(uuid));
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testGetInstance() {
		LOGGER.debug("testGetInstance");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
			String uuid = miService.createMatomoInstance(instid, "m4", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			PMatomoInstance pmi = miService.getMatomoInstance(instid, unknownPfId);
			Assertions.assertNotNull(pmi);
			Assertions.assertEquals(uuid, pmi.getUuid());
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testGetInstanceKOUnexistent() {
		LOGGER.debug("testGetInstanceKOUnexistent");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
			PMatomoInstance pmi = miService.getMatomoInstance(instid, unknownPfId);
			Assertions.assertNull(pmi);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testFindInstUnexistentPlatform() {
		LOGGER.debug("testFindInstUnexistentPlatform");
		Assertions.assertThrows(EntityNotFoundException.class, () -> {
			miService.findMatomoInstance(ID_NO_PF);
		});
	}

	@Test
	void testFindInstNoInstance() {
		LOGGER.debug("testFindInstNoInstance");
		Assertions.assertDoesNotThrow(() -> {
			miService.findMatomoInstance(unknownPfId).isEmpty();
		});
	}

	@Test
	void testCreateInstanceUnexistentPlatform() {
		LOGGER.debug("testCreateInstanceUnexistentPlatform");
		Assertions.assertThrows(EntityNotFoundException.class, () -> {
			miService.createMatomoInstance("", "", PlatformKind.CLOUDFOUNDRY, "", "", ID_NO_PF, new Parameters());
		});
	}

	@Test
	void testCreateInstanceGlobSharedKONoSmtp() {
		LOGGER.debug("testCreateInstanceGlobSharedKONoSmtp");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask().smtpReady(false));
			String uuid = miService.createMatomoInstance(instid, "m5", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			OperationAndState oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.FAILED, oas.getState());
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testCreateInstanceGlobSharedKONoSharedDB() {
		LOGGER.debug("testCreateInstanceGlobSharedKONoSharedDB");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask().globalSharedReady(false));
			String uuid = miService.createMatomoInstance(instid, "m6", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			OperationAndState oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.FAILED, oas.getState());
			String msg = miService.deleteMatomoInstance(instid, unknownPfId);
			LOGGER.debug("delete msg: {}", msg);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testCreateInstanceGlobSharedOK() {
		LOGGER.debug("testCreateInstanceGlobSharedOK");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
			String uuid = miService.createMatomoInstance(instid, "m7", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			OperationAndState oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(POperationStatus.OpCode.CREATE_SERVICE_INSTANCE, oas.getOperation());
			Assertions.assertEquals(OperationState.SUCCEEDED, oas.getState());
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testCreateInstanceGlobalSharedKOAlreadyExist() {
		LOGGER.debug("testCreateInstanceGlobalSharedKOAlreadyExist");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
			String uuid = miService.createMatomoInstance(instid, "m8", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			OperationAndState oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.SUCCEEDED, oas.getState());
			uuid = miService.createMatomoInstance(instid, "m7", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNull(uuid);
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testCreateInstanceGlobalSharedKOFailedCfDeploy1() {
		LOGGER.debug("testCreateInstanceGlobalSharedKOFailedCfDeploy1");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask().setFailedDeployCfAppAtOccur(1));
			String uuid = miService.createMatomoInstance(instid, "m9", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			OperationAndState oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.FAILED, oas.getState());
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testCreateInstanceGlobalSharedKOFailedInitialize() {
		LOGGER.debug("testCreateInstanceGlobalSharedKOFailedInitialize");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask().setFailedInitialize());
			String uuid = miService.createMatomoInstance(instid, "m10", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			OperationAndState oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.FAILED, oas.getState());
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testCreateInstanceGlobalSharedKOFailedGetConfFile() {
		LOGGER.debug("testCreateInstanceGlobalSharedKOFailedGetConfFile");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask().setFailedGetConfFile());
			String uuid = miService.createMatomoInstance(instid, "m11", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			OperationAndState oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.FAILED, oas.getState());
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testCreateInstanceGlobalSharedKOFailedCfDeploy2() {
		LOGGER.debug("testCreateInstanceGlobalSharedKOFailedCfDeploy2");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask().setFailedDeployCfAppAtOccur(2));
			String uuid = miService.createMatomoInstance(instid, "m12", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			OperationAndState oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.FAILED, oas.getState());
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testCreateInstanceGlobalSharedKOFailedCfGetAppEnv() {
		LOGGER.debug("testCreateInstanceGlobalSharedKOFailedCfGetAppEnv");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask().setFailedGetAppEnv());
			String uuid = miService.createMatomoInstance(instid, "m13", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			OperationAndState oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.FAILED, oas.getState());
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testCreateInstanceGlobalSharedKOFailedGetApiAccessToken() {
		LOGGER.debug("testCreateInstanceGlobalSharedKOFailedGetApiAccessToken");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask().setFailedGetApiAccessToken());
			String uuid = miService.createMatomoInstance(instid, "m14", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			OperationAndState oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.FAILED, oas.getState());
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testCreateInstanceMatomoSharedOK() {
		LOGGER.debug("testCreateInstanceMatomoSharedOK");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
			String uuid = miService.createMatomoInstance(instid, "m15", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			OperationAndState oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.SUCCEEDED, oas.getState());
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testCreateInstanceMatomoSharedOKMaxNb() {
		LOGGER.debug("testCreateInstanceMatomoSharedOKMaxNb");
		String[] instids = new String[max_instid];
		for (int i = 0; i < max_instid; i++) {
			instids[i] = null;
		}
		try {
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
			for (int i = 0; i < max_instid; i++) {
				instids[i] = miService.createMatomoInstance(UUID.randomUUID().toString(), "mc-" + i, PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID, unknownPfId, new Parameters());
				Assertions.assertNotNull(instids[i]);
				OperationAndState oas = miService.getLastOperationAndState(instids[i], unknownPfId);
				Assertions.assertEquals(OperationState.SUCCEEDED, oas.getState());
			}
			for (int i = 0; i < max_instid; i++) {
				miService.deleteMatomoInstance(instids[i], unknownPfId);
			}
		} catch (Exception e) {
			int alloc = 0;
			for (int i = 0; i < max_instid; i++) {
				if (instids[i] != null) {
					miService.deleteMatomoInstance(instids[i], unknownPfId);
					alloc++;
				}
			}
			Assertions.fail("Manage to allocate " + alloc + " rather than " + max_instid, e);
		}
	}

	@Test
	void testCreateInstanceMatomoSharedKOMaxNb() {
		LOGGER.debug("testCreateInstanceMatomoSharedKOMaxNb");
		String[] instids = new String[max_instid];
		for (int i = 0; i < max_instid; i++) {
			instids[i] = null;
		}
		try {
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
			for (int i = 0; i < max_instid; i++) {
				instids[i] = UUID.randomUUID().toString();
				String uuid = miService.createMatomoInstance(instids[i], "mf-" + i, PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID, unknownPfId, new Parameters());
				Assertions.assertNotNull(uuid);
				OperationAndState oas = miService.getLastOperationAndState(instids[i], unknownPfId);
				Assertions.assertEquals(OperationState.SUCCEEDED, oas.getState());
			}
			miService.createMatomoInstance(UUID.randomUUID().toString(), "mout", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.fail("Could not be able to allocate more!!");
		} catch (Exception e) {
			LOGGER.debug("Out of instance!!", e);
			for (int i = 0; i < max_instid; i++) {
				if (instids[i] != null) {
					miService.deleteMatomoInstance(instids[i], unknownPfId);
				}
			}
		}
	}

	@Test
	void testCreateInstanceMatomoSharedKOFailedCreateDB() {
		LOGGER.debug("testCreateInstanceMatomoSharedKOFailedCreateDB");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask().setFailedCreateDedicatedDB());
			String uuid = miService.createMatomoInstance(instid, "m16", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			OperationAndState oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.FAILED, oas.getState());
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testCreateInstanceDedicatedOK() {
		LOGGER.debug("testCreateInstanceDedicatedOK");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
			String uuid = miService.createMatomoInstance(instid, "m17", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANDEDICATEDDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			OperationAndState oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.SUCCEEDED, oas.getState());
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testCreateInstanceDedicatedKOFailedCreateDB() {
		LOGGER.debug("testCreateInstanceDedicatedKOFailedCreateDB");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask().setFailedCreateDedicatedDB());
			String uuid = miService.createMatomoInstance(instid, "m18", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANDEDICATEDDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			OperationAndState oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.FAILED, oas.getState());
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testDeleteInstanceOK() {
		LOGGER.debug("testDeleteInstanceOK");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
			String uuid = miService.createMatomoInstance(instid, "m19", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			OperationAndState oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.SUCCEEDED, oas.getState());
			String msg = miService.deleteMatomoInstance(instid, unknownPfId);
			Assertions.assertNotNull(msg);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testDeleteInstanceKOUnexistent() {
		LOGGER.debug("testDeleteInstanceKOUnexistent");
		try {
			String instid = UUID.randomUUID().toString();
			String msg = miService.deleteMatomoInstance(instid, unknownPfId);
			Assertions.assertNull(msg);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testDeleteInstanceKOWrongPF() {
		LOGGER.debug("testDeleteInstanceKOWrongPF");
		String instid = UUID.randomUUID().toString();
		try {
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
			String uuid = miService.createMatomoInstance(instid, "m20", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			OperationAndState oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.SUCCEEDED, oas.getState());
			String msg = miService.deleteMatomoInstance(instid, ID_NO_PF);
			Assertions.assertNull(msg);
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testDeleteInstanceFailed() {
		LOGGER.debug("testDeleteInstanceFailed");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask().setFailedDeleteMatomoCfApp());
			String uuid = miService.createMatomoInstance(instid, "m21", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			OperationAndState oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.SUCCEEDED, oas.getState());
			String msg = miService.deleteMatomoInstance(instid, unknownPfId);
			oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.FAILED, oas.getState());
			Assertions.assertNotNull(msg);
			msg = miService.deleteMatomoInstance(instid, unknownPfId);
			oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.SUCCEEDED, oas.getState());
			Assertions.assertNotNull(msg);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testDeleteInstanceFailedDelDedDbKO() {
		LOGGER.debug("testDeleteInstanceFailedDelDedDbKO");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask().setFailedDeleteDedicatedDB());
			String uuid = miService.createMatomoInstance(instid, "m22", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(uuid);
			OperationAndState oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.SUCCEEDED, oas.getState());
			String msg = miService.deleteMatomoInstance(instid, unknownPfId);
			oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.FAILED, oas.getState());
			Assertions.assertNotNull(msg);
			msg = miService.deleteMatomoInstance(instid, unknownPfId);
			oas = miService.getLastOperationAndState(instid, unknownPfId);
			Assertions.assertEquals(OperationState.SUCCEEDED, oas.getState());
			Assertions.assertNotNull(msg);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}
}
