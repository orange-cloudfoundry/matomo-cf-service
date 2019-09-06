/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.servicebroker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.GetLastServiceBindingOperationRequest;
import org.springframework.cloud.servicebroker.model.binding.GetLastServiceBindingOperationResponse;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * @author P. Déchamboux
 *
 */
@Service
public class MatomoServiceInstanceBindingService implements ServiceInstanceBindingService {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	@Override
	public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(
			CreateServiceInstanceBindingRequest request) {
		LOGGER.debug("BROKER::createServiceInstanceBinding: appId={}, appRoute={}, bindId={}, serviceInstId={}",
				request.getBindResource().getAppGuid(),
				request.getBindResource().getRoute(),
				request.getBindingId(),
				request.getServiceInstanceId());
		CreateServiceInstanceBindingResponse resp = CreateServiceInstanceAppBindingResponse.builder()
				.async(false)
				.operation("bind")
				.build();
		return Mono.just(resp);
	}

	@Override
	public Mono<GetServiceInstanceBindingResponse> getServiceInstanceBinding(GetServiceInstanceBindingRequest request) {
		LOGGER.debug("BROKER::getServiceInstanceBinding");
		GetServiceInstanceBindingResponse resp = GetServiceInstanceAppBindingResponse.builder()
				.build();
		return Mono.just(resp);
	}

	@Override
	public Mono<GetLastServiceBindingOperationResponse> getLastOperation(
			GetLastServiceBindingOperationRequest request) {
		LOGGER.debug("BROKER::getLastOperation");
		GetLastServiceBindingOperationResponse resp = GetLastServiceBindingOperationResponse.builder()
				.build();
		return Mono.just(resp);
	}

	@Override
	public Mono<DeleteServiceInstanceBindingResponse> deleteServiceInstanceBinding(
			DeleteServiceInstanceBindingRequest request) {
		LOGGER.debug("BROKER::deleteServiceInstanceBinding");
		DeleteServiceInstanceBindingResponse resp = DeleteServiceInstanceBindingResponse.builder()
				.build();
		return Mono.just(resp);
	}
}
