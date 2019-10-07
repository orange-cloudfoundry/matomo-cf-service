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

import com.orange.oss.matomocfservice.api.model.OpCode;

/**
 * @author P. Déchamboux
 *
 */
@Entity
@Table(name = "operationstates")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class POperationStatus {
	protected final static int LENGTH_ID = 36;
	private final static int LENGTH_OPCODE = 8;
	private final static int LENGTH_OPSTATE = 12;

	@Id
	@Column(length = LENGTH_ID, updatable = false, nullable = false)
	private final String id;

	private final ZonedDateTime createTime;

	private ZonedDateTime updateTime;

	@Column(length = LENGTH_OPCODE)
	private String lastOperation;

	@Column(length = LENGTH_OPSTATE)
	private String lastOperationState;
	
	@ManyToOne
	@JoinColumn(name = "platform_id", referencedColumnName = "id", nullable = false)
	PPlatform platform;

	@SuppressWarnings("unused")
	protected POperationStatus() {
		this.id = null;
		this.createTime = null;
	}

	public POperationStatus(String id, String opcode, String opstate, PPlatform ppf) {
		this.id = id;
		this.createTime = ZonedDateTime.now();
		this.updateTime = this.createTime;		
		this.lastOperation = opcode;
		this.lastOperationState = opstate;
		this.platform = ppf;
	}

	public String getId() {
		return this.id;
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
	}

	public OpCode getLastOperation() {
		return lastOperation.equals(OpCode.CREATE.toString())
				? OpCode.CREATE
						: (lastOperation.equals(OpCode.READ.toString())
								? OpCode.READ
										: (lastOperation.equals(OpCode.UPDATE.toString())
												? OpCode.UPDATE
														: OpCode.DELETE));	
	}

	public void setLastOperationState(OperationState opst) {
		lastOperationState = opst.getValue();
	}

	public OperationState getLastOperationState() {
		OperationState res;
		if (lastOperationState.equals(OperationState.IN_PROGRESS.getValue())) {
			res = OperationState.IN_PROGRESS;
		} else if (lastOperationState.equals(OperationState.SUCCEEDED.getValue())) {
			res = OperationState.SUCCEEDED;
		} else {
			res = OperationState.FAILED;
		}
		return res;
	}

	public PPlatform getPlatform() {
		return this.platform;
	}
}
