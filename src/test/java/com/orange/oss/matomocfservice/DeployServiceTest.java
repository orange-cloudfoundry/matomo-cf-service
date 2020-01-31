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
package com.orange.oss.matomocfservice;

import java.util.jar.Manifest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import com.orange.oss.matomocfservice.servicebroker.ServiceCatalogConfiguration;

/**
 * @author P. Déchamboux
 *
 */
@SpringBootTest
public class DeployServiceTest {
	private final static Logger LOGGER = LoggerFactory.getLogger(DeployServiceTest.class);

	@Test
	void startServiceInTestModeOK() {
		LOGGER.debug("startServiceInTestModeOK");
		Manifest manifest = ServiceCatalogConfiguration.getManifest();
		LOGGER.debug("Manifest: {}", manifest);
//		Assertions.assertNotNull(manifest);
//		Assertions.assertNotNull(manifest.getMainAttributes().getValue("Implementation-Title"));
//		Assertions.assertNotNull(manifest.getMainAttributes().getValue("Implementation-Version"));
	}
}
