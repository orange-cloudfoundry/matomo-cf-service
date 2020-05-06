/*
 * Copyright 2020 Orange and the original author or authors.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.stereotype.Service;

import com.orange.oss.matomocfservice.web.domain.Parameters;
import com.orange.oss.matomocfservice.web.domain.PMatomoInstance;
import com.orange.oss.matomocfservice.web.domain.PMatomoInstance.PlatformKind;

/**
 * @author P. Déchamboux
 *
 */
@Service
public class BootstrapSharedInstance {
	private final static Logger LOGGER = LoggerFactory.getLogger(BootstrapSharedInstance.class);
	private final static String SHAREDINSTUUID = "3d39ad9e-8f85-11ea-990e-fb6e63a1745e";
	private final static String SHAREDINSTNAME = "mcfs-shared-matomo";
	@Autowired
	private MatomoInstanceService miServ;

	public String getSharedInstanceId() {
		return SHAREDINSTUUID;
	}

	public void initializeSharedInst(String servicename, String planid) {
		LOGGER.debug("SERV::initializeSharedInst: serviceName={}, planid={}", servicename, planid);
		PMatomoInstance pmi = miServ.getMatomoInstance(SHAREDINSTUUID, PlatformService.UNKNOWNPLATFORM_NAME);
		if (pmi != null) {
			if (pmi.getLastOperationState() == OperationState.SUCCEEDED) {
				LOGGER.debug("SERV::initializeSharedInst: shared mode ready to use !!");
				return;
			}
			// TODO: problem while initializing before: need remediation here !!
		}
		LOGGER.debug("SERV::initializeSharedInst: create shared Matomo instance");
//		miServ.createMatomoInstance(SHAREDINSTUUID, SHAREDINSTNAME, PlatformKind.CLOUDFOUNDRY, null,
//				planid, PlatformService.UNKNOWNPLATFORM_NAME,
//				new Parameters().autoVersionUpgrade(false).cfInstances(3));
	}
}
