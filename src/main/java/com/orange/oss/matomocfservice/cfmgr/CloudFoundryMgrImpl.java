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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.GetApplicationEnvironmentsRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.cloudfoundry.operations.applications.Route;
import org.cloudfoundry.operations.applications.ScaleApplicationRequest;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.DeleteServiceInstanceRequest;
import org.cloudfoundry.operations.services.GetServiceInstanceRequest;
import org.cloudfoundry.operations.services.UnbindServiceInstanceRequest;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.orange.oss.matomocfservice.servicebroker.ServiceCatalogConfiguration;
import com.orange.oss.matomocfservice.web.domain.PMatomoInstance;
import com.orange.oss.matomocfservice.web.domain.Parameters;
import com.orange.oss.matomocfservice.web.service.MatomoReleases;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.FileSystemFile;
import reactor.core.publisher.Mono;

/**
 * @author P. Déchamboux
 *
 */
public class CloudFoundryMgrImpl extends CloudFoundryMgrAbs {
	private final static Logger LOGGER = LoggerFactory.getLogger(CloudFoundryMgr.class);
	private final static String MATOMOINSTANCE_ROOTUSER = "admin";
	private String sshHost;
	private int sshPort;
	private boolean smtpReady = false;
	private boolean globalSharedReady = false;
	@Autowired
	private CloudFoundryOperations cfops;
	@Autowired
	private ReactorCloudFoundryClient cfclient;

	/**
	 * Initialize CF manager and especially create the shared database for the dev flavor of
	 * Matomo service instances.
	 */
	public void initialize() {
		LOGGER.debug("CFMGR::CloudFoundryMgr-initialize");
		cfclient.info().get(GetInfoRequest.builder().build())
		.doOnError(t -> {})
		.doOnSuccess(infos -> {
			String host = infos.getApplicationSshEndpoint();
			int indc = host.indexOf(":");
			sshHost = host.substring(0, indc);
			sshPort = Integer.parseInt(host.substring(indc + 1));
			LOGGER.debug("CONFIG::CloudFoundryMgr-initialize: sshHost={}, sshPort={}", sshHost, sshPort);
		})
		.block();
		// Check if SMTP service has already been created and create otherwise
		cfops.services().getInstance(GetServiceInstanceRequest.builder()
				.name(properties.getSmtpCreds().getInstanceServiceName())
				.build())
		.doOnError(t -> {
			LOGGER.debug("CONFIG::CloudFoundryMgr-initialize: create SMTP service instance");
			cfops.services().createInstance(CreateServiceInstanceRequest.builder()
					.serviceInstanceName(properties.getSmtpCreds().getInstanceServiceName())
					.serviceName(properties.getSmtpCreds().getServiceName())
					.planName(properties.getSmtpCreds().getPlanName())
					.build())
			.doOnError(tt -> {
				LOGGER.error("CONFIG::CloudFoundryMgr-initialize: cannot create SMTP service -> whole service unavailable!!");
			})
			.doOnSuccess(v -> {
				LOGGER.debug("CONFIG::CloudFoundryMgr-initialize: SMTP service instance created");
				smtpReady = true;
			})
			.subscribe();
		})
		.doOnSuccess(v -> {
			LOGGER.debug("CONFIG::CloudFoundryMgr-initialize: SMTP service instance already exist");
			smtpReady = true;
		})
		.block();
		// Check if global shared DB service has already been created and create otherwise
		cfops.services().getInstance(GetServiceInstanceRequest.builder()
				.name(properties.getDbCreds(ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID).getInstanceServiceName(null))
				.build())
		.doOnError(t -> {
			LOGGER.debug("CONFIG::CloudFoundryMgr-initialize: create global shared db service instance");
			cfops.services().createInstance(CreateServiceInstanceRequest.builder()
					.serviceInstanceName(properties.getDbCreds(ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID).getInstanceServiceName(null))
					.serviceName(properties.getDbCreds(ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID).getServiceName())
					.planName(properties.getDbCreds(ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID).getPlanName())
					.completionTimeout(Duration.ofMinutes(CREATEDBSERV_TIMEOUT))
					.build())
			.timeout(Duration.ofMinutes(CREATEDBSERV_TIMEOUT))
			.doOnError(tt -> {
				LOGGER.error("CONFIG::CloudFoundryMgr-initialize: cannot create global shared DB service -> global-shared-db plan unavailable!!");
			})
			.doOnSuccess(vv -> {
				LOGGER.debug("CONFIG::CloudFoundryMgr-initialize: global shared DB service instance created");
				globalSharedReady = true;
			})
			.subscribe();
		})
		.doOnSuccess(v -> {
			LOGGER.debug("CONFIG::CloudFoundryMgr-initialize: global shared DB service instance already exist");
			globalSharedReady = true;
		})
		.subscribe();
		LOGGER.debug("CONFIG::CloudFoundryMgr-initialize: finished");
	}

