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
import com.orange.oss.matomocfservice.web.domain.PMatomoInstance.PlatformKind;
import com.orange.oss.matomocfservice.web.domain.Parameters;

/**
 * @author P. Déchamboux
 *
 */
@SpringBootTest
public class TestMatomoInstanceService {
	private final static Logger LOGGER = LoggerFactory.getLogger(TestMatomoInstanceService.class);
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
	}

	@Test
	void testGetInstUnexistentPlatform() {
		LOGGER.debug("testGetInstUnexistentPlatform");
		Assertions.assertThrows(EntityNotFoundException.class, () -> {
			miService.getMatomoInstance(ID_NO_PF, "");
		});
	}

	@Test
	void testGetInstUnexistentInstance() {
		LOGGER.debug("testGetInstUnexistentInstance");
		Assertions.assertNull(miService.getMatomoInstance(unknownPfId, ID_NO_INST));
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
			PMatomoInstance pmi = miService.createMatomoInstance(instid, "m1", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(pmi);
			Assertions.assertEquals(OperationState.FAILED, pmi.getLastOperationState());
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
			PMatomoInstance pmi = miService.createMatomoInstance(instid, "m2", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(pmi);
			Assertions.assertEquals(OperationState.FAILED, pmi.getLastOperationState());
			miService.deleteMatomoInstance(instid, unknownPfId);
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
			PMatomoInstance pmi = miService.createMatomoInstance(instid, "m3", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(pmi);
			Assertions.assertEquals(OperationState.SUCCEEDED, pmi.getLastOperationState());
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}

	@Test
	void testGetInstURL() {
		LOGGER.debug("testGetInstURL");
		try {
			String instid = UUID.randomUUID().toString();
			cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
			PMatomoInstance pmi = miService.createMatomoInstance(instid, "m4", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(pmi);
			Assertions.assertEquals(OperationState.SUCCEEDED, pmi.getLastOperationState());
			Assertions.assertEquals("https://" + instid + "." + properties.getDomain(), miService.getInstanceUrl(pmi));
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
			PMatomoInstance pmi = miService.createMatomoInstance(instid, "m5", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(pmi);
			Assertions.assertEquals(OperationState.SUCCEEDED, pmi.getLastOperationState());
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
			PMatomoInstance pmi = miService.createMatomoInstance(instid, "m6", PlatformKind.CLOUDFOUNDRY, "https://apicf.foo.com", ServiceCatalogConfiguration.PLANDEDICATEDDB_UUID, unknownPfId, new Parameters());
			Assertions.assertNotNull(pmi);
			Assertions.assertEquals(OperationState.SUCCEEDED, pmi.getLastOperationState());
			miService.deleteMatomoInstance(instid, unknownPfId);
		} catch (Exception e) {
			Assertions.fail(e);
		}
	}
}
