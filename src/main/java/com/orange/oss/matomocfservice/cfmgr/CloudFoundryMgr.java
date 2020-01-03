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

package com.orange.oss.matomocfservice.cfmgr;

import java.util.Map;

import com.orange.oss.matomocfservice.web.domain.Parameters;

import reactor.core.publisher.Mono;

/**
 * @author P. Déchamboux
 *
 */
public interface CloudFoundryMgr {
	public final static long CREATEDBSERV_TIMEOUT = 90; // in minutes

	public void initialize();
	public boolean isSmtpReady();
	public boolean isMatomoSharedReady();
	public boolean isGlobalSharedReady();
	public String getAppName(String appcode);
	public String getTablePrefix(String appcode, String planid);
	public String getInstanceUrl(String uuid);
	public Mono<Void> deployMatomoCfApp(String instid, String uuid, String planid, Parameters mip, int memsize, int nbinst);
	public Mono<Void> scaleMatomoCfApp(String instid, int instances, int memsize);
	public Mono<Void> createDedicatedDb(String instid, String planid);
	public Mono<Void> deleteDedicatedDb(String instid, String planid);
	public Mono<Map<String, Object>> getApplicationEnv(String instid);
	public Mono<Void> deleteMatomoCfApp(String instid, String planid);
	public Mono<AppConfHolder> getInstanceConfigFile(String instid, String version, boolean clustermode);

	public class AppConfHolder {
		public String appId = null;
		public byte[] fileContent = null;
	}
}
