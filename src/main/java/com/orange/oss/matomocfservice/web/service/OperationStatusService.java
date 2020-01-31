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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.model.instance.OperationState;

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
	@Autowired
	private PPlatformRepository pfRepo;
	@Autowired
	private POperationStatusRepository osRepo;
	@Autowired
	private ApplicationInformation applicationInformation;
	@Autowired
	EntityManagerFactory entityManagerFactory;

	public OperationAndState getLastOperationAndState(String instanceId, String platformId) {
		EntityManager em = beginTx();
		Optional<POperationStatus> opms = osRepo.findById(instanceId);
		if (! opms.isPresent()) {
			LOGGER.error("SERV::getLastOperationAndState: unknow service instance.");
			commitTx(em);
			return null;
		}
		POperationStatus pos = opms.get();
		if (!pos.getPlatform().getId().equals(platformId)) {
			LOGGER.error("SERV::getLastOperationAndState: wrong platform.");
			commitTx(em);
			return null;
		}
		if (pos.getLastOperationState() == OperationState.IN_PROGRESS) {
			Duration d = Duration.between(pos.getUpdateTime(), ZonedDateTime.now());
			if (d.getSeconds() > applicationInformation.getTimeoutFrozenInProgress()) {
				LOGGER.error("SERV::getLastOperationAndState: last operation is in progress for too long (more than {} seconds): considered failed.", applicationInformation.getTimeoutFrozenInProgress());
				pos.setLastOperationState(OperationState.FAILED);
				osRepo.save(pos);
			} else {
				LOGGER.debug("SERV::getLastOperationAndState: Operation={} State={} for {}min {}sec", pos.getLastOperation().toString(), pos.getLastOperationState().toString(), d.getSeconds() / 60, d.getSeconds() % 60);			
			}
		} else {
			LOGGER.debug("SERV::getLastOperationAndState: Operation={} State={}", pos.getLastOperation().toString(), pos.getLastOperationState().toString());
		}
		OperationAndState opandst2fill = new OperationAndState();
		opandst2fill.setOperation(pos.getLastOperation());
		opandst2fill.setState(pos.getLastOperationState());
		commitTx(em);
		return opandst2fill;
	}

	public static class OperationAndState {
		private POperationStatus.OpCode opCode;
		private OperationState opState;

		public OperationAndState() {
			opCode = null;
			opState = null;
		}

		public POperationStatus.OpCode getOperation() {
			return opCode;
		}

		public String getOperationMessage() {
			switch (opCode) {
			case CREATE_SERVICE_INSTANCE:
				return "Create Matomo Service Instance";
			case UPDATE_SERVICE_INSTANCE:
				return "Update Matomo Service Instance";
			case DELETE_SERVICE_INSTANCE:
				return "Delete Matomo Service Instance";
			case CREATE_SERVICE_INSTANCE_APP_BINDING:
				return "Create Matomo Service Instance Application Binding";
			case DELETE_SERVICE_INSTANCE_APP_BINDING:
				return "Delete Matomo Service Instance Application Binding";
			}
			return "";
		}

		void setOperation(POperationStatus.OpCode opCode) {
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
		Optional<PPlatform> oppf = pfRepo.findById(platformId);
		if (oppf.isPresent()) {
			return oppf.get();
		}
		LOGGER.error("SERV::getPPlatform: wrong platform.");
		throw new EntityNotFoundException("Platform with ID=" + platformId + " not known");
	}

	protected EntityManager beginTx() {
		EntityManager em = entityManagerFactory.createEntityManager();
		LOGGER.debug("Begin TX: {}", em);
		em.getTransaction().begin();
		return em;
	}

	protected void commitTx(EntityManager em) {
		LOGGER.debug("Commit TX: {}", em);
		em.getTransaction().commit();
		em.close();
	}
}
