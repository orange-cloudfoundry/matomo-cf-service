/**
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
package com.orange.oss.matomocfservice.cfmgr;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.oss.matomocfservice.web.domain.Parameters;

import reactor.core.publisher.Mono;

/**
 * @author P. Déchamboux
 *
 */
public class CloudFoundryMgr4Test extends CloudFoundryMgrAbs {
	private final static Logger LOGGER = LoggerFactory.getLogger(CloudFoundryMgr.class);

	@Override
	public void initialize() {
		LOGGER.debug("CFMGR-TEST::CloudFoundryMgr-initialize");
	}

	@Override
	public boolean isSmtpReady() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isMatomoSharedReady() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isGlobalSharedReady() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getInstanceUrl(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	public Mono<Void> deployMatomoCfApp(String instid, String uuid, String planid, Parameters mip, int memsize, int nbinst) {
		LOGGER.debug("CFMGR::deployMatomoCfApp: instId={}", instid);
		return Mono.empty();
	}

	public Mono<Void> scaleMatomoCfApp(String instid, int instances, int memsize) {
		LOGGER.debug("CFMGR::scaleMatomoCfApp: instId={}, instances={}, memsize={}", instid, instances, memsize);
		return Mono.empty();
	}

	public Mono<Void> createDedicatedDb(String instid, String planid) {
		LOGGER.debug("CFMGR::createDedicatedDb: instId={}, planid={}", instid, planid);
		return Mono.empty();
	}

	public Mono<Void> deleteDedicatedDb(String instid, String planid) {
		LOGGER.debug("CFMGR::deleteDedicatedDb: instId={}, planid={}", instid, planid);
		return Mono.empty();
	}

	public Mono<Map<String, Object>> getApplicationEnv(String instid) {
		LOGGER.debug("CFMGR::getApplicationEnv: instId={}", instid);
		return Mono.empty();
	}

	public Mono<Void> deleteMatomoCfApp(String instid, String planid) {
		LOGGER.debug("CFMGR::deleteMatomoCfApp: instId={}", instid);
		return Mono.empty();
	}

	public Mono<AppConfHolder> getInstanceConfigFile(String instid, String version, boolean clustermode) {
		LOGGER.debug("CFMGR::getInstanceConfigFile: instid={}, version={}, clusterMode={}", instid, version, clustermode);
		return Mono.empty();
	}
}
