/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.cfmgr;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.cloudfoundry.operations.services.GetServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;
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
	@Autowired
	private CloudFoundryOperations cfops;
	@Autowired
	private ReactorCloudFoundryClient cfclient;
	@Autowired
	private CloudFoundryMgrProperties properties;
	private final static String GLOBSHARDBINSTNAME = "matomo-service-db";
	private final static String PHPBUILDPACK = "php_buildpack";
	private final String MATOMO_ANPREFIX = "MATOMO_";
	private final String MATOMO_AUPREFIX = "M";
	private String sshHost;
	private int sshPort;
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
	public Mono<Void> deployMatomoCfAppBindToGlobalSharedDb(String instid, String version, String expohost) {
		LOGGER.debug("CFMGR::createMatomoCfAppBindToGlobalSharedDb: instId={}", instid);
		matomoReleases.activateVersionPath(instid, version);
		String verspath = matomoReleases.getVersionPath(version, instid);
		LOGGER.debug("File for Matomo bits: " + verspath);
		String[] serviceList = { GLOBSHARDBINSTNAME };
		return cfops.applications()
				.pushManifest(PushApplicationManifestRequest.builder()
						.manifest(ApplicationManifest.builder()
								.path(Paths.get(verspath))
								.domain(properties.getDomain())
								.buildpack(PHPBUILDPACK)
								.name(getAppName(instid))
								.host(instid.equals(expohost) ? getAppUrlPrefix(expohost) : expohost)
								.services(serviceList)
								.memory(256)
								.timeout(180)
								.build())
						.build());
	}

	/**
	 * Launch the deletion of the CF app for a dev flavor instance of Matomo service.
	 * @param instid	The name of the instance as it is exposed to the Web
	 * @return	The Mono to signal the end of the async process (produce nothng indeed)
	 */
	public Mono<Void> deleteMatomoCfAppBindToGlobalSharedDb(String instid) {
		LOGGER.debug("CFMGR::deleteMatomoCfAppBindToGlobalSharedDb: instId={}", instid);
		return cfops.applications().delete(
				DeleteApplicationRequest.builder()
				.deleteRoutes(true)
				.name(getAppName(instid))
				.build());
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
					SSHClient ssh = new SSHClient();
					ssh.addHostKeyVerifier(new PromiscuousVerifier());
					try {
						ssh.loadKnownHosts();
						ssh.connect(sshHost, sshPort);
						ssh.authPassword("cf:" + appidh.appId + "/0", pwd);
						String target = matomoReleases.getVersionPath(version, instid) + File.separator + "config" + File.separator;
						ssh.newSCPFileTransfer().download("/home/vcap/app/htdocs/config/config.ini.php", new FileSystemFile(target));
						appidh.fileContent = Files.readAllBytes(Paths.get(target + "config.ini.php"));
						sink.success(appidh);
					} catch (Exception e) {
						e.printStackTrace();
						sink.error(e);
					} finally {
						try {
							ssh.disconnect();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				})
				.subscribe();
			})
			.subscribe();
		});
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
				// TODO Auto-generated catch block
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
