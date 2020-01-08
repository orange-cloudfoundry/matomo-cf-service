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

package com.orange.oss.matomocfservice;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.orange.oss.matomocfservice.servicebroker.ServiceCatalogConfiguration;

/**
 * @author P. Déchamboux
 *
 */
@SpringBootApplication
public class MatomoCfServiceApplication {
	private final static Logger LOGGER = LoggerFactory.getLogger(MatomoCfServiceApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(MatomoCfServiceApplication.class, args);
		Manifest manifest = ServiceCatalogConfiguration.getManifest();
		if (manifest != null) {
			String title = (String)manifest.getMainAttributes().getValue("Implementation-Title");
			String version = (String)manifest.getMainAttributes().getValue("Implementation-Version");
			LOGGER.info("--------------------- Starting {} version {} ---------------------", title, version);
		} else {
			LOGGER.info("--------------------- Starting matomo-cf-service version unknown ---------------------");
		}
	}
}
