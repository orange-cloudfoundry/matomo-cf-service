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