	public boolean isSmtpReady() {
		return smtpReady;
	}

	public boolean isGlobalSharedReady() {
		return globalSharedReady;
	}

	/**
	 * Launch the deployment of a CF app for a dev flavor instance of Matomo service.
	 * @param instid	The code name of the instance
	 * @return	The Mono to signal the end of the async process (produce nothng indeed)
	 */
	public Mono<Void> deployMatomoCfApp(String instid, String uuid, String planid, Parameters mip, int memsize, int nbinst) {
		LOGGER.debug("CFMGR::deployMatomoCfApp: instId={}", instid);
		String instpath = MatomoReleases.getVersionPath(mip.getVersion(), instid);
		LOGGER.debug("File for Matomo bits: " + instpath);
		ApplicationManifest.Builder manifestbuilder;
		manifestbuilder = ApplicationManifest.builder()
				.name(getAppName(instid))
				.path(Paths.get(instpath))
				.route(Route.builder().route(uuid + "." + properties.getDomain()).build())
				.buildpack(properties.getPhpBuildpack())
				.memory(memsize)
				.timeout(180)
				.instances(nbinst)
				.environmentVariable("TZ", mip.getTimeZone());
		List<String> services = new ArrayList<String>();
		properties.getSmtpCreds().addVars(manifestbuilder).addService(services);
		properties.getDbCreds(planid).addVars(manifestbuilder).addService(services, getAppName(instid));
		manifestbuilder.services(services);
		return cfops.applications().pushManifest(PushApplicationManifestRequest.builder()
				.manifest(manifestbuilder.build())
				.build());
	}

	public Mono<Void> scaleMatomoCfApp(String instid, int instances, int memsize) {
		LOGGER.debug("CFMGR::scaleMatomoCfApp: instId={}, instances={}, memsize={}", instid, instances, memsize);
		return cfops.applications().scale(ScaleApplicationRequest.builder()
				.name(getAppName(instid))
				.instances(instances)
				.memoryLimit(memsize)
				.build());
	}

	/**
	 * Launch the creation of a dedicated DB for a particular Matomo instance.
	 * @param instid	The code name of the instance
	 * @return	The Mono to signal the end of the async process (produce nothing indeed)
	 */
	public Mono<Void> createDedicatedDb(String instid, String planid) {
		LOGGER.debug("CFMGR::createDedicatedDb: instId={}", instid);
		return cfops.services().createInstance(CreateServiceInstanceRequest.builder()
					.serviceInstanceName(properties.getDbCreds(planid).getInstanceServiceName(getAppName(instid)))
					.serviceName(properties.getDbCreds(planid).getServiceName())
					.planName(properties.getDbCreds(planid).getPlanName())
					.completionTimeout(Duration.ofMinutes(CREATEDBSERV_TIMEOUT))
					.build());
	}

	/**
	 * Launch the deletion of the dedicated DB for a particular Matomo instance.
	 * @param instid	The code name of the instance
	 * @return	The Mono to signal the end of the async process (produce nothing indeed)
	 */
	public Mono<Void> deleteDedicatedDb(String instid, String planid) {
		LOGGER.debug("CFMGR::deleteDedicatedDb: instId={}", instid);
		return cfops.services().deleteInstance(DeleteServiceInstanceRequest.builder()
					.name(properties.getDbCreds(planid).getInstanceServiceName(getAppName(instid)))
					.build());
	}

	public Mono<Map<String, Object>> getApplicationEnv(String instid) {
		LOGGER.debug("CFMGR::getApplicationEnv: instId={}", instid);
		return Mono.create(sink -> {
			cfops.applications().getEnvironments(GetApplicationEnvironmentsRequest.builder()
					.name(getAppName(instid))
					.build())
			.doOnError(t -> {sink.error(t);})
			.doOnSuccess(envs -> {
				LOGGER.debug("ENVS: " + envs.toString());
				sink.success(envs.getSystemProvided());
			}).subscribe();
		});
	}

