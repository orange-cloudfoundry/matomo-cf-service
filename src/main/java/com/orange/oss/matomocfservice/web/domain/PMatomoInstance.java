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

import org.springframework.cloud.servicebroker.model.instance.OperationState;

import com.orange.oss.matomocfservice.servicebroker.ServiceCatalogConfiguration;


/**
 * @author P. Déchamboux
 * 
 */
@Entity
@Table(name = "matomoinstances")
public class PMatomoInstance extends POperationStatus {
	private final static int LENGTH_IDURL = 16;
	private final static int LENGTH_SERVDEFID = 128;
	private final static int LENGTH_NAME = 80;
	private final static int LENGTH_PFKIND = 16;
	private final static int LENGTH_PFAPI = 256;
	private final static int LENGTH_PLANID = 64;
	private final static int LENGTH_INSTVERS = 8;
	private final static int LENGTH_PASSWORD = 12;
	private final static int LENGTH_DBCRED = 512;
	private final static int LENGTH_TOKENAUTH = 48;
	private final static int LENGTH_INSTINITFILE = 4096;
	private final static int LENGTH_TIMEZONE = 48;
	
	private int idUrl;

	@Column(length = LENGTH_SERVDEFID)
	private final String serviceDefinitionId;

	@Column(length = LENGTH_NAME)
	private final String name;
	
	@Column(length = LENGTH_PFKIND)
	private String platformKind;
	
	@Column(length = LENGTH_PFAPI)
	private String platformApiLocation;
	
	@Column(length = LENGTH_PLANID)
	private String planId;
	
	@Column(length = LENGTH_INSTVERS)
	private String installedVersion;
	
	@Column(length = LENGTH_PASSWORD)
	private String password;
	
	@Column(length = LENGTH_DBCRED)
	private String dbCred;
	
	@Column(length = LENGTH_TOKENAUTH)
	private String tokenAuth;
	
	@Column(length = LENGTH_INSTINITFILE)
	private byte[] configFileContent;
	
	private boolean automaticVersionUpgrade;
	
	private int instances;
	
	private int memorySize;

	@Column(length = LENGTH_TIMEZONE)
	private String timeZone;

	@ManyToOne
	@JoinColumn(name = "shared_matomo_instance_id")
	private PMatomoInstance sharedInstance;

	protected PMatomoInstance() {
		super();
		this.idUrl = -1;
		this.serviceDefinitionId = null;
		this.name = null;
		this.platformKind = null;
		this.platformApiLocation = null;
		this.planId = null;
		this.platform = null;
		this.installedVersion = null;
		this.password = null;
		this.dbCred = null;
		this.tokenAuth = null;
		this.configFileContent = null;
		this.automaticVersionUpgrade = true;
		this.instances = 0;
		this.memorySize = 0;
		this.timeZone = null;
		this.sharedInstance = null;
	}

	public PMatomoInstance(String uuid, int idUrl, String name, PlatformKind pfkind, String pfapi, String planid, PPlatform pf, Parameters mip) {
		super(uuid, POperationStatus.OpCode.CREATE_SERVICE_INSTANCE, OperationState.IN_PROGRESS, pf);
		this.idUrl = idUrl;
		this.serviceDefinitionId = "matomo-cf-service";
		this.name = name;
		this.platformKind = pfkind.toString();
		this.platformApiLocation = pfapi;
		this.planId = planid;
		this.installedVersion = mip.getVersion();
		this.password = "piwikpw"/*RandomStringUtils.randomAlphanumeric(LENGTH_PASSWORD)*/;
		this.tokenAuth = null;
		this.configFileContent = null;
		this.automaticVersionUpgrade = mip.isAutoVersionUpgrade();
		this.instances = mip.getCfInstances();
		this.memorySize = mip.getMemorySize();
		this.timeZone = mip.getTimeZone();
	}

	public PMatomoInstance(String uuid, int idUrl, String name, PlatformKind pfkind, String pfapi, String planid, PPlatform pf, Parameters mip, PMatomoInstance shared) {
		this(uuid, idUrl, name, pfkind, pfapi, planid, pf, mip);
		this.configFileContent = "fake_content".getBytes();
		this.sharedInstance = shared;
	}

	public boolean isSharedPlan() {
		return this.planId.equals(ServiceCatalogConfiguration.PLANSHARED_UUID);
	}

	public PMatomoInstance getSharedInstance() {
		return this.sharedInstance;
	}

	public int getIdUrl() {
		return this.idUrl;
	}

	public String getIdUrlStr() {
		String res;
		res = Integer.toHexString(this.idUrl).toUpperCase();
		while (res.length() < LENGTH_IDURL) {
			res = "0" + res;
		}
		return res;
	}

