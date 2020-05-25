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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.cloudfoundry.operations.applications.ApplicationManifest.Builder;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.orange.oss.matomocfservice.servicebroker.ServiceCatalogConfiguration;

/**
 * @author P. Déchamboux
 *
 */
@Configuration
public class CloudFoundryMgrProperties {
	private final static Logger LOGGER = LoggerFactory.getLogger(CloudFoundryMgrProperties.class);
	private final static String SMTPINSTNAME = "mcfs-smtp";
	final static String GLOBSHAREDDBINSTNAME = "mcfs-globshared-db";
	private final static String SERVICESUFFIX = "-DB";
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
	private DbCreds sharedCreds = null;
	@Value("${matomo-service.shared-db.creds}")
	private String sharedDbCredsStr;
	private DbCreds sharedDbCreds = null;
	@Value("${matomo-service.matomo-shared-db.creds}")
	private String sharedDedicatedDbCredsStr;
	private DbCreds sharedDedicatedDbCreds = null;
	@Value("${matomo-service.dedicated-db.creds}")
	private String dedicatedDbCredsStr;
	private DbCreds dedicatedDbCreds = null;
	@Autowired
	EntityManagerFactory entityManagerFactory;
	private int maxDbConnections = 1;

	public int getMaxDbConnections() {
		return this.maxDbConnections;
	}

	public boolean getMatomoDebug() {
		return this.matomoDebug;
	}

	public String getDomain() {
		return this.serviceDomain;
	}

	public String getPhpBuildpack() {
		return this.servicePhpBuildpack;
	}

	public int getMaxServiceInstances() {
		return this.maxServiceInstances;
	}

	public SmtpCreds getSmtpCreds() {
		if (this.smtpCreds == null) {
			this.smtpCreds = new SmtpCreds(this.smtpCredsStr);
		}
		return this.smtpCreds;
	}

	public DbCreds getDbCreds(String planid) {
		if (planid.equals(ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID)) {
			if (this.sharedDbCreds == null) {
				this.sharedDbCreds = new DbCreds(this.sharedDbCredsStr, GLOBSHAREDDBINSTNAME);
			}
			return this.sharedDbCreds;
		} else if (planid.equals(ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID)) {
			if (this.sharedDedicatedDbCreds == null) {
				this.sharedDedicatedDbCreds = new DbCreds(this.sharedDedicatedDbCredsStr, null);
			}
			return this.sharedDedicatedDbCreds;
		} else if (planid.equals(ServiceCatalogConfiguration.PLANDEDICATEDDB_UUID)) {
			if (this.dedicatedDbCreds == null) {
				this.dedicatedDbCreds = new DbCreds(this.dedicatedDbCredsStr, null);
			}
			return this.dedicatedDbCreds;
		} else if (planid.equals(ServiceCatalogConfiguration.PLANSHARED_UUID)) {
			if (this.sharedCreds == null) {
				this.sharedCreds = new DbCreds(null, null);
			}
			return this.sharedCreds;
		}
		LOGGER.error("SERV::createMatomoInstance: unknown plan=" + planid);
		throw new IllegalArgumentException("Unkown plan");
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

		public SmtpCreds addVars(Builder b) {
			b.environmentVariable("MCFS_MAILSRV", this.service);
			b.environmentVariable("MCFS_MAILHOST", this.host);
			b.environmentVariable("MCFS_MAILPORT", this.port);
			b.environmentVariable("MCFS_MAILUSER", this.user);
			b.environmentVariable("MCFS_MAILPASSWD", this.password);
			return this;
		}

		public SmtpCreds addService(List<String> services) {
			services.add(SMTPINSTNAME);
			return this;
		}

		public String getInstanceServiceName() {
			return SMTPINSTNAME;
		}

		public String getServiceName() {
			return this.service;
		}

		public String getPlanName() {
			return this.plan;
		}
	}

	public class DbCreds {
		private String sharedServiceName;
		private String service;
		private String plan;
		private String name;
		private String host;
		private String port;
		private String user;
		private String password;