	/**
	 * Launch the deletion of the CF app for an instance of Matomo service.
	 * @param instid	The name of the Matomo instance as it is exposed to the Web
	 * @param planid	The service plan of the Matomo instance
	 * @return	The Mono to signal the end of the async process (produce nothing indeed)
	 */
	public Mono<ApplicationDetail> deleteMatomoCfApp(String instid, String planid) {
		final String NOID = "none";
		LOGGER.debug("CFMGR::deleteMatomoCfApp: instId={}", instid);
		return cfops.applications().get(GetApplicationRequest.builder().name(getAppName(instid)).build())
		.onErrorResume(t -> {
			LOGGER.debug("CFMGR::deleteMatomoCfApp: app does not exist, nothing to delete!!");
			return Mono.just(ApplicationDetail.builder()
					.name(getAppName(instid))
					.id(NOID)
					.stack("cflinuxfs3")
					.diskQuota(1024)
					.instances(1)
					.memoryLimit(1024)
					.requestedState("none")
					.runningInstances(1)
					.build());
		}).doOnSuccess(ad -> {
			if (ad.getId().equals(NOID)) { // resume from not exist: OK but do nothing
				return;
			}
			LOGGER.debug("CFMGR::deleteMatomoCfApp: app exist");
			cfops.services().unbind(UnbindServiceInstanceRequest.builder()
					.serviceInstanceName(properties.getDbCreds(planid).getInstanceServiceName(getAppName(instid)))
					.applicationName(getAppName(instid)).build())
			.doOnError(t -> {
				LOGGER.error("CFMGR::deleteMatomoCfApp: problem to unbind from DB.", t);
				cfops.applications().delete(
						DeleteApplicationRequest.builder().deleteRoutes(true).name(getAppName(instid)).build())
				.doOnError(tt -> {
					LOGGER.error("CFMGR::deleteMatomoCfApp: problem to delete app (no unbind).", tt);
				}).doOnSuccess(vv -> {
					LOGGER.debug("CFMGR::deleteMatomoCfApp: app unbound and deleted.");
				}).subscribe();
			}).doOnSuccess(v -> {
				cfops.applications().delete(
						DeleteApplicationRequest.builder().deleteRoutes(true).name(getAppName(instid)).build())
				.doOnError(tt -> {
					LOGGER.error("CFMGR::deleteMatomoCfApp: problem to delete app (after unbind).", tt);
				}).doOnSuccess(vv -> {
					LOGGER.debug("CFMGR::deleteMatomoCfApp: app unbound and deleted.");
				}).subscribe();
			}).subscribe();
		});
	}

	public Mono<AppConfHolder> getInstanceConfigFile(String instid, String version, boolean clustermode) {
		LOGGER.debug("CFMGR::getInstanceConfigFile: instid={}, version={}, clusterMode={}", instid, version, clustermode);
		AppConfHolder appidh = new AppConfHolder();
		return Mono.create(sink -> {
			cfops.applications().get(GetApplicationRequest.builder().name(getAppName(instid)).build())
			.doOnError(t -> {sink.error(t);})
			.doOnSuccess(appinfo -> {
				appidh.appId = appinfo.getId();
				cfops.advanced().sshCode()
				.doOnError(t -> {sink.error(t);})
				.doOnSuccess(pwd -> {
					LOGGER.debug("CFMGR::getInstanceConfigFile: user={}, pwd={}", "cf:" + appidh.appId + "/0", pwd);
					@SuppressWarnings("resource")
					SSHClient ssh = new SSHClient();
					ssh.addHostKeyVerifier(new PromiscuousVerifier());
					try {
						ssh.loadKnownHosts();
						ssh.connect(sshHost, sshPort);
						ssh.authPassword("cf:" + appidh.appId + "/0", pwd);
						String target = MatomoReleases.getVersionPath(version, instid) + File.separator + "config" + File.separator;
						ssh.newSCPFileTransfer().download("/home/vcap/app/htdocs/config/config.ini.php", new FileSystemFile(target));
						Path pcf = Paths.get(target + "config.ini.php");
						if (properties.getMatomoDebug()) {
							Files.write(pcf, "\n[Tracker]\ndebug = 1\nenable_sql_profiler = 1\n".getBytes(), StandardOpenOption.APPEND);
						}
						if (clustermode) {
							List<String> allLines = Files.readAllLines(pcf);
							Files.write(pcf, "".getBytes());
							for (String line : allLines) {
								Files.write(pcf, (line + "\n").getBytes(), StandardOpenOption.APPEND);
								if (line.startsWith("[General]")) {
									LOGGER.debug("CFMGR::getInstanceConfigFile: add configuration for cluster mode support");
									Files.write(pcf, "session_save_handler = dbtable\n".getBytes(), StandardOpenOption.APPEND);
									Files.write(pcf, "multi_server_environment = 1\n".getBytes(), StandardOpenOption.APPEND);									
								}
							}
						}
						appidh.fileContent = Files.readAllBytes(Paths.get(target + "config.ini.php"));
						sink.success(appidh);
					} catch (Exception e) {
						sink.error(e);
					} finally {
						try {
							ssh.disconnect();
						} catch (IOException e) {
							sink.error(e);
						}
					}
				})
				.subscribe();
			})
			.subscribe();
		});
	}

