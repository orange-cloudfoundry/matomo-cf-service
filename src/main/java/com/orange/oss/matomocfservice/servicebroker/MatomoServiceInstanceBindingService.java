/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.servicebroker;

import org.springframework.beans.factory.annotation.Autowired;
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

//import com.orange.oss.matomocfservice.cfmgr.CfMgr;

import reactor.core.publisher.Mono;

/**
 * @author P. Déchamboux
 *
 */
@Service
public class MatomoServiceInstanceBindingService implements ServiceInstanceBindingService {
//	@Autowired
//	CfMgr cfMgr;

	@Override
	public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(
			CreateServiceInstanceBindingRequest request) {
		
		CreateServiceInstanceBindingResponse resp = CreateServiceInstanceAppBindingResponse.builder()
				.build();
		
		return null;
	}

	@Override
	public Mono<GetServiceInstanceBindingResponse> getServiceInstanceBinding(GetServiceInstanceBindingRequest request) {
		
		GetServiceInstanceBindingResponse resp = GetServiceInstanceAppBindingResponse.builder()
				.build();
		
		return null;
	}

	@Override
	public Mono<GetLastServiceBindingOperationResponse> getLastOperation(
			GetLastServiceBindingOperationRequest request) {
		
		GetLastServiceBindingOperationResponse resp = GetLastServiceBindingOperationResponse.builder()
				.build();
		
		return null;
	}

	@Override
	public Mono<DeleteServiceInstanceBindingResponse> deleteServiceInstanceBinding(
			DeleteServiceInstanceBindingRequest request) {
		
		DeleteServiceInstanceBindingResponse resp = DeleteServiceInstanceBindingResponse.builder()
				.build();
		
		return null;
	}
}