		DbCreds(String creds, String sharedServiceName) {
			this.sharedServiceName = sharedServiceName;
			if (creds != null) {
				String[] credsarr = creds.split(":");
				this.service = credsarr[0];
				this.plan = credsarr[1];
				this.name = credsarr[2];
				this.host = credsarr[3];
				this.port = credsarr[4];
				this.user = credsarr[5];
				this.password = credsarr[6];
			} else {
				this.service = null;
				this.plan = null;
				this.name = null;
				this.host = null;
				this.port = null;
				this.user = null;
				this.password = null;
			}
		}

		public DbCreds addVars(Builder b) {
			if (this.service != null) {
				b.environmentVariable("MCFS_DBSRV", this.service);
				b.environmentVariable("MCFS_DBNAME", this.name);
				b.environmentVariable("MCFS_DBHOST", this.host);
				b.environmentVariable("MCFS_DBPORT", this.port);
				b.environmentVariable("MCFS_DBUSER", this.user);
				b.environmentVariable("MCFS_DBPASSWD", this.password);
			}
			return this;
		}

		public DbCreds addService(List<String> services, String appName) {
			if (sharedServiceName != null) {
				services.add(sharedServiceName);
			} else {
				services.add(appName + SERVICESUFFIX);
			}
			return this;
		}

		public boolean isDedicatedDb() {
			return sharedServiceName == null;
		}

		public String getInstanceServiceName(String appName) {
			if (sharedServiceName == null) {
				return appName + SERVICESUFFIX;
			}
			return this.sharedServiceName;
		}

		public String getServiceName() {
			return this.service;
		}

		public String getPlanName() {
			return this.plan;
		}

		public String getJdbcUrl(Map<String, Object> vcapServices) {
			StringBuffer sb = new StringBuffer("jdbc:mysql://");
			if (this.service != null) {
				@SuppressWarnings("unchecked")
				Map<String, Object> creds = (Map<String, Object>)((Map<String, Object>)((List<Object>)vcapServices.get(this.service)).get(0)).get("credentials");
				sb.append(creds.get(this.host));
				sb.append(":3306/");
				sb.append(creds.get(this.name));
				sb.append("?user=");
				sb.append(creds.get(this.user));
				sb.append("&password=");
				sb.append(creds.get(this.password));
			}
			return sb.toString();
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("{service-domain: \"");
		sb.append(serviceDomain);
		sb.append("\", shared-db: {service-name: \"");
		sb.append(getDbCreds(ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID).service);
		sb.append("\", plan-name: \"");
		sb.append(getDbCreds(ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID).plan);
		sb.append("\"}, dedicated-db: {service-name: \"");
		sb.append(getDbCreds(ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID).service);
		sb.append("\", plan-name: \"");
		sb.append(getDbCreds(ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID).plan);
		sb.append("\"}}");
		return sb.toString();
	}

    @PostConstruct
    public void afterInitialize() {
    	LOGGER.debug("CONFIG::properties: " + this.toString());
    	EntityManager em = null;
    	try {
			em = entityManagerFactory.createEntityManager();
			em.getTransaction().begin();
			Work4Conn work4conn = new Work4Conn();
			Session session = em.unwrap(Session.class);
			session.doWork(work4conn);
			this.maxDbConnections = work4conn.getMaxDbConn();
			em.getTransaction().commit();
			LOGGER.debug("CONFIG:: maxDbConnections={}", this.maxDbConnections);
		} catch (Exception e) {
			LOGGER.error("CONFIG::cannot retrieve maxDbConnections from JDBC Driver");
		} finally {
			if (em != null) {
				em.close();
			}
		}
    }

    private static class Work4Conn implements Work {
    	int maxDbConn;

    	int getMaxDbConn() {
    		return this.maxDbConn;
    	}

		@Override
		public void execute(Connection connection) throws SQLException {
			this.maxDbConn = connection.getMetaData().getMaxConnections();
		}
    }
}