	public String getServiceDefinitionId() {
		return this.serviceDefinitionId;
	}

	public String getName() {
		return this.name;
	}

	public PlatformKind getPlatformKind() {
		return PlatformKind.valueOf(platformKind);
	}

	public String getPlatformApiLocation() {
		return platformApiLocation;
	}
	public void setPlatformApiLocation(String api) {
		this.platformApiLocation = api;
	}

	public String getPlanId() {
		return this.planId;
	}

	public boolean getClusterMode() {
		if (this.planId.equals(ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID)) {
			return false;
		}
		return true;
	}

	public byte[] getConfigFileContent() {
		return this.configFileContent;
	}

	public void setConfigFileContent(byte cfc[]) {
		this.configFileContent = cfc;
	}

	public boolean getAutomaticVersionUpgrade() {
		if (this.planId.equals(ServiceCatalogConfiguration.PLANSHARED_UUID)) {
			return this.sharedInstance.getAutomaticVersionUpgrade();
		}
		return this.automaticVersionUpgrade;
	}

	public void setAutomaticVersionUpgrade(boolean avu) {
		if (this.planId.equals(ServiceCatalogConfiguration.PLANSHARED_UUID)) {
			return;
		}
		this.automaticVersionUpgrade = avu;
		super.touch();
	}

	public int getInstances() {
		if (this.planId.equals(ServiceCatalogConfiguration.PLANSHARED_UUID)) {
			return this.sharedInstance.getInstances();
		}
		return this.instances;
	}

	public void setIntances(int instances) {
		if (this.planId.equals(ServiceCatalogConfiguration.PLANSHARED_UUID)) {
			return;
		}
		this.instances = instances;
		super.touch();
	}

	public int getMemorySize() {
		if (this.planId.equals(ServiceCatalogConfiguration.PLANSHARED_UUID)) {
			return this.sharedInstance.getMemorySize();
		}
		return this.memorySize;
	}

	public void setMemorySize(int memorysize) {
		if (this.planId.equals(ServiceCatalogConfiguration.PLANSHARED_UUID)) {
			return;
		}
		this.memorySize = memorysize;
		super.touch();
	}

	public String getTimeZone() {
		if (this.planId.equals(ServiceCatalogConfiguration.PLANSHARED_UUID)) {
			return this.sharedInstance.getTimeZone();
		}
		return this.timeZone;
	}

	public void setTimeZone(String timezone) {
		if (this.planId.equals(ServiceCatalogConfiguration.PLANSHARED_UUID)) {
			return;
		}
		this.timeZone = timezone;
		super.touch();
	}

	public void setInstalledVersion(String instVers) {
		if (this.planId.equals(ServiceCatalogConfiguration.PLANSHARED_UUID)) {
			return;
		}
		this.installedVersion = instVers;
		super.touch();
	}

	public String getInstalledVersion() {
		if (this.planId.equals(ServiceCatalogConfiguration.PLANSHARED_UUID)) {
			return this.sharedInstance.getInstalledVersion();
		}
		return this.installedVersion;
	}

	public String getPassword() {
		if (this.planId.equals(ServiceCatalogConfiguration.PLANSHARED_UUID)) {
			return null;
		}
		return this.password;
	}

	public void setDbCred(String dbc) {
		if (this.planId.equals(ServiceCatalogConfiguration.PLANSHARED_UUID)) {
			return;
		}
		this.dbCred = dbc;
	}

	public String getDbCred() {
		if (this.planId.equals(ServiceCatalogConfiguration.PLANSHARED_UUID)) {
			return null;
		}
		return this.dbCred;
	}

	public void setTokenAuth(String ta) {
		if (this.planId.equals(ServiceCatalogConfiguration.PLANSHARED_UUID)) {
			return;
		}
		this.tokenAuth = ta;
	}

	public String getTokenAuth() {
		if (this.planId.equals(ServiceCatalogConfiguration.PLANSHARED_UUID)) {
			return this.sharedInstance.getTokenAuth();
		}
		return this.tokenAuth;
	}

	public Parameters getParameters() {
		return new Parameters()
				.version(getInstalledVersion())
				.autoVersionUpgrade(getAutomaticVersionUpgrade())
				.cfInstances(getInstances())
				.memorySize(getMemorySize())
				.timeZone(getTimeZone());
	}

	public enum PlatformKind {
		CLOUDFOUNDRY ("CLOUDFOUNDRY"),
		KUBERNETES ("KUBERNETES"),
		OPENSHIFT ("OPENSHIFT"),
		OTHER ("OTHER");

		private String value;

		private PlatformKind(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}
	}
}
