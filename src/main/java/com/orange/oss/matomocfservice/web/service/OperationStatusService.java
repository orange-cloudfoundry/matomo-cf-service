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

package com.orange.oss.matomocfservice.web.service;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

import javax.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.model.instance.OperationState;

import com.orange.oss.matomocfservice.api.model.OpCode;
import com.orange.oss.matomocfservice.web.domain.POperationStatus;
import com.orange.oss.matomocfservice.web.domain.PPlatform;
import com.orange.oss.matomocfservice.web.repository.POperationStatusRepository;
import com.orange.oss.matomocfservice.web.repository.PPlatformRepository;

/**
 * @author P. Déchamboux
 *
 */
public abstract class OperationStatusService {
	private final static Logger LOGGER = LoggerFactory.getLogger(OperationStatusService.class);
	private final long TIMEOUT_FROZENINPROGRESS = 1800; // in seconds
	@Autowired
	private PPlatformRepository pfRepo;
	@Autowired
	private POperationStatusRepository osRepo;
	@Autowired
	private PlatformService platformService;

	public OperationAndState getLastOperationAndState(String platformId, String instanceId) {
		PPlatform ppf = getPPlatform(platformId);
		Optional<POperationStatus> opms = osRepo.findById(instanceId);
		if (! opms.isPresent()) {
			LOGGER.error("SERV::getLastOperationAndState: unknow service instance.");
			return null;
		}
		POperationStatus pos = opms.get();
		if (pos.getPlatform() != ppf) {
			LOGGER.error("SERV::getLastOperationAndState: wrong platform.");
			return null;
		}
		Duration d = Duration.between(pos.getUpdateTime(), ZonedDateTime.now());
		if (d.getSeconds() > TIMEOUT_FROZENINPROGRESS) {
			LOGGER.error("SERV::getLastOperationAndState: last operation is in progress for too long (more than {} minutes): considered failed.", TIMEOUT_FROZENINPROGRESS);
			pos.setLastOperationState(OperationState.FAILED);
			osRepo.save(pos);
		}
		if (pos.getLastOperationState() == OperationState.IN_PROGRESS) {
			LOGGER.debug("SERV::getLastOperationAndState: Operation={} State={} for {}'{}''", pos.getLastOperation().toString(), pos.getLastOperationState().toString(), d.getSeconds() / 60, d.getSeconds() % 60);			
		} else {
			LOGGER.debug("SERV::getLastOperationAndState: Operation={} State={}", pos.getLastOperation().toString(), pos.getLastOperationState().toString());
		}
		OperationAndState opandst2fill = new OperationAndState();
		opandst2fill.setOperation(pos.getLastOperation());
		opandst2fill.setState(pos.getLastOperationState());
		return opandst2fill;
	}

	public static class OperationAndState {
		private OpCode opCode;
		private OperationState opState;

		public OperationAndState() {
			opCode = null;
			opState = null;
		}

		public OpCode getOperation() {
			return opCode;
		}

		public String getOperationMessage() {
			if (opCode.equals(OpCode.CREATE)) {
				return "Create Matomo Service Instance or Binding";
			} else if (opCode.equals(OpCode.READ)) {
				return "Read Matomo Service Instance or Binding";
			} else if (opCode.equals(OpCode.UPDATE)) {
				return "Update Matomo Service Instance or Binding";
			} else {
				return "Delete Matomo Service Instance or Binding";
			}
		}

		void setOperation(OpCode opCode) {
			this.opCode = opCode;
		}

		public OperationState getState() {
			return opState;
		}

		void setState(OperationState opState) {
			this.opState = opState;
		}
	}

	protected PPlatform getPPlatform(String platformId) {
		//LOGGER.debug("SERV::getPPlatform: platformId={}", platformId);
		String id = platformId == null ? platformService.getUnknownPlatformId() : platformId;
		Optional<PPlatform> oppf = pfRepo.findById(id);
		if (oppf.isPresent()) {
			return oppf.get();
		}
		LOGGER.error("SERV::getPPlatform: wrong platform.");
		throw new EntityNotFoundException("Platform with ID=" + id + " not known");
	}
}
