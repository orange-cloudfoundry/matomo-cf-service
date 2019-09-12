/**
 * 
 */
package com.orange.oss.matomocfservice.web.service;

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
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private PPlatformRepository pfRepo;
	@Autowired
	private POperationStatusRepository osRepo;
	@Autowired
	private PlatformService platformService;

	public OperationAndState getLastOperationAndState(String platformId, String instanceId) {
		LOGGER.debug("SERV::getLastOperationAndState: platformId={} instanceId={}", platformId, instanceId);
		PPlatform ppf = getPPlatform(platformId);
		Optional<POperationStatus> opms = osRepo.findById(instanceId);
		if (! opms.isPresent()) {
			throw new EntityNotFoundException("Matomo Instance with ID=" + instanceId + " not known in Platform with ID=" + platformId);
		}
		POperationStatus pos = opms.get();
		if (pos.getPlatform() != ppf) {
			throw new IllegalArgumentException("Wrong platform with ID=" + platformId + " for Service Instance with ID=" + instanceId);
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
				return "Create Matomo Service Instance";
			} else if (opCode.equals(OpCode.READ)) {
				return "Read Matomo Service Instance";
			} else if (opCode.equals(OpCode.UPDATE)) {
				return "Update Matomo Service Instance";
			} else {
				return "Delete Matomo Service Instance";
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
		String id = platformId == null ? platformService.getUnknownPlatformId() : platformId;
		Optional<PPlatform> oppf = pfRepo.findById(id);
		if (oppf.isPresent()) {
			return oppf.get();
		}
		throw new EntityNotFoundException("Platform with ID=" + id + " not known");
	}
}
