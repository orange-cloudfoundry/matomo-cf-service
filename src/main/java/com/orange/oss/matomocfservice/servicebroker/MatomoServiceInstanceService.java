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

package com.orange.oss.matomocfservice.servicebroker;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceExistsException;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;

import com.orange.oss.matomocfservice.web.domain.PMatomoInstance;
import com.orange.oss.matomocfservice.web.domain.POperationStatus;
import com.orange.oss.matomocfservice.web.domain.Parameters;
import com.orange.oss.matomocfservice.web.service.MatomoInstanceService;
import com.orange.oss.matomocfservice.web.service.MatomoReleases;
import com.orange.oss.matomocfservice.web.service.OperationStatusService.OperationAndState;

import reactor.core.publisher.Mono;

/**
 * @author P. Déchamboux
 *
 */
@Service
public class MatomoServiceInstanceService implements ServiceInstanceService {
	private final static Logger LOGGER = LoggerFactory.getLogger(MatomoServiceInstanceService.class);
	private final static String PARAM_VERSION = "matomoVersion";
	private final static String PARAM_TZ = "matomoTimeZone";
	private final static String PARAM_INSTANCES = "matomoInstances";
	private final static String PARAM_VERSIONUPGRADEPOLICY = "versionUpgradePolicy";
	private final static String PARAM_MEMORYSIZE = "memorySize";
	private final static int MAX_INSTANCES = 10;
	private final static int CLUSTERSIZE_DEFAULT = 2;
	private final static int MEMSIZE_SMALL = 256;
	private final static int MEMSIZE_DEFAULT = 512;
	private final static int MEMSIZE_MAX = 2048;
	@Autowired
	private MatomoReleases matomoReleases;
	@Autowired
	private MatomoInstanceService miServ;

