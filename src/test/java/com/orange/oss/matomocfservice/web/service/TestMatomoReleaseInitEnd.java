/**
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
package com.orange.oss.matomocfservice.web.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author P. Déchamboux
 *
 */
@SpringBootTest
public class TestMatomoReleaseInitEnd {
	@Autowired
	MatomoReleases matomoReleases;

	@Test
	void testInitEnd() {
		matomoReleases.initialize();
		Assertions.assertNotNull(matomoReleases.getDefaultReleaseName());
		Assertions.assertNotNull(matomoReleases.getLatestReleaseName());
		Assertions.assertNotNull(matomoReleases.getReleaseList());
		matomoReleases.onExit();
		Assertions.assertNull(matomoReleases.getDefaultReleaseName());
		Assertions.assertNull(matomoReleases.getLatestReleaseName());
		Assertions.assertNull(matomoReleases.getReleaseList());
		matomoReleases.initialize();
	}
}
