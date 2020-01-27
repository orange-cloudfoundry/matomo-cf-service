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

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
		if (respMask.delayDeployCfApp() > 0) {
			LOGGER.debug("CFMGR-TEST::deployMatomoCfApp: create a Mono for delay");
			return Mono.create(sink -> {
				Mono.delay(Duration.ofSeconds(respMask.delayDeployCfApp()))
				.doOnError(t -> {
					LOGGER.debug("CFMGR-TEST::deployMatomoCfApp: pb with delay");
					sink.error(t);
					})
				.doOnSuccess(l -> {
					LOGGER.debug("CFMGR-TEST::deployMatomoCfApp: delay expired={}", respMask.delayDeployCfApp());
					respMask.setDelayDeployCfApp(0);
					sink.success();
					})
				.doOnSubscribe(v -> {
					LOGGER.debug("CFMGR-TEST::deployMatomoCfApp: start delay={}", respMask.delayDeployCfApp());
				})
				.subscribe();
			});
		}
		if (respMask.failedDeployCfAppAtOccur()) {
			LOGGER.debug("CFMGR-TEST::deployMatomoCfApp: create a Mono for error");
			return Mono.error(new TimeoutException("Timeout after some time"));
		}
		LOGGER.debug("CFMGR-TEST::deployMatomoCfApp: create a Mono for NOP");
		return Mono.create(sink -> {sink.success();});
	}

	@Override
	public Mono<Void> scaleMatomoCfApp(String instid, int instances, int memsize) {
		LOGGER.debug("CFMGR-TEST::scaleMatomoCfApp: instId={}, instances={}, memsize={}", instid, instances, memsize);
		return Mono.create(sink -> {sink.success();});
	}

	@Override
	public Mono<Void> createDedicatedDb(String instid, String planid) {
		LOGGER.debug("CFMGR-TEST::createDedicatedDb: instId={}, planid={}", instid, planid);
		if (respMask.failedCreateDedicatedDB()) {
			return Mono.error(new TimeoutException("Timeout after some time"));
		}
		return Mono.create(sink -> {sink.success();});
	}

	@Override
	public Mono<Void> deleteDedicatedDb(String instid, String planid) {
		LOGGER.debug("CFMGR-TEST::deleteDedicatedDb: instId={}, planid={}", instid, planid);
		if (respMask.failedDeleteDedicatedDB()) {
			return Mono.error(new TimeoutException("Timeout after some time"));
		}
		return Mono.create(sink -> {sink.success();});
	}

	@Override
	public Mono<Map<String, Object>> getApplicationEnv(String instid) {
		LOGGER.debug("CFMGR-TEST::getApplicationEnv: instId={}", instid);
		if (respMask.failedGetAppEnv()) {
			return Mono.error(new TimeoutException("Timeout after some time"));
		}
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
		if (respMask.failedDeleteMatomoCfApp()) {
			return Mono.error(new TimeoutException("Timeout after some time"));
		}
		return Mono.create(sink -> {sink.success();});
	}

	@Override
	public Mono<AppConfHolder> getInstanceConfigFile(String instid, String version, boolean clustermode) {
		LOGGER.debug("CFMGR-TEST::getInstanceConfigFile: instid={}, version={}, clusterMode={}", instid, version, clustermode);
		if (respMask.failedGetConfFile()) {
			return Mono.error(new IOException("Pb in file transfer"));
		}
		AppConfHolder appidh = new AppConfHolder();
		appidh.appId = "fake id";
		appidh.fileContent = "fake content".getBytes();
		return Mono.just(appidh);
	}

	@Override
	public boolean initializeMatomoInstance(String appcode, String nuri, String pwd, String planid) {
		LOGGER.debug("CFMGR-TEST::initializeMatomoInstance");
		return !respMask.failedInitializeMatomoInstance();
	}

	@Override
	public boolean upgradeMatomoInstance(String appcode, String nuri) {
		LOGGER.debug("CFMGR-TEST::upgradeMatomoInstance");
		return respMask.updateMatomoInstanceOK();
	}

	@Override
	public String getApiAccessToken(String dbcred, String instid, String planid) {
		LOGGER.debug("CFMGR-TEST::getApiAccessToken");
		if (respMask.failedGetApiAccessToken()) {
			return null;
		}
		return respMask.getAccessToken();
	}

	@Override
	public void deleteAssociatedDbSchema(PMatomoInstance pmi) {
		LOGGER.debug("CFMGR-TEST::deleteAssociatedDbSchema");
	}
}
