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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

/**
 * @author P. Déchamboux
 *
 */
@Entity
@Table(name = "platforms")
public class PPlatform {
	@Id
	@Column(length = 36)
	private final String id;

	@Column(length = 64)
	private final String name;

	@Column(length = 256)
	private String description;

	@Column(length = 128)
	private final ZonedDateTime createTime;

	@Column(length = 128)
	private ZonedDateTime updateTime;

	@SuppressWarnings("unused")
	protected PPlatform() {
		this.id = null;
		this.name = null;
		this.description = null;
		this.createTime = null;
		this.updateTime = null;
	}

	public PPlatform(String id, String n, String d) {
		this.id = id;
		this.name = n;
		this.description = d;
		this.createTime = ZonedDateTime.now();
		this.updateTime = this.createTime;
	}

	public String getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return this.description;
	}

	public ZonedDateTime getCreateTime() {
		return this.createTime;
	}

	public ZonedDateTime getUpdateTime() {
		return this.createTime;
	}
}
