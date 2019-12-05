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
import javax.persistence.Table;

import io.jsonwebtoken.lang.Assert;

/**
 * @author P. Déchamboux
 *
 */
@Entity
@Table(name = "platforms")
public class PPlatform {
	private final static int LENGTH_ID = 36;
	private final static int LENGTH_NAME = 64;
	private final static int LENGTH_DESC = 256;
	private final static int LENGTH_TIME = 128;

	@Id
	@Column(length = LENGTH_ID)
	private final String id;

	@Column(length = LENGTH_NAME)
	private final String name;

	@Column(length = LENGTH_DESC)
	private String description;

	@Column(length = LENGTH_TIME)
	private final ZonedDateTime createTime;

	@Column(length = LENGTH_TIME)
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
		Assert.isTrue(id.length() <= LENGTH_ID, "Maximum length for id is " + LENGTH_ID);
		this.id = id;
		if (n.length() > LENGTH_NAME) {
			// truncate
			this.name = n.substring(0, LENGTH_NAME);
		} else {
			this.name = n;
		}
		if (d.length() > LENGTH_DESC) {
			// truncate
			this.description = d.substring(0, LENGTH_DESC);
		} else {
			this.description = d;
		}
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
