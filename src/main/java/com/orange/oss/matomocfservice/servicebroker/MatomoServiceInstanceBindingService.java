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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.orange.oss.matomocfservice.api.model.OpCode;
import com.orange.oss.matomocfservice.web.service.BindingService;
import com.orange.oss.matomocfservice.web.service.OperationStatusService.OperationAndState;

import reactor.core.publisher.Mono;

/**
 * @author P. Déchamboux
 *
 */
@Service
public class MatomoServiceInstanceBindingService implements ServiceInstanceBindingService {
	private final static Logger LOGGER = LoggerFactory.getLogger(MatomoServiceInstanceBindingService.class);
	@Autowired
	private BindingService bindingServ;

	@Override
	public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(
			CreateServiceInstanceBindingRequest request) {
		LOGGER.debug("BROKER::createServiceInstanceBinding: appId={}, bindId={}, serviceInstId={}, parameters={}",
				request.getBindResource().getAppGuid(),
				request.getBindingId(),
				request.getServiceInstanceId(),
				request.getParameters().toString());
		boolean existed = bindingServ.createBinding(
				request.getBindingId(),
				request.getServiceInstanceId(),
				request.getBindResource().getAppGuid(),
				request.getParameters());
		CreateServiceInstanceBindingResponse resp = CreateServiceInstanceAppBindingResponse.builder()
				.async(false)
				.operation("Bound Matomo Instance <" + request.getServiceInstanceId() + "> to Application <" + request.getBindResource().getAppGuid() + ">")
				.bindingExisted(existed)
				.credentials(bindingServ.getCredentials(request.getBindingId(), request.getServiceInstanceId()))
				.build();
		return Mono.just(resp);
	}

	@Override
	public Mono<GetServiceInstanceBindingResponse> getServiceInstanceBinding(GetServiceInstanceBindingRequest request) {
		LOGGER.debug("BROKER::getServiceInstanceBinding: bindId={}, serviceInstId={}", request.getBindingId(), request.getServiceInstanceId());
		GetServiceInstanceBindingResponse resp = GetServiceInstanceAppBindingResponse.builder()
				.credentials(bindingServ.getCredentials(request.getBindingId(), request.getServiceInstanceId()))
				.parameters(bindingServ.getParameters(request.getBindingId(), request.getServiceInstanceId()))
				.build();
		return Mono.just(resp);
	}

	@Override
	public Mono<GetLastServiceBindingOperationResponse> getLastOperation(
			GetLastServiceBindingOperationRequest request) {
		LOGGER.debug("BROKER::getLastOperation");
		OperationAndState opandstate = bindingServ.getLastOperationAndState(request.getPlatformInstanceId(), request.getServiceInstanceId());
		return Mono.just(GetLastServiceBindingOperationResponse.builder()
				.deleteOperation(opandstate.getOperation().equals(OpCode.DELETE))
				.operationState(opandstate.getState())
				.description(opandstate.getOperationMessage())
				.build());
	}

	@Override
	public Mono<DeleteServiceInstanceBindingResponse> deleteServiceInstanceBinding(
			DeleteServiceInstanceBindingRequest request) {
		LOGGER.debug("BROKER::deleteServiceInstanceBinding: bindId={}, serviceInstId={}", request.getBindingId(), request.getServiceInstanceId());
		String error = bindingServ.deleteBinding(request.getPlatformInstanceId(), request.getBindingId());
		return Mono.just(DeleteServiceInstanceBindingResponse.builder()
				.async(false)
				.operation(error == null ? "Deleting Matomo service instance binding" : error)
				.build());
	}
}
