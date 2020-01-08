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
	private boolean initializeMatomoInstance = true;
	private boolean updateMatomoInstance = true;
	private String dbService = "p-mysql";
	private String accessToken = "fakeToken";

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

	public CfMgr4TResponseMask initializeMatomoInstance(boolean v) {
		initializeMatomoInstance = v;
		return this;
	}
	public boolean initializeMatomoInstanceOK() {
		return initializeMatomoInstance;
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
}
