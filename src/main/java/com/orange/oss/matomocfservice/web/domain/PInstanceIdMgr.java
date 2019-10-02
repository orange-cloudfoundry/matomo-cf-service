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

	public void setMaxAllocatable(int max) {
		this.maxAllocatable = max;
	}

	public int getMaxAllocatable() {
		return this.maxAllocatable;
	}
}
