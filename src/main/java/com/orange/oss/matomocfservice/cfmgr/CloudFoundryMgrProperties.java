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

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import javax.annotation.PostConstruct;

import org.cloudfoundry.operations.applications.ApplicationManifest.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author P. Déchamboux
 *
 */
@Configuration
public class CloudFoundryMgrProperties {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	@Value("${matomo-service.matomo-debug:false}")
	private boolean matomoDebug;
	@Value("${matomo-service.smtp.creds}")
	private String smtpCredsStr;
	private SmtpCreds smtpCreds = null;
	@Value("${matomo-service.domain}")
	private String serviceDomain;
	@Value("${matomo-service.phpBuildpack}")
	private String servicePhpBuildpack;
	@Value("${matomo-service.max-service-instances}")
	private int maxServiceInstances;
	@Value("${matomo-service.shared-db.creds}")
	private String sharedDbCredsStr;
	private DbCreds sharedDbCreds = null;
	@Value("${matomo-service.dedicated-db.creds}")
	private String dedicatedDbCredsStr;
	private DbCreds dedicatedDbCreds = null;

	public boolean getMatomoDebug() {
		return matomoDebug;
	}

	public String getDomain() {
		return serviceDomain;
	}

	public String getPhpBuildpack() {
		return servicePhpBuildpack;
	}

	public int getMaxServiceInstances() {
		return maxServiceInstances;
	}

	public String getSharedDbServiceName() {
		return getSharedDbCreds().service;
	}

	public String getSharedDbPlanName() {
		return getSharedDbCreds().plan;
	}

	public SmtpCreds getSmtpCreds() {
		if (smtpCreds == null) {
			smtpCreds = new SmtpCreds(smtpCredsStr);
		}
		return smtpCreds;
	}

	public DbCreds getSharedDbCreds() {
		if (sharedDbCreds == null) {
			sharedDbCreds = new DbCreds(sharedDbCredsStr);
		}
		return sharedDbCreds;
	}

	public DbCreds getDedicatedDbCreds() {
		if (dedicatedDbCreds == null) {
			dedicatedDbCreds = new DbCreds(dedicatedDbCredsStr);
		}
		return dedicatedDbCreds;
	}

	public String getDedicatedDbServiceName() {
		return getDedicatedDbCreds().service;
	}

	public String getDedicatedDbPlanName() {
		return getDedicatedDbCreds().plan;
	}

	public static class SmtpCreds {
		private String service;
		private String plan;
		private String host;
		private String port;
		private String user;
		private String password;

		SmtpCreds(String creds) {
			String[] credsarr = creds.split(":");
			this.service = credsarr[0];
			this.plan = credsarr[1];
			this.host = credsarr[2];
			this.port = credsarr[3];
			this.user = credsarr[4];
			this.password = credsarr[5];
		}

		public Builder addVars(Builder b) {
			b.environmentVariable("MCFS_MAILSRV", this.service);
			b.environmentVariable("MCFS_MAILHOST", this.host);
			b.environmentVariable("MCFS_MAILPORT", this.port);
			b.environmentVariable("MCFS_MAILUSER", this.user);
			b.environmentVariable("MCFS_MAILPASSWD", this.password);
			return b;
		}

		public String getServiceName() {
			return this.service;
		}

		public String getPlanName() {
			return this.plan;
		}
	}

	public class DbCreds {
		private String service;
		private String plan;
		private String name;
		private String host;
		private String port;
		private String user;
		private String password;

		DbCreds(String creds) {
			String[] credsarr = creds.split(":");
			this.service = credsarr[0];
			this.plan = credsarr[1];
			this.name = credsarr[2];
			this.host = credsarr[3];
			this.port = credsarr[4];
			this.user = credsarr[5];
			this.password = credsarr[6];
		}

		public Builder addVars(Builder b) {
			b.environmentVariable("MCFS_DBSRV", this.service);
			b.environmentVariable("MCFS_DBNAME", this.name);
			b.environmentVariable("MCFS_DBHOST", this.host);
			b.environmentVariable("MCFS_DBPORT", this.port);
			b.environmentVariable("MCFS_DBUSER", this.user);
			b.environmentVariable("MCFS_DBPASSWD", this.password);
			return b;
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("{service-domain: \"");
		sb.append(serviceDomain);
		sb.append("\", shared-db: {service-name: \"");
		sb.append(getSharedDbServiceName());
		sb.append("\", plan-name: \"");
		sb.append(getSharedDbPlanName());
		sb.append("\"}, dedicated-db: {service-name: \"");
		sb.append(getDedicatedDbServiceName());
		sb.append("\", plan-name: \"");
		sb.append(getDedicatedDbPlanName());
		sb.append("\"}}");
		return sb.toString();
	}

    @PostConstruct
    public void afterInitialize() {
    	LOGGER.debug("CONFIG::properties: " + this.toString());
    }
}
