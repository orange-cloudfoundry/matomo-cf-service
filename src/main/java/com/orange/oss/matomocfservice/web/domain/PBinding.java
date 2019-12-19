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

package com.orange.oss.matomocfservice.web.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.cloud.servicebroker.model.instance.OperationState;

/**
 * @author P. Déchamboux
 *
 */
@Entity
@Table(name = "bindings")
public class PBinding extends POperationStatus {
	private final static int LENGTH_SITENAME = 128;
	private final static int LENGTH_SITEURL = 1024;
	private final static int LENGTH_ADMINEMAIL = 256;
	private final static int LENGTH_USERNAME = 16;
	private final static int LENGTH_PASSWORD = 16;

	@ManyToOne
	@JoinColumn(name = "matomo_instance_id")
	private final PMatomoInstance pmatomoInstance;
	
	@Column(length = LENGTH_ID)
	private final String appId;

	@Column(length = LENGTH_SITENAME)
	private final String siteName;

	@Column(length = LENGTH_ADMINEMAIL)
    private String adminEmail;

	@Column(length = LENGTH_SITEURL)
    private String trackedUrl;

	@Column(length = LENGTH_SITEURL)
    private final String matomoUrl;

	private int siteId;

	@Column(length = LENGTH_USERNAME)
	private final String userName;

	@Column(length = LENGTH_PASSWORD)
	private final String password;

	@SuppressWarnings("unused")
	protected PBinding() {
		super();
		this.pmatomoInstance = null;
		this.appId = null;
		this.siteName = null;
		this.trackedUrl = null;
		this.adminEmail = null;
		this.matomoUrl = null;
		this.siteId = -1;
		this.userName = null;
		this.password = null;
	}

	public PBinding(String id, PMatomoInstance pmi, String appid, String sitename, String trackedurl, String adminemail, PPlatform ppf, String matomoUrl) {
		super(id, POperationStatus.OpCode.CREATE_SERVICE_INSTANCE_APP_BINDING, OperationState.IN_PROGRESS, ppf);
		this.pmatomoInstance = pmi;
		this.appId = appid;
		this.siteName = sitename;
		this.trackedUrl = trackedurl;
		this.adminEmail = adminemail;
		this.matomoUrl = matomoUrl;
		this.siteId = -1;
		this.userName = RandomStringUtils.randomAlphanumeric(LENGTH_USERNAME);
		this.password = RandomStringUtils.randomAlphanumeric(LENGTH_PASSWORD);
	}

	public PMatomoInstance getPMatomoInstance() {
		return this.pmatomoInstance;
	}

	public String getSiteName() {
		return this.siteName;
	}

	public String getTrackedUrl() {
		return this.trackedUrl;
	}

	public String getAdminEmail() {
		return this.adminEmail;
	}

	public void setSiteId(int siteid) {
		this.siteId = siteid;
	}

	public String getMatomoUrl() {
		return this.matomoUrl;
	}

	public int getSiteId() {
		return this.siteId;
	}

	public String getUserName() {
		return this.userName;
	}

	public String getPassword() {
		return this.password;
	}
}
