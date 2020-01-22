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

import java.util.Objects;

/**
 * @author P. Déchamboux
 * 
 */
public class Parameters {
	private static String defaultVersion;
	private String version;
	private String timeZone = "Europe/Paris";
	private Boolean autoVersionUpgrade = true;
	private Integer cfInstances = 1;
	private Integer memorySize = 256;

	static public void setDefaultVersion(String vers) {
		defaultVersion = vers;
	}

	public Parameters() {
		version = defaultVersion;
	}

	public Parameters version(String version) {
		this.version = version;
		return this;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Parameters timeZone(String timeZone) {
		this.timeZone = timeZone;
		return this;
	}

	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public Parameters autoVersionUpgrade(Boolean autoVersionUpgrade) {
		this.autoVersionUpgrade = autoVersionUpgrade;
		return this;
	}

	public Boolean isAutoVersionUpgrade() {
		return autoVersionUpgrade;
	}

	public void setAutoVersionUpgrade(Boolean autoVersionUpgrade) {
		this.autoVersionUpgrade = autoVersionUpgrade;
	}

	public Parameters cfInstances(Integer cfInstances) {
		this.cfInstances = cfInstances;
		return this;
	}

	public Integer getCfInstances() {
		return cfInstances;
	}

	public void setCfInstances(Integer cfInstances) {
		this.cfInstances = cfInstances;
	}

	public Parameters memorySize(Integer memorySize) {
		this.memorySize = memorySize;
		return this;
	}

	public Integer getMemorySize() {
		return memorySize;
	}

	public void setMemorySize(Integer memorySize) {
		this.memorySize = memorySize;
	}

	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Parameters miParameters = (Parameters) o;
		return Objects.equals(this.version, miParameters.version)
				&& Objects.equals(this.timeZone, miParameters.timeZone)
				&& Objects.equals(this.autoVersionUpgrade, miParameters.autoVersionUpgrade)
				&& Objects.equals(this.cfInstances, miParameters.cfInstances)
				&& Objects.equals(this.memorySize, miParameters.memorySize);
	}

	@Override
	public int hashCode() {
		return Objects.hash(version, timeZone, autoVersionUpgrade, cfInstances, memorySize);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class PMatomoInstance.Parameters {\n");
		sb.append("    version: ").append(version).append("\n");
		sb.append("    timeZone: ").append(timeZone).append("\n");
		sb.append("    autoVersionUpgrade: ").append(autoVersionUpgrade).append("\n");
		sb.append("    cfInstances: ").append(cfInstances).append("\n");
		sb.append("    memorySize: ").append(memorySize).append("\n");
		sb.append("}");
		return sb.toString();
	}
}
