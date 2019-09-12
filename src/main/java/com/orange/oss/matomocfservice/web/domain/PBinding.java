/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.web.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.cloud.servicebroker.model.instance.OperationState;

import com.orange.oss.matomocfservice.api.model.OpCode;

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
	private final static int LENGTH_USERNAME = 12;
	private final static int LENGTH_PASSWORD = 12;

	@ManyToOne
	private final PMatomoInstance pmatomoInstance;
	
	@Column(length = LENGTH_ID)
	private final String appId;

	@Column(length = LENGTH_SITENAME)
	private final String siteName;

	@Column(length = LENGTH_ADMINEMAIL)
    private String adminEmail;

	@Column(length = LENGTH_SITEURL)
    private String trackedUrl;

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
		this.siteId = -1;
		this.userName = null;
		this.password = null;
	}

	public PBinding(String id, PMatomoInstance pmi, String appid, String sitename, String trackedurl, String adminemail, PPlatform ppf) {
		super(id, OpCode.CREATE.toString(), OperationState.IN_PROGRESS.getValue(), ppf);
		this.pmatomoInstance = pmi;
		this.appId = appid;
		this.siteName = sitename;
		this.trackedUrl = trackedurl;
		this.adminEmail = adminemail;
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
