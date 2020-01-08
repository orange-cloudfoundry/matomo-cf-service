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

package com.orange.oss.matomocfservice.servicebroker;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author P. Déchamboux
 *
 */
@Configuration
public class ServiceCatalogConfiguration {
	private final static Logger LOGGER = LoggerFactory.getLogger(ServiceCatalogConfiguration.class);
	public final static String PLANGLOBSHARDB_NAME = "global-shared-db";
	public final static String PLANGLOBSHARDB_UUID = "2f95934e-23c5-4f73-bdd3-f3383febb59a";
	public final static String PLANMATOMOSHARDB_NAME = "matomo-shared-db";
	public final static String PLANMATOMOSHARDB_UUID = "c0be2cce-aa25-4368-830f-cd671083723f";
	public final static String PLANDEDICATEDDB_NAME = "dedicated-db";
	public final static String PLANDEDICATEDDB_UUID = "44ea64fc-c7fb-49b9-bbef-d12a928c7613";

	@Bean
	public Catalog catalog() {
		LOGGER.debug("CONFIG - retrieve OSB catalog information.");
		String env_vcapApp = System.getenv("VCAP_APPLICATION");
		String serviceName = "matomo-service";
		String serviceUri = "none";
		if (env_vcapApp != null) {
			JSONObject jres = new JSONObject(env_vcapApp);
			serviceName = jres.getString("name");
			serviceUri = jres.getJSONArray("uris").getString(0);
		}
		Plan plan1 = Plan.builder()
				.name(PLANGLOBSHARDB_NAME)
				.id(PLANGLOBSHARDB_UUID.toString())
				.description("\"Matomo Service\" provided as a dedicated CF application with data store in global shared DB service")
				.free(false)
				.build();
		Plan plan2 = Plan.builder()
				.name(PLANMATOMOSHARDB_NAME)
				.id(PLANMATOMOSHARDB_UUID.toString())
				.description("\"Matomo Service\" provided as a dedicated CF application with data stored in a dedicated DB shared by these Matomo instances")
				.free(false)
				.build();
		Plan plan3 = Plan.builder()
				.name(PLANDEDICATEDDB_NAME)
				.id(PLANDEDICATEDDB_UUID.toString())
				.description("\"Matomo Service\" provided as a dedicated CF application with data stored in a DB dedicated to this instance")
				.free(false)
				.build();
		Manifest manifest = getManifest();
		String version = manifest == null ? "Unknown" : (String)manifest.getMainAttributes().getValue("Implementation-Version");
		ServiceDefinition serviceDefinition = ServiceDefinition.builder()
				.id("matomo-cf-service")
				.name(serviceName)
				.description("CloudFoundry-based \"Matomo as a Service\" (experimental / no SLA)")
				.bindable(true)
				.tags("Matomo", "Web Analytics")
				.plans(plan1, plan2, plan3)
				.metadata("displayName", "Matomo Service - Version " + version)
				.metadata("longDescription", "CloudFoundry-based Matomo as a Service")
				.metadata("providerDisplayName", "Orange")
				.metadata("documentationUrl", "https://" + serviceUri + "/index.html")
//				.metadata("supportUrl", "")
				.build();
		return Catalog.builder()
				.serviceDefinitions(serviceDefinition)
				.build();
	}

	public static Manifest getManifest() {
	    try {
	    	Enumeration<URL> resEnum = Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF");
	        while (resEnum.hasMoreElements()) {
	            try {
	                URL url = resEnum.nextElement();
	                if (url.toString().equals("file:/home/vcap/app/META-INF/MANIFEST.MF")) {
	                    InputStream is = url.openStream();
	                    if (is != null) {
	                        Manifest manifest = new Manifest(is);
	                        return manifest;
	                    }
	                }
	            }
	            catch (Exception e) {
	                // Silently ignore wrong manifests on classpath?
	            }
	        }
	    } catch (IOException e1) {
	        // Silently ignore wrong manifests on classpath?
	    }
	    return null;
	}
}
