/**
 * Orange File HEADER
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
