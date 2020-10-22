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

package com.orange.oss.matomocfservice.web.domain;

import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.springframework.cloud.servicebroker.model.instance.OperationState;

/**
 * @author P. Déchamboux
 *
 */
@Entity
@Table(name = "operationstates")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class POperationStatus {
	protected final static int LENGTH_ID = 36;
	private final static int LENGTH_OPCODE = 32;
	private final static int LENGTH_OPSTATE = 12;

	@Id
	@Column(length = LENGTH_ID, updatable = false, nullable = false)
	private String uuid;

	private final ZonedDateTime createTime;

	private ZonedDateTime updateTime;

	@Column(length = LENGTH_OPCODE)
	private String lastOperation;

	@Column(length = LENGTH_OPSTATE)
	private String lastOperationState;

	private boolean locked;

	@ManyToOne
	@JoinColumn(name = "platform_id", referencedColumnName = "id", nullable = false)
	PPlatform platform;

	@SuppressWarnings("unused")
	protected POperationStatus() {
		this.uuid = null;
		this.createTime = null;
	}

	public POperationStatus(String uuid, OpCode opcode, OperationState opstate, PPlatform ppf) {
		this.uuid = uuid;
		this.createTime = ZonedDateTime.now();
		this.updateTime = this.createTime;		
		this.lastOperation = opcode.toString();
		this.lastOperationState = opstate.toString();
		this.locked = false;
		this.platform = ppf;
	}

	public String getUuid() {
		return this.uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public ZonedDateTime getCreateTime() {
		return this.createTime;
	}

	public ZonedDateTime getUpdateTime() {
		return this.updateTime;
	}

	protected void touch() {
		this.updateTime = ZonedDateTime.now();
	}

	public void setLastOperation(OpCode opCode) {
		lastOperation = opCode.toString();
		touch();
	}

	public OpCode getLastOperation() {
		if (OpCode.CREATE_SERVICE_INSTANCE.toString().equals(lastOperation)) {
			return OpCode.CREATE_SERVICE_INSTANCE;
		}
		if (OpCode.UPDATE_SERVICE_INSTANCE.toString().equals(lastOperation)) {
			return OpCode.UPDATE_SERVICE_INSTANCE;
		}
		if (OpCode.DELETE_SERVICE_INSTANCE.toString().equals(lastOperation)) {
			return OpCode.DELETE_SERVICE_INSTANCE;
		}
		if (OpCode.CREATE_SERVICE_INSTANCE_APP_BINDING.toString().equals(lastOperation)) {
			return OpCode.CREATE_SERVICE_INSTANCE_APP_BINDING;
		}
		return OpCode.DELETE_SERVICE_INSTANCE_APP_BINDING;
	}

	public void setLastOperationState(OperationState opst) {
		lastOperationState = opst.getValue();
		touch();
	}

	public OperationState getLastOperationState() {
		if (OperationState.SUCCEEDED.getValue().equals(lastOperationState)) {
			return OperationState.SUCCEEDED;
		}
		if (OperationState.IN_PROGRESS.getValue().equals(lastOperationState)) {
			return OperationState.IN_PROGRESS;
		}
		return OperationState.FAILED;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}
	public boolean isLocked() {
		return this.locked;
	}

	public PPlatform getPlatform() {
		return this.platform;
	}

	public static enum OpCode {
		CREATE_SERVICE_INSTANCE ("CreateServiceInstance"),
		UPDATE_SERVICE_INSTANCE ("UpdateServiceInstance"),
		DELETE_SERVICE_INSTANCE ("DeleteServiceInstance"),
		CREATE_SERVICE_INSTANCE_APP_BINDING ("CreateServiceInstanceAppBinding"),
		DELETE_SERVICE_INSTANCE_APP_BINDING ("DeleteServiceInstanceAppBinding");

		private String opCode;

		private OpCode(String opcode) {
			this.opCode = opcode;
		}

		@Override
		public String toString() {
			return this.opCode;
		}
	}
}
