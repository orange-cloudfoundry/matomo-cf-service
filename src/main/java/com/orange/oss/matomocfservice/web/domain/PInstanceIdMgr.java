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
@Table(name = "instanceidmgr")
public class PInstanceIdMgr {
	public final static int UNIQUEID = 1;

	@Id
	private final int id;

	private int maxAllocatable;

	private int nbAllocated;

	PInstanceIdMgr() {
		id = 0;
	}

	public PInstanceIdMgr(int max) {
		this.id = UNIQUEID;
		this.maxAllocatable = max;
		this.nbAllocated = 0;
	}

	public void incNbAllocated() {
		this.nbAllocated++;
	}

	public void decNbAllocated() {
		this.nbAllocated--;
	}

	public int getNbAllocated() {
		return this.nbAllocated;
	}
}
