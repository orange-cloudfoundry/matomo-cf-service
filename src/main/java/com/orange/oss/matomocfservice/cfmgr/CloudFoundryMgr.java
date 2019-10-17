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
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
import org.cloudfoundry.operations.services.GetServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.UnbindServiceInstanceRequest;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.orange.oss.matomocfservice.config.ServiceCatalogConfiguration;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.FileSystemFile;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

/**
 * @author P. Déchamboux
 *
 */
@Service
public class CloudFoundryMgr {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	private final static String GLOBSHARDBINSTNAME = "matomo-globshared-db";
	private final static String MATOMO_ANPREFIX = "MATOMO_";
	private final static String MATOMO_AUPREFIX = "M";
	private String sshHost;
	private int sshPort;
	@Autowired
	private CloudFoundryOperations cfops;
	@Autowired
	private ReactorCloudFoundryClient cfclient;
	@Autowired
	private CloudFoundryMgrProperties properties;
	@Autowired
	MatomoReleases matomoReleases;

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
		ExistDbSubscriber ssub = new ExistDbSubscriber();
		cfops.services().getInstance(GetServiceInstanceRequest.builder()
				.name(GLOBSHARDBINSTNAME)
				.build())
		.subscribe(ssub);
		waitOnSub(ssub);
		if (ssub.isExisted()) {
			LOGGER.debug("CONFIG::CloudFoundryMgr-initialize: finished");
			return;
		}
		LOGGER.debug("CONFIG::CloudFoundryMgr-initialize: create shared db");
		CreateDbSubscriber csub = new CreateDbSubscriber();
		cfops.services().createInstance(CreateServiceInstanceRequest.builder()
				.serviceInstanceName(GLOBSHARDBINSTNAME)
				.serviceName(properties.getSharedDbServiceName())
				.planName(properties.getSharedDbPlanName())
				.build())
		.subscribe(csub);
		waitOnSub(csub);
		if (csub.getCause() != null) {
			throw new RuntimeException("Cannot create database with shared database service.", csub.getCause());
		}
		LOGGER.debug("CONFIG::CloudFoundryMgr-initialize: finished");
	}

	public String getAppName(String appcode) {
		return MATOMO_ANPREFIX + appcode;
	}

	public String getAppUrlPrefix(String appcode) {
		return MATOMO_AUPREFIX + appcode;
	}

	/**
	 * Launch the deployment of a CF app for a dev flavor instance of Matomo service.
	 * @param instid	The name of the instance as it is exposed to the Web
	 * @return	The Mono to signal the end of the async process (produce nothng indeed)
	 */
	public Mono<Void> deployMatomoCfAppBindToGlobalSharedDb(String instid, String version, String expohost, String planid, String tz) {
		LOGGER.debug("CFMGR::createMatomoCfAppBindToGlobalSharedDb: instId={}", instid);
		String verspath = matomoReleases.getVersionPath(version, instid);
		LOGGER.debug("File for Matomo bits: " + verspath);
		List<String> services = new ArrayList<String>();
		if (planid.equals(ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID)) {
			services.add(GLOBSHARDBINSTNAME);
		} else if (planid.equals(ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID)) {
			LOGGER.error("SERV::createMatomoInstance: plan MATOMO_SHARED_DB -> not currently supported");
			throw new UnsupportedOperationException("Plan currently not supprted");
		} else if (planid.equals(ServiceCatalogConfiguration.PLANDEDICATEDDB_UUID)) {
			LOGGER.error("SERV::createMatomoInstance: plan MATOMO_DEDICATED_DB -> not currently supported");
			throw new UnsupportedOperationException("Plan currently not supprted");
		} else {
			LOGGER.error("SERV::createMatomoInstance: unknown plan=" + planid);
			throw new IllegalArgumentException("Unkown plan when creating service instance");
		}
		return cfops.applications()
				.pushManifest(PushApplicationManifestRequest.builder()
						.manifest(ApplicationManifest.builder()
								.name(getAppName(instid))
								.path(Paths.get(verspath))
								.route(Route.builder().route(getHost(instid, expohost) + "." + properties.getDomain()).build())
								.buildpack(properties.getPhpBuildpack())
								.services(services)
								.memory(256)
								.timeout(180)
								.environmentVariable("TZ", tz)
								.build())
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
	 * @param instid	The name of the instance as it is exposed to the Web
	 * @return	The Mono to signal the end of the async process (produce nothng indeed)
	 */
	public Mono<Void> deleteMatomoCfAppBindToGlobalSharedDb(String instid) {
		LOGGER.debug("CFMGR::deleteMatomoCfAppBindToGlobalSharedDb: instId={}", instid);
		return cfops.services().unbind(UnbindServiceInstanceRequest.builder()
				.serviceInstanceName(GLOBSHARDBINSTNAME)
				.applicationName(getAppName(instid))
				.build())
				.doOnError(t -> {
					LOGGER.error("CFMGR::deleteMatomoCfAppBindToGlobalSharedDb: problem to unbind from DB.", t);
					cfops.applications().delete(DeleteApplicationRequest.builder()
							.deleteRoutes(true)
							.name(getAppName(instid))
							.build())
					.doOnError(tt -> {
						LOGGER.error("CFMGR::deleteMatomoCfAppBindToGlobalSharedDb: problem to delete app.", tt);
					})
					.doOnSuccess(tt -> {
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
						LOGGER.error("CFMGR::deleteMatomoCfAppBindToGlobalSharedDb: problem to delete app.", tt);
					})
					.doOnSuccess(tt -> {
						LOGGER.debug("CFMGR::deleteMatomoCfAppBindToGlobalSharedDb: app unbound and deleted.");
					})
					.subscribe();
				});
	}

	public Mono<AppConfHolder> getInstanceConfigFile(String instid, String version) {
		LOGGER.debug("CFMGR::getInstanceConfigFile: instid={}, version={}", instid, version);
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
						if (properties.getMatomoDebug()) {
							Files.write(Paths.get(target + "config.ini.php"), "\n[Tracker]\ndebug = 1\nenable_sql_profiler = 1\n".getBytes(), StandardOpenOption.APPEND);
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

    private void waitOnSub(Object osub) {
		synchronized (osub) {
			try {
				osub.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}		    	
    }

	public class ExistDbSubscriber extends BaseSubscriber<ServiceInstance> {
		private boolean exist = false;
		boolean isExisted() {
			return exist;
		}
		public void hookOnNext(ServiceInstance si) {
			LOGGER.debug("CFMGR::   instance found -> name=" + si.getClass().getName());
			exist = true;
		}
		public void hookOnError(Throwable throwable) {
			LOGGER.debug("CFMGR::   instance not found -> " + throwable.getMessage());
		}
		public void hookFinally(SignalType st) {
			LOGGER.debug("CFMGR::   instance search finally");
			synchronized (this) {
				this.notifyAll();
			}
		}
	}

	public class CreateDbSubscriber extends BaseSubscriber<Void> {
		private Throwable t = null;
		Throwable getCause() {
			return t;
		}
		public void hookOnError(Throwable throwable) {
			LOGGER.debug("CFMGR::   instance not created -> " + throwable.getMessage());
			t = throwable;
		}
		public void hookOnNext() {
			LOGGER.debug("CFMGR::   instance created");
		}
		public void hookFinally(SignalType st) {
			LOGGER.debug("CFMGR::   instance creation finally");
			synchronized (this) {
				this.notifyAll();
			}
		}
	}
}
