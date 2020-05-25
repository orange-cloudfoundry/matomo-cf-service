/**
 * Copyright 2020 Orange and the original author or authors.
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
package com.orange.oss.matomocfservice.web.service;

/**
 * @author P. Déchamboux
 *
 */
public class MatomoReleaseSpec {
	public String name;
	public boolean isDefault = false;
	public boolean isLatest = false;

	MatomoReleaseSpec(String n) {
		name = n;
	}

	public String getName() {
		return name;
	}

	public boolean  isDefault() {
		return isDefault;
	}

	public boolean  isLatest() {
		return isLatest;
	}

	MatomoReleaseSpec defaultRel() {
		isDefault = true;
		return this;
	}

	MatomoReleaseSpec latestRel() {
		isLatest = true;
		return this;
	}
}
