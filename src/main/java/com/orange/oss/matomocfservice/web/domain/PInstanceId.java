/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.web.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author P. Déchamboux
 *
 */
@Entity
@Table(name = "instanceids")
public class PInstanceId {
	@Id
	private final Integer id;
	
	private boolean allocated;

	PInstanceId() {
		this.id = -1;
	}

	public PInstanceId(int id) {
		this.id = id;
		this.allocated = false;
	}

	public int getId() {
		return id;
	}

	public boolean isAllocated() {
		return allocated;
	}

	public void setAllocated(boolean alloc) {
		this.allocated = alloc;
	}
}
