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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orange.oss.matomocfservice.web.domain.PMatomoInstance;
import com.orange.oss.matomocfservice.web.domain.Parameters;

import reactor.core.publisher.Mono;

/**
 * @author P. Déchamboux
 *
 */
public class CloudFoundryMgr4Test extends CloudFoundryMgrAbs {
	private final static Logger LOGGER = LoggerFactory.getLogger(CloudFoundryMgr4Test.class);
	private CfMgr4TResponseMask respMask;

	public void setResponseMask(CfMgr4TResponseMask m) {
		respMask = m;
	}

	@Override
	public void initialize() {
		LOGGER.debug("CFMGR-TEST::CloudFoundryMgr-initialize");
	}

	@Override
	public boolean isSmtpReady() {
		LOGGER.debug("CFMGR-TEST::CloudFoundryMgr-isSmtpReady");
		return respMask.isSmtpReady();
	}

	@Override
	public boolean isGlobalSharedReady() {
		LOGGER.debug("CFMGR-TEST::CloudFoundryMgr-isGlobalSharedReady");
		return respMask.isGlobalSharedReady();
	}

	@Override
	public Mono<Void> deployMatomoCfApp(String instid, String uuid, String planid, Parameters mip, int memsize, int nbinst) {
		LOGGER.debug("CFMGR-TEST::deployMatomoCfApp: instId={}", instid);
		return Mono.empty();
	}

	@Override
	public Mono<Void> scaleMatomoCfApp(String instid, int instances, int memsize) {
		LOGGER.debug("CFMGR-TEST::scaleMatomoCfApp: instId={}, instances={}, memsize={}", instid, instances, memsize);
		return Mono.empty();
	}

	@Override
	public Mono<Void> createDedicatedDb(String instid, String planid) {
		LOGGER.debug("CFMGR-TEST::createDedicatedDb: instId={}, planid={}", instid, planid);
		return Mono.empty();
	}

	@Override
	public Mono<Void> deleteDedicatedDb(String instid, String planid) {
		LOGGER.debug("CFMGR-TEST::deleteDedicatedDb: instId={}, planid={}", instid, planid);
		return Mono.empty();
	}

	@Override
	public Mono<Map<String, Object>> getApplicationEnv(String instid) {
		LOGGER.debug("CFMGR-TEST::getApplicationEnv: instId={}", instid);
		Map<String, Object> creds = new HashMap<String, Object>();
		creds.put("name", "fakeDbName");
		creds.put("hostname", "fakeHostName");
		creds.put("username", "fakeUserName");
		creds.put("password", "fakePassword");
		Map<String, Object> db = new HashMap<String, Object>();
		db.put("credentials", creds);
		List<Object> dbs = new ArrayList<Object>();
		dbs.add(db);
		Map<String, Object> serv = new HashMap<String, Object>();
		serv.put(respMask.getDbService(), dbs);
		Map<String, Object> envvcapserv = new HashMap<String, Object>();
		envvcapserv.put("VCAP_SERVICES", serv);
		return Mono.just(envvcapserv);
	}

	@Override
	public Mono<Void> deleteMatomoCfApp(String instid, String planid) {
		LOGGER.debug("CFMGR-TEST::deleteMatomoCfApp: instId={}", instid);
		return Mono.empty();
	}

	@Override
	public Mono<AppConfHolder> getInstanceConfigFile(String instid, String version, boolean clustermode) {
		LOGGER.debug("CFMGR-TEST::getInstanceConfigFile: instid={}, version={}, clusterMode={}", instid, version, clustermode);
		AppConfHolder appidh = new AppConfHolder();
		appidh.appId = "fake id";
		appidh.fileContent = "fake content".getBytes();
		return Mono.just(appidh);
	}

	@Override
	public boolean initializeMatomoInstance(String appcode, String nuri, String pwd, String planid) {
		LOGGER.debug("CFMGR-TEST::initializeMatomoInstance");
		return respMask.initializeMatomoInstanceOK();
	}

	@Override
	public boolean upgradeMatomoInstance(String appcode, String nuri) {
		LOGGER.debug("CFMGR-TEST::upgradeMatomoInstance");
		return respMask.updateMatomoInstanceOK();
	}

	@Override
	public String getApiAccessToken(String dbcred, String instid, String planid) {
		LOGGER.debug("CFMGR-TEST::getApiAccessToken");
		return respMask.getAccessToken();
	}

	@Override
	public void deleteAssociatedDbSchema(PMatomoInstance pmi) {
		LOGGER.debug("CFMGR-TEST::deleteAssociatedDbSchema");
	}
}
