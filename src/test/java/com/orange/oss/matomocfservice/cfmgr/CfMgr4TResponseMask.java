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
package com.orange.oss.matomocfservice.cfmgr;

/**
 * @author P. Déchamboux
 *
 */
public class CfMgr4TResponseMask {
	private boolean smtpReady = true;
	private boolean globalSharedReady = true;
	private boolean failedInitializeMatomoInstance = false;
	private boolean updateMatomoInstance = true;
	private String dbService = "p-mysql";
	private String accessToken = "fakeToken";
	private boolean failedCreateDedicatedDB = false;
	private boolean failedDeleteDedicatedDB = false;
	private int delayDeployCfApp = 0;
	private int failedDeployCfAppAtOccur = 0;
	private int deployCfAppOccur = 0;
	private boolean failedScaleMatomoCfApp = false;
	private boolean failedGetConfFile = false;
	private boolean failedGetAppEnv = false;
	private boolean failedGetApiAccessToken = false;
	private boolean failedDeleteMatomoCfApp = false;

	public CfMgr4TResponseMask() {
	}

	public CfMgr4TResponseMask smtpReady(boolean v) {
		smtpReady = v;
		return this;
	}
	public boolean isSmtpReady() {
		return smtpReady;
	}

	public CfMgr4TResponseMask globalSharedReady(boolean v) {
		globalSharedReady = v;
		return this;
	}
	public boolean isGlobalSharedReady() {
		return globalSharedReady;
	}

	public CfMgr4TResponseMask setFailedInitialize() {
		failedInitializeMatomoInstance = true;
		return this;
	}
	public boolean failedInitializeMatomoInstance() {
		return failedInitializeMatomoInstance;
	}

	public CfMgr4TResponseMask updateMatomoInstance(boolean v) {
		updateMatomoInstance = v;
		return this;
	}
	public boolean updateMatomoInstanceOK() {
		return updateMatomoInstance;
	}

	public CfMgr4TResponseMask setDbService(String v) {
		dbService = v;
		return this;
	}
	public String getDbService() {
		return dbService;
	}

	public CfMgr4TResponseMask setAccessToken(String v) {
		accessToken = v;
		return this;
	}
	public String getAccessToken() {
		return accessToken;
	}

	public CfMgr4TResponseMask setFailedCreateDedicatedDB() {
		failedCreateDedicatedDB = true;
		return this;
	}
	public boolean failedCreateDedicatedDB() {
		return failedCreateDedicatedDB;
	}

	public CfMgr4TResponseMask setFailedDeleteDedicatedDB() {
		failedDeleteDedicatedDB = true;
		return this;
	}
	public boolean failedDeleteDedicatedDB() {
		return failedDeleteDedicatedDB;
	}

	public CfMgr4TResponseMask setFailedDeployCfAppAtOccur(int v) {
		failedDeployCfAppAtOccur = v;
		return this;
	}
	public boolean failedDeployCfAppAtOccur() {
		if (failedDeployCfAppAtOccur == 0) {
			return false;
		}
		deployCfAppOccur++;
		if (deployCfAppOccur == failedDeployCfAppAtOccur) {
			return true;
		}
		return false;
	}

	public CfMgr4TResponseMask setDelayDeployCfApp(int v) {
		delayDeployCfApp = v;
		return this;
	}
	public int delayDeployCfApp() {
		return delayDeployCfApp;
	}

	public CfMgr4TResponseMask setFailedScaleMatomoCfApp() {
		failedScaleMatomoCfApp = true;
		return this;
	}
	public boolean failedScaleMatomoCfApp() {
		return failedGetConfFile;
	}

	public CfMgr4TResponseMask setFailedGetConfFile() {
		failedGetConfFile = true;
		return this;
	}
	public boolean failedGetConfFile() {
		return failedGetConfFile;
	}

	public CfMgr4TResponseMask setFailedGetAppEnv() {
		failedGetAppEnv = true;
		return this;
	}
	public boolean failedGetAppEnv() {
		return failedGetAppEnv;
	}

	public CfMgr4TResponseMask setFailedGetApiAccessToken() {
		failedGetApiAccessToken = true;
		return this;
	}
	public boolean failedGetApiAccessToken() {
		return failedGetApiAccessToken;
	}

	public CfMgr4TResponseMask setFailedDeleteMatomoCfApp() {
		failedDeleteMatomoCfApp = true;
		return this;
	}
	public boolean failedDeleteMatomoCfApp() {
		return failedDeleteMatomoCfApp;
	}
}
