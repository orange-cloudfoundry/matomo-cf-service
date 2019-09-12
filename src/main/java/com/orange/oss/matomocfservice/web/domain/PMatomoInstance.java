/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.web.domain;

import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.springframework.cloud.servicebroker.model.instance.OperationState;

import com.orange.oss.matomocfservice.api.model.OpCode;
import com.orange.oss.matomocfservice.api.model.PlatformKind;

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
	private int idUrl;

	@Column(length = LENGTH_SERVDEFID)
	private final String serviceDefinitionId;

	@Column(length = LENGTH_NAME)
	private final String name;

	private final ZonedDateTime createTime;

	private ZonedDateTime updateTime;
	
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

	@SuppressWarnings("unused")
	protected PMatomoInstance() {
		super();
		this.idUrl = -1;
		this.serviceDefinitionId = null;
		this.name = null;
		this.createTime = null;
		this.updateTime = null;
		this.platformKind = null;
		this.platformApiLocation = null;
		this.planId = null;
		this.platform = null;
		this.installedVersion = null;
		this.password = null;
		this.dbCred = null;
		this.tokenAuth = null;
		this.configFileContent = null;
	}

	public PMatomoInstance(String id, int idUrl, String servDefId, String name, PlatformKind pfkind, String pfapi, String planid, PPlatform pf, String version) {
		super(id, OpCode.CREATE.toString(), OperationState.IN_PROGRESS.getValue(), pf);
		this.idUrl = idUrl;
		this.serviceDefinitionId = servDefId;
		this.name = name;
		this.createTime = ZonedDateTime.now();
		this.updateTime = this.createTime;
		this.platformKind = pfkind.toString();
		this.platformApiLocation = pfapi;
		this.planId = planid;
		this.installedVersion = version;
		this.password = "piwikpw"/*RandomStringUtils.randomAlphanumeric(LENGTH_PASSWORD)*/;
		this.tokenAuth = null;
		this.configFileContent = null;
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

	public ZonedDateTime getCreateTime() {
		return this.createTime;
	}

	public ZonedDateTime getUpdateTime() {
		return this.updateTime;
	}

	public PlatformKind getPlatformKind() {
		return PlatformKind.fromValue(platformKind);
	}

	public String getPlatformApiLocation() {
		return platformApiLocation;
	}

	public String getPlanId() {
		return planId;
	}

	public byte[] getConfigFileContent() {
		return this.configFileContent;
	}

	public void setConfigFileContent(byte cfc[]) {
		this.configFileContent = cfc;
	}

	public String getInstalledVersion() {
		return this.installedVersion;
	}

	public String getPassword() {
		return this.password;
	}

	public void setDbCred(String dbc) {
		this.dbCred = dbc;
	}

	public String getDbCred() {
		return this.dbCred;
	}

	public void setTokenAuth(String ta) {
		this.tokenAuth = ta;
	}

	public String getTokenAuth() {
		return this.tokenAuth;
	}
}
