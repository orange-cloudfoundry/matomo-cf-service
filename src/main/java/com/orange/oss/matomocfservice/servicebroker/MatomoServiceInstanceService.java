/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.servicebroker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import com.orange.oss.matomocfservice.api.model.MatomoInstance;
import com.orange.oss.matomocfservice.api.model.OpCode;
import com.orange.oss.matomocfservice.api.model.PlatformKind;
import com.orange.oss.matomocfservice.web.service.MatomoInstanceService;
import com.orange.oss.matomocfservice.web.service.MatomoInstanceService.OperationAndState;

import reactor.core.publisher.Mono;

/**
 * @author P. Déchamboux
 *
 */
@Service
public class MatomoServiceInstanceService implements ServiceInstanceService {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private MatomoInstanceService miServ;

	@Override
	public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
		LOGGER.debug("BROKER::createServiceInstance: platformId={} / serviceId={}", request.getPlatformInstanceId(), request.getServiceInstanceId());
//		LOGGER.debug("BROKER::   request=" + request.toString());
//		LOGGER.debug("BROKER::   platform={}", request.getContext().getPlatform());
		PlatformKind pfkind;
		String instn, tid, stid;
		switch (request.getContext().getPlatform()) {
		case "cloudfoundry":
			pfkind = PlatformKind.CLOUDFOUNDRY;
			instn = (String)request.getContext().getProperty("instance_name");
			tid = (String)request.getContext().getProperty("organizationGuid");
			stid = (String)request.getContext().getProperty("spaceGuid");
			break;
		default:
			LOGGER.warn("BROKER::   unknown kind of platform -> " + request.getContext().getPlatform());
			pfkind = PlatformKind.OTHER;
			instn = tid = stid = "";
		}
		MatomoInstance mi = miServ.createMatomoInstance(new MatomoInstance()
				.uuid(request.getServiceInstanceId())
				.serviceDefinitionId(request.getServiceDefinitionId())
				.name(instn)
				.tenantId(tid)
				.subtenantId(stid)
				.platformKind(pfkind)
				.platformApiLocation(request.getApiInfoLocation())
				.planId(request.getPlanId())
				.platformId(request.getPlatformInstanceId()));
		CreateServiceInstanceResponse resp = CreateServiceInstanceResponse.builder()
				.async(true)
				.dashboardUrl(mi.getDashboardUrl())
				.instanceExisted(false)
				.operation("Create Matomo Service Instance \"" + instn + "\"")
				.build();
		return Mono.just(resp);
	}

	@Override
	public Mono<GetLastServiceOperationResponse> getLastOperation(GetLastServiceOperationRequest request) {
		LOGGER.debug("BROKER::getServiceInstance: platformId={}, instanceId={}", request.getPlatformInstanceId(), request.getServiceInstanceId());
		OperationAndState opandstate = new OperationAndState();
		miServ.fillLastOperationAndState(request.getPlatformInstanceId(), request.getServiceInstanceId(), opandstate);
		GetLastServiceOperationResponse resp = GetLastServiceOperationResponse.builder()
				.deleteOperation(opandstate.getOperation().equals(OpCode.DELETE))
				.operationState(opandstate.getState())
				.description(opandstate.getOperationMessage())
				.build();
		return Mono.just(resp);
	}

	@Override
	public Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request) {
		LOGGER.debug("BROKER::getServiceInstance: platformId={}, instanceId={}", request.getPlatformInstanceId(), request.getServiceInstanceId());
		MatomoInstance mi = miServ.getMatomoInstance(request.getPlatformInstanceId(), request.getServiceInstanceId());
		GetServiceInstanceResponse resp = GetServiceInstanceResponse.builder()
				.serviceDefinitionId(mi.getServiceDefinitionId())
				.planId(mi.getPlanId())
				.dashboardUrl(mi.getDashboardUrl())
				.build();
		return Mono.just(resp);
	}

	@Override
	public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
		LOGGER.debug("BROKER::deleteServiceInstance: platformId={}, instanceId={}", request.getPlatformInstanceId(), request.getServiceInstanceId());
		Mono<DeleteServiceInstanceResponse> resp;
		try {
			miServ.deleteMatomoInstance(request.getPlatformInstanceId(), request.getServiceInstanceId());
			resp = Mono.just(DeleteServiceInstanceResponse.builder()
					.async(true)
					.build());
		} catch (Exception e) {
			resp = Mono.error(e);
		}
		return resp;
	}

	@Override
	public Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
		LOGGER.debug("BROKER::updateServiceInstance: platformId={}, instanceId={}", request.getPlatformInstanceId(), request.getServiceInstanceId());
		Mono<UpdateServiceInstanceResponse> resp;
		try {
			PlatformKind pfkind;
			String instn, tid, stid;
			switch (request.getContext().getPlatform()) {
			case "cloudfoundry":
				pfkind = PlatformKind.CLOUDFOUNDRY;
				instn = (String)request.getContext().getProperty("instance_name");
				tid = (String)request.getContext().getProperty("organizationGuid");
				stid = (String)request.getContext().getProperty("spaceGuid");
				break;
			default:
				LOGGER.warn("BROKER::   unknown kind of platform -> " + request.getContext().getPlatform());
				pfkind = PlatformKind.OTHER;
				instn = tid = stid = "";
			}
			miServ.updateMatomoInstance(new MatomoInstance()
					.uuid(request.getServiceInstanceId())
					.serviceDefinitionId(request.getServiceDefinitionId())
					.name(instn)
					.tenantId(tid)
					.subtenantId(stid)
					.platformKind(pfkind)
					.platformApiLocation(request.getApiInfoLocation())
					.planId(request.getPlanId())
					.platformId(request.getPlatformInstanceId()));
			resp = Mono.just(UpdateServiceInstanceResponse.builder()
					.async(true)
					.build());
		} catch (Exception e) {
			resp = Mono.error(e);
		}
		return resp;
	}
}
