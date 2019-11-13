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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.ws.Holder;

import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.GetApplicationEnvironmentsRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.cloudfoundry.operations.applications.Route;
import org.cloudfoundry.operations.applications.ScaleApplicationRequest;
import org.cloudfoundry.operations.services.GetServiceInstanceRequest;
import org.cloudfoundry.operations.services.UnbindServiceInstanceRequest;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.DeleteServiceInstanceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.orange.oss.matomocfservice.api.model.MiParameters;
import com.orange.oss.matomocfservice.servicebroker.ServiceCatalogConfiguration;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.FileSystemFile;
import reactor.core.publisher.Mono;

/**
 * @author P. Déchamboux
 *
 */
@Service
public class CloudFoundryMgr {
	private final static Logger LOGGER = LoggerFactory.getLogger(CloudFoundryMgr.class);
	private final static String MATOMO_ANPREFIX = "MATOMO_";
	private final static String MATOMO_AUPREFIX = "M";
	public final static long CREATEDBSERV_TIMEOUT = 90; // in minutes
	private String sshHost;
	private int sshPort;
	private boolean smtpReady = false;
	private boolean globalSharedReady = false;
	private boolean matomoSharedReady = false;
	@Autowired
	private CloudFoundryOperations cfops;
	@Autowired
	private ReactorCloudFoundryClient cfclient;
	@Autowired
	private CloudFoundryMgrProperties properties;
	@Autowired
	private MatomoReleases matomoReleases;

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
		.subscribe();
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
		// Check if Matomo shared DB service has already been created and create otherwise
		cfops.services().getInstance(GetServiceInstanceRequest.builder()
				.name(properties.getDbCreds(ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID).getInstanceServiceName(null))
				.build())
		.doOnError(t -> {
			LOGGER.debug("CONFIG::CloudFoundryMgr-initialize: create Matomo shared db service instance");
			cfops.services().createInstance(CreateServiceInstanceRequest.builder()
					.serviceInstanceName(properties.getDbCreds(ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID).getInstanceServiceName(null))
					.serviceName(properties.getDbCreds(ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID).getServiceName())
					.planName(properties.getDbCreds(ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID).getPlanName())
					.completionTimeout(Duration.ofMinutes(CREATEDBSERV_TIMEOUT))
					.build())
			.timeout(Duration.ofMinutes(CREATEDBSERV_TIMEOUT))
			.doOnError(tt -> {
				LOGGER.error("CONFIG::CloudFoundryMgr-initialize: cannot create Matomo shared db service instance ("
						+ tt.getMessage()
						+ ") -> plan matomo-shared-db unavailable");
				tt.printStackTrace();
			})
			.doOnSuccess(v -> {
				LOGGER.info("CONFIG::CloudFoundryMgr-initialize: Matomo shared db service instance created -> plan matomo-shared-db ready");
				matomoSharedReady = true;
			})
			.subscribe();
		})
		.doOnSuccess(v -> {
			LOGGER.debug("CONFIG::CloudFoundryMgr-initialize: Matomo shared DB service instance already exist");
			matomoSharedReady = true;
		})
		.subscribe();
		LOGGER.debug("CONFIG::CloudFoundryMgr-initialize: finished");
	}

	public boolean isSmtpReady() {
		return smtpReady;
	}

	public boolean isMatomoSharedReady() {
		return matomoSharedReady;
	}

	public boolean isGlobalSharedReady() {
		return globalSharedReady;
	}

	public String getAppName(String appcode) {
		return MATOMO_ANPREFIX + appcode;
	}

	public String getAppUrlPrefix(String appcode) {
		return MATOMO_AUPREFIX + appcode;
	}

	/**
	 * Launch the deployment of a CF app for a dev flavor instance of Matomo service.
	 * @param instid	The code name of the instance
	 * @return	The Mono to signal the end of the async process (produce nothng indeed)
	 */
	public Mono<Void> deployMatomoCfApp(String instid, String expohost, String planid, MiParameters mip, int memsize, int nbinst) {
		LOGGER.debug("CFMGR::deployMatomoCfApp: instId={}", instid);
		String instpath = matomoReleases.getVersionPath(mip.getVersion(), instid);
		LOGGER.debug("File for Matomo bits: " + instpath);
		ApplicationManifest.Builder manifestbuilder;
		manifestbuilder = ApplicationManifest.builder()
				.name(getAppName(instid))
				.path(Paths.get(instpath))
				.route(Route.builder().route(getHost(instid, expohost) + "." + properties.getDomain()).build())
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
	public Mono<Void> createDedicatedDb(String instid) {
		LOGGER.debug("CFMGR::createDedicatedDb: instId={}", instid);
		return cfops.services().createInstance(CreateServiceInstanceRequest.builder()
				.serviceInstanceName(properties.getDbCreds(ServiceCatalogConfiguration.PLANDEDICATEDDB_UUID).getInstanceServiceName(getAppName(instid)))
				.serviceName(properties.getDbCreds(ServiceCatalogConfiguration.PLANDEDICATEDDB_UUID).getServiceName())
				.planName(properties.getDbCreds(ServiceCatalogConfiguration.PLANDEDICATEDDB_UUID).getPlanName())
				.completionTimeout(Duration.ofMinutes(CREATEDBSERV_TIMEOUT))
				.build());
	}

	/**
	 * Launch the deletion of the dedicated DB for a particular Matomo instance.
	 * @param instid	The code name of the instance
	 * @return	The Mono to signal the end of the async process (produce nothing indeed)
	 */
	public Mono<Void> deleteDedicatedDb(String instid) {
		LOGGER.debug("CFMGR::deleteDedicatedDb: instId={}", instid);
		return cfops.services().deleteInstance(DeleteServiceInstanceRequest.builder()
				.name(properties.getDbCreds(ServiceCatalogConfiguration.PLANDEDICATEDDB_UUID).getInstanceServiceName(getAppName(instid)))
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

	private String getHost(String instid, String expohost) {
		return instid.equals(expohost) ? getAppUrlPrefix(expohost) : expohost;
	}

	public String getInstanceUrl(String instid, String expohost) {
		return "https://" + getHost(instid, expohost) + "." + properties.getDomain();
	}

	/**
	 * Launch the deletion of the CF app for a dev flavor instance of Matomo service.
	 * @param instid	The name of the Matomo instance as it is exposed to the Web
	 * @param planid	The service plan of the Matomo instance
	 * @return	The Mono to signal the end of the async process (produce nothng indeed)
	 */
	public Mono<Void> deleteMatomoCfApp(String instid, String planid) {
		LOGGER.debug("CFMGR::deleteMatomoCfApp: instId={}", instid);
		return cfops.services().unbind(UnbindServiceInstanceRequest.builder()
				.serviceInstanceName(properties.getDbCreds(planid).getInstanceServiceName(getAppName(instid)))
				.applicationName(getAppName(instid))
				.build())
				.doOnError(t -> {
					LOGGER.error("CFMGR::deleteMatomoCfAppBindToGlobalSharedDb: problem to unbind from DB.", t);
					cfops.applications().delete(DeleteApplicationRequest.builder()
							.deleteRoutes(true)
							.name(getAppName(instid))
							.build())
					.doOnError(tt -> {
						LOGGER.error("CFMGR::deleteMatomoCfAppBindToGlobalSharedDb: problem to delete app (no unbind).", tt);
					})
					.doOnSuccess(vv -> {
						LOGGER.debug("CFMGR::deleteMatomoCfAppBindToGlobalSharedDb: app unbound and deleted.");
					})
					.subscribe();
				})
				.doOnSuccess(v -> {
					cfops.applications().delete(DeleteApplicationRequest.builder()
							.deleteRoutes(true)
							.name(getAppName(instid))
							.build())
					.doOnError(tt -> {
						LOGGER.error("CFMGR::deleteMatomoCfAppBindToGlobalSharedDb: problem to delete app (unbind).", tt);
					})
					.doOnSuccess(vv -> {
						LOGGER.debug("CFMGR::deleteMatomoCfAppBindToGlobalSharedDb: app unbound and deleted.");
					})
					.subscribe();
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
						String target = matomoReleases.getVersionPath(version, instid) + File.separator + "config" + File.separator;
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

	public List<String> getAppRoutes(String appid) {
		LOGGER.debug("CFMGR::getAppRoutes: appid={}", appid);
		Holder<List<String>> hres = new Holder<List<String>>();
		cfops.applications().get(GetApplicationRequest.builder().name(appid).build())
		.doOnError(t -> {})
		.doOnSuccess(appinfo -> {
			hres.value = appinfo.getUrls();
		}).block();
		return hres.value;
	}

	public class AppConfHolder {
		public String appId = null;
		public byte[] fileContent = null;
	}
}