	/**
	 * 
	 * @param appcode
	 * @param nuri
	 * @param pwd
	 * @return		true if the instance has been intialized correctly
	 */
	public boolean initializeMatomoInstance(String appcode, String nuri, String pwd, String planid) {
		LOGGER.debug("CFMGR::initializeMatomoInstance: appCode={}", appcode);
		RestTemplate restTemplate = new RestTemplate();
		try {
			URI uri = new URI("https://" + nuri + "." + properties.getDomain()), calluri;
			LOGGER.debug("Base URI: {}", uri.toString());
			String res = restTemplate.getForObject(calluri = uri, String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			res = restTemplate.getForObject(calluri = URI.create(uri.toString() + "/index.php?action=systemCheck"),
					String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			res = restTemplate.getForObject(calluri = URI.create(uri.toString() + "/index.php?action=databaseSetup"),
					String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			Document d = Jsoup.parse(res);
			if (d == null) {
				LOGGER.error("CFMGR::initializeMatomoInstance: error while decoding JSON from GET databaseSetup.");
				return false;
			}
			MultipartBodyBuilder mbb = new MultipartBodyBuilder();
			mbb.part("type", d.getElementById("type-0").attr("value"));
			mbb.part("host", d.getElementById("host-0").attr("value"));
			mbb.part("username", d.getElementById("username-0").attr("value"));
			mbb.part("password", d.getElementById("password-0").attr("value"));
			mbb.part("dbname", d.getElementById("dbname-0").attr("value"));
			mbb.part("tables_prefix", getTablePrefix(appcode, planid) + "_");
			mbb.part("adapter", "PDO\\MYSQL");
			mbb.part("submit", "Suivant+%C2%BB");
			res = restTemplate.postForObject(calluri = URI.create(uri.toString() + "/index.php?action=databaseSetup"),
					mbb.build(), String.class);
			LOGGER.debug("After POST on <{}>", calluri.toString());
			res = restTemplate.getForObject(
					calluri = URI.create(uri.toString() + "/index.php?action=tablesCreation&module=Installation"),
					String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			res = restTemplate.getForObject(
					calluri = URI.create(uri.toString() + "/index.php?action=setupSuperUser&module=Installation"),
					String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			mbb = new MultipartBodyBuilder();
			mbb.part("login", MATOMOINSTANCE_ROOTUSER);
			mbb.part("password", pwd);
			mbb.part("password_bis", pwd);
			mbb.part("email", "piwik@orange.com");
			mbb.part("subscribe_newsletter_piwikorg", "0");
			mbb.part("subscribe_newsletter_professionalservices", "0");
			mbb.part("submit", "Suivant+%C2%BB");
			res = restTemplate.postForObject(
					calluri = URI.create(uri.toString() + "/index.php?action=setupSuperUser&module=Installation"),
					mbb.build(), String.class);
			LOGGER.debug("After POST on <{}>", calluri.toString());
			mbb = new MultipartBodyBuilder();
			mbb.part("siteName", appcode);
			mbb.part("url", uri.toASCIIString());
			mbb.part("timezone", "Europe/Paris");
			mbb.part("ecommerce", "0");
			mbb.part("submit", "Suivant+%C2%BB");
			res = restTemplate.postForObject(
					calluri = URI.create(uri.toString() + "/index.php?action=firstWebsiteSetup&module=Installation"),
					mbb.build(), String.class);
			LOGGER.debug("After POST on <{}>", calluri.toString());
			res = restTemplate.getForObject(calluri = URI.create(
					uri.toString() + "/index.php?action=finished" + "&clientProtocol=https" + "&module=Installation"
							+ "&site_idSite=4" + "&site_name=" + getTablePrefix(appcode, planid)),
					String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			mbb = new MultipartBodyBuilder();
			mbb.part("do_not_track", "1");
			mbb.part("anonymise_ip", "1");
			mbb.part("submit", "Continuer+vers+Matomo+%C2%BB");
			res = restTemplate.postForObject(calluri = URI.create(uri.toString()
					+ "/index.php?action=finished&clientProtocol=https&module=Installation&site_idSite=4&site_name="
					+ getTablePrefix(appcode, planid)), mbb.build(), String.class);
			LOGGER.debug("After POST on <{}>", calluri.toString());
			res = restTemplate.getForObject(calluri = uri, String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
		} catch (RestClientException e) {
			e.printStackTrace();
			LOGGER.error("CFMGR::initializeMatomoInstance: error while calling service instance through HTTP.", e);
			return false;
		} catch (URISyntaxException e) {
			LOGGER.error("CFMGR::initializeMatomoInstance: wrong URI for calling service instance through HTTP.", e);
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param appcode
	 * @param nuri
	 * @return		true if the instance has been upgraded correctly
	 */
	public boolean upgradeMatomoInstance(String appcode, String nuri) {
		LOGGER.debug("CFMGR::upgradeMatomoInstance: appCode={}", appcode);
		RestTemplate restTemplate = new RestTemplate();
		try {
			URI uri = new URI("https://" + nuri + "." + properties.getDomain()), calluri;
			LOGGER.debug("Base URI: {}", uri.toString());
			restTemplate.getForObject(calluri = uri, String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
			restTemplate.getForObject(calluri = URI.create(uri.toString() + "/index.php?updateCorePlugins=1"),
					String.class);
			LOGGER.debug("After GET on <{}>", calluri.toString());
		} catch (RestClientException e) {
			e.printStackTrace();
			LOGGER.error("CFMGR::upgradeMatomoInstance: error while calling service instance through HTTP.", e);
			return false;
		} catch (URISyntaxException e) {
			LOGGER.error("CFMGR::upgradeMatomoInstance: wrong URI for calling service instance through HTTP.", e);
			return false;
		}
		return true;
	}

	public String getApiAccessToken(String dbcred, String instid, String planid) {
		LOGGER.debug("CFMGR::getApiAccessToken: instId={}", instid);
		String token = null;
		Connection conn = null;
		Statement stmt = null;
		try {
			Class.forName("org.mariadb.jdbc.Driver");
			conn = DriverManager.getConnection(dbcred);
			stmt = conn.createStatement();
			stmt.execute("SELECT token_auth FROM " + getTablePrefix(instid, planid) + "_user WHERE login='" + MATOMOINSTANCE_ROOTUSER + "'");
			if (!stmt.getResultSet().first()) {
				LOGGER.error("Cannot retrieve the credentials of the admin user (resultset issue).");
			}
			token = stmt.getResultSet().getString(1);
		} catch (ClassNotFoundException e) {
			LOGGER.error("Cannot retrieve the credentials of the admin user (driver not found).", e);
		} catch (SQLException e) {
			LOGGER.error("Cannot retrieve the credentials of the admin user (sql problem).", e);
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException e) {
				LOGGER.error("Problem while retrieving the credentials of the admin user (at connexion close): " + e.getMessage());
			}
		}
		return token;
	}

	public void deleteAssociatedDbSchema(PMatomoInstance pmi) {
		LOGGER.debug("CFMGR::deleteAssociatedDbSchema: appCode={}", pmi.getIdUrlStr());
		if (pmi.getDbCred() == null) {
			return;
		}
        Connection conn = null;
        Statement stmt = null;
		try {
			Class.forName("org.mariadb.jdbc.Driver");
			//LOGGER.debug("SERV::deleteAssociatedDbSchema: jdbcUrl={}", pmi.getDbCred());
			conn = DriverManager.getConnection(pmi.getDbCred());
			DatabaseMetaData m = conn.getMetaData();
			ResultSet tables = m.getTables(null, null, "%", null);
			while (tables.next()) {
				if (! tables.getString(3).startsWith(getTablePrefix(pmi.getIdUrlStr(), pmi.getPlanId()))) {
					continue;
				}
				stmt = conn.createStatement();
				stmt.execute("DROP TABLE " + tables.getString(3));
				stmt.close();
			}
		} catch (ClassNotFoundException e) {
			LOGGER.error("CFMGR::deleteAssociatedDbSchema: cannot find JDBC driver.");
			e.printStackTrace();
			throw new RuntimeException("Cannot remove tables of deleted Matomo service instance (DRIVER).", e);
		} catch (SQLException e) {
			LOGGER.error("CFMGR::deleteAssociatedDbSchema: SQL problem -> {}", e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Cannot remove tables of deleted Matomo service instance (SQL EXEC).", e);
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException e) {
				LOGGER.error("CFMGR::deleteAssociatedDbSchema: SQL problem while closing connexion -> {}", e.getMessage());
				e.printStackTrace();
				throw new RuntimeException("Cannot remove tables of deleted Matomo service instance (CNX CLOSE).", e);
			}
		}
	}
}