	@Override
	public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
		LOGGER.debug("BROKER::createServiceInstance: platformId={} / serviceId={}", request.getPlatformInstanceId(), request.getServiceInstanceId());
//		LOGGER.debug("BROKER::   request=" + request.toString());
		LOGGER.debug("BROKER::   platform={}", request.getContext().getPlatform());
		PMatomoInstance.PlatformKind pfkind;
		String instname, tenantid, subtenantid;
		switch (request.getContext().getPlatform()) {
		case "cloudfoundry":
			pfkind = PMatomoInstance.PlatformKind.CLOUDFOUNDRY;
			instname = (String)request.getContext().getProperty("instance_name");
			tenantid = (String)request.getContext().getProperty("organizationGuid");
			subtenantid = (String)request.getContext().getProperty("spaceGuid");
			break;
		default:
			LOGGER.warn("BROKER::   unknown kind of platform -> " + request.getContext().getPlatform());
			pfkind = PMatomoInstance.PlatformKind.OTHER;
			instname = tenantid = subtenantid = "";
		}
		PMatomoInstance pmi = miServ.createMatomoInstance(
				request.getServiceInstanceId(),
				request.getServiceDefinitionId(),
				instname,
				tenantid,
				subtenantid,
				pfkind,
				request.getApiInfoLocation(),
				request.getPlanId(),
				request.getPlatformInstanceId(),
				toParameters(request.getParameters(), request.getPlanId()));
		if (pmi == null) {
			throw new ServiceInstanceExistsException(request.getServiceInstanceId(), request.getServiceDefinitionId());
		}
		return Mono.just(CreateServiceInstanceResponse.builder()
				.async(true)
				.dashboardUrl(miServ.getInstanceUrl(pmi))
				.instanceExisted(false)
				.operation("Create Matomo Service Instance \"" + instname + "\"")
				.build());
	}

	@Override
	public Mono<GetLastServiceOperationResponse> getLastOperation(GetLastServiceOperationRequest request) {
		LOGGER.debug("BROKER::getLastOperation: platformId={}, instanceId={}", request.getPlatformInstanceId(), request.getServiceInstanceId());
		OperationAndState opandstate = miServ.getLastOperationAndState(request.getPlatformInstanceId(), request.getServiceInstanceId());
		if (opandstate == null) {
			throw new ServiceInstanceDoesNotExistException("Cannot find instance " + request.getServiceInstanceId());
		}
		return Mono.just(GetLastServiceOperationResponse.builder()
				.deleteOperation(opandstate.getOperation().equals(POperationStatus.OpCode.DELETE_SERVICE_INSTANCE))
				.operationState(opandstate.getState())
				.description(opandstate.getOperationMessage())
				.build());
	}

	@Override
	public Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request) {
		LOGGER.debug("BROKER::getServiceInstance: platformId={}, instanceId={}", request.getPlatformInstanceId(), request.getServiceInstanceId());
		PMatomoInstance pmi = miServ.getMatomoInstance(request.getPlatformInstanceId(), request.getServiceInstanceId());
		if (pmi == null) {
			throw new ServiceInstanceDoesNotExistException("Cannot find instance " + request.getServiceInstanceId());
		}
		return Mono.just(GetServiceInstanceResponse.builder()
				.serviceDefinitionId(pmi.getServiceDefinitionId())
				.planId(pmi.getPlanId())
				.dashboardUrl(miServ.getInstanceUrl(pmi))
				.parameters(toMap(pmi.getParameters()))
				.build());
	}

	@Override
	public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
		LOGGER.debug("BROKER::deleteServiceInstance: platformId={}, instanceId={}", request.getPlatformInstanceId(), request.getServiceInstanceId());
		if (miServ.deleteMatomoInstance(request.getPlatformInstanceId(), request.getServiceInstanceId()) == null) {
			throw new ServiceInstanceDoesNotExistException("Cannot find instance " + request.getServiceInstanceId());
		}
		return Mono.just(DeleteServiceInstanceResponse.builder()
				.async(true)
				.operation("Delete Matomo service instance \"" + request.getServiceInstanceId() + "\"")
				.build());
	}

	@Override
	public Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
		LOGGER.debug("BROKER::updateServiceInstance: platformId={}, instanceId={}", request.getPlatformInstanceId(), request.getServiceInstanceId());
		String instn;
		switch (request.getContext().getPlatform()) {
		case "cloudfoundry":
			instn = (String)request.getContext().getProperty("instance_name");
			break;
		default:
			LOGGER.warn("BROKER::   unknown kind of platform -> " + request.getContext().getPlatform());
			instn = "";
		}
		PMatomoInstance pmi = miServ.updateMatomoInstance(
				request.getServiceInstanceId(),
				request.getPlatformInstanceId(),
				toParameters(request.getParameters(), request.getPlanId()));
		if (pmi == null) {
			throw new ServiceInstanceDoesNotExistException("Cannot find instance " + request.getServiceInstanceId());
		}
		return Mono.just(UpdateServiceInstanceResponse.builder()
					.async(true)
					.dashboardUrl(miServ.getInstanceUrl(pmi))
					.operation("Update Matomo Service Instance \"" + instn + "\"")
					.build());
	}

	private Parameters toParameters(Map<String, Object> map, String planid) {
		return new Parameters()
				.autoVersionUpgrade(getAutomaticVersionUpgrade(map))
				.cfInstances(getInstances(map, planid))
				.memorySize(getMemorySize(map, planid))
				.timeZone(getTimeZone(map))
				.version(getVersion(map));
	}

	private Map<String, Object> toMap(Parameters mip) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(PARAM_VERSION, mip.getVersion());
		params.put(PARAM_TZ, mip.getTimeZone());
		params.put(PARAM_INSTANCES, mip.getCfInstances());
		params.put(PARAM_VERSIONUPGRADEPOLICY, mip.isAutoVersionUpgrade());
		params.put(PARAM_MEMORYSIZE, mip.getMemorySize());
		return params;
	}

	private String getVersion(Map<String, Object> parameters) {
		String instversion = (String) parameters.get(PARAM_VERSION);
		if (instversion == null) {
			instversion = matomoReleases.getDefaultReleaseName();
		} else if (instversion.equals("latest")) {
			instversion = matomoReleases.getLatestReleaseName();
		} else if (! matomoReleases.isVersionAvailable(instversion)) {
				LOGGER.warn("SERV::getVersion: version {} is not supported -> switch to default one.", instversion);
				throw new RuntimeException("Version <" + instversion + "> is not supported by this Matomo CF Service!!");
		}
		LOGGER.debug("SERV::getVersion: return {}", instversion);
		return instversion;
	}

	private boolean getAutomaticVersionUpgrade(Map<String, Object> parameters) {
		String policy = (String) parameters.get(PARAM_VERSIONUPGRADEPOLICY);
		boolean bpol;
		if (policy == null) {
			bpol = true;
		} else {
			policy = policy.toUpperCase();
			if (policy.equals("AUTOMATIC")) {
				bpol =  true;
			} else if (policy.equals("EXPLICIT")) {
				bpol =  false;
			} else {
				LOGGER.warn("SERV::getAutomaticVersionUpgrade: <{}> is a wrong value for version upgrade policy -> should be either AUTOMATIC or EXPLICIT.", policy);
				throw new IllegalArgumentException("Version upgrade policy <" + policy + "> is a wrong value!!");
			}
		}
		LOGGER.debug("SERV::getAutomaticVersionUpgrade: return {}", bpol);
		return bpol;
	}

	private int getInstances(Map<String, Object> parameters, String planid) {
		Integer instances;
		if (planid.equals(ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID)) {
			instances = 1;
		} else {
		// then this is a cluster conf
			if (planid.equals(ServiceCatalogConfiguration.PLANMATOMOSHARDB_UUID)) {
				instances =  CLUSTERSIZE_DEFAULT;
			} else {
				instances = (Integer) parameters.get(PARAM_INSTANCES);
				if (instances == null) {
					instances =  CLUSTERSIZE_DEFAULT;
				} else if (instances < CLUSTERSIZE_DEFAULT) {
					instances =  CLUSTERSIZE_DEFAULT;
				} else if (instances > MAX_INSTANCES) {
					instances =  MAX_INSTANCES;
				}
			}
		}
		LOGGER.debug("SERV::getInstances: return {}", instances);
		return instances;
	}

	/**
	 * Define the memory size in MB of containers that run Matomo.
	 * @param parameters
	 * @param planid
	 * @return
	 */
	private int getMemorySize(Map<String, Object> parameters, String planid) {
		Integer memsize;
		if (planid.equals(ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID)) {
			memsize = MEMSIZE_SMALL;
		} else {
			// then this is a cluster conf
			memsize = (Integer) parameters.get(PARAM_MEMORYSIZE);
			if (memsize == null) {
				memsize = MEMSIZE_DEFAULT;
			} else if (memsize < MEMSIZE_SMALL) {
				memsize = MEMSIZE_SMALL;
			} else if (memsize > MEMSIZE_MAX) {
				memsize = MEMSIZE_MAX;
			}
		}
		LOGGER.debug("SERV::getMemorySize: return {}MB", memsize);
		return memsize;
	}

	private String getTimeZone(Map<String, Object> parameters) {
		String tz = (String) parameters.get(PARAM_TZ);
		if (tz == null) {
			tz = "Europe/Paris";
		}
		LOGGER.debug("SERV::getTimeZone: return {}", tz);
		return tz;
	}
}
