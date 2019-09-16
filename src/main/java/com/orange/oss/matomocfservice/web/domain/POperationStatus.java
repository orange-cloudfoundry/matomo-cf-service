/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.web.domain;

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
	}

	public POperationStatus(String id, String opcode, String opstate, PPlatform ppf) {
		this.id = id;
		this.lastOperation = opcode;
		this.lastOperationState = opstate;
		this.platform = ppf;
	}

	public String getId() {
		return this.id;
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