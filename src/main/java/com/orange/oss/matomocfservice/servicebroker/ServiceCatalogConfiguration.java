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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.MethodSchema;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.Schemas;
import org.springframework.cloud.servicebroker.model.catalog.ServiceBindingSchema;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.catalog.ServiceInstanceSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.orange.oss.matomocfservice.web.domain.Parameters;
import com.orange.oss.matomocfservice.web.service.BindingService;
import com.orange.oss.matomocfservice.web.service.MatomoReleaseSpec;
import com.orange.oss.matomocfservice.web.service.MatomoReleases;

/**
 * @author P. Déchamboux
 *
 */
@Configuration
public class ServiceCatalogConfiguration {
	private final static Logger LOGGER = LoggerFactory.getLogger(ServiceCatalogConfiguration.class);
	public final static String PLANSHARED_NAME = "matomo-shared";
	public final static String PLANSHARED_UUID = "b3e944b0-8f83-11ea-92bf-2f4ff84fde77";
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
		String serviceUri = "localhost"; // set "localhost" as default URI
		String reqBindParams[] = {
				BindingService.PARAM_SITENAME,
				BindingService.PARAM_TRACKEDURL,
				BindingService.PARAM_ADMINEMAIL
				};
		String polEnum[] = {Parameters.UPGRADE_EXPLICIT, Parameters.UPGRADE_AUTOMATIC};
		List<MatomoReleaseSpec> lmrs = MatomoReleases.getReleaseList();
		String relEnum[] = new String[lmrs.size()];
		int i = 0;
		for (MatomoReleaseSpec mrs : lmrs) {
			relEnum[i++] = mrs.getName();
		}
		if (env_vcapApp != null) {
			JSONObject jres = new JSONObject(env_vcapApp);
			serviceName = jres.getString("name");
			serviceUri = jres.getJSONArray("uris").getString(0);
		}
		Map<String,Map<String,Object>> props = new HashMap<String,Map<String,Object>>();
		Map<String,Object> param_props = new HashMap<String,Object>();
		param_props.put("description", "Matomo version to be instanciated");
		param_props.put("type", "string");
		param_props.put("enum", relEnum);
		props.put(MatomoServiceInstanceService.PARAM_VERSION, param_props);
		param_props = new HashMap<String,Object>();
		param_props.put("description", "Time Zone within which to run this Matomo instance (e.g. \"Europe/Paris\")");
		param_props.put("type", "string");
		props.put(MatomoServiceInstanceService.PARAM_TZ, param_props);
		param_props = new HashMap<String,Object>();
		param_props.put("description", "Number of containers to run in parallel (in 1-" + MatomoServiceInstanceService.MAX_INSTANCES + " range)");
		param_props.put("type", "integer");
		param_props.put("minimum", 1);
		param_props.put("maximum", MatomoServiceInstanceService.MAX_INSTANCES);
		props.put(MatomoServiceInstanceService.PARAM_INSTANCES, param_props);
		param_props = new HashMap<String,Object>();
		param_props.put("description", "Upgrade policy to move to the newest available version");
		param_props.put("type", "string");
		param_props.put("enum", polEnum);
		props.put(MatomoServiceInstanceService.PARAM_VERSIONUPGRADEPOLICY, param_props);
		param_props = new HashMap<String,Object>();
		param_props.put("description", "Size in MB of containers to be run (in " + Parameters.MINMEMORYSIZE + "-" + Parameters.MAXMEMORYSIZE + " range)");
		param_props.put("type", "integer");
		param_props.put("minimum", Parameters.MINMEMORYSIZE);
		param_props.put("maximum", Parameters.MAXMEMORYSIZE);
		param_props.put("multipleOf", Parameters.INCMEMORYSIZE);
		props.put(MatomoServiceInstanceService.PARAM_INSTANCES, param_props);
		MethodSchema methodSchema = MethodSchema.builder()
				.parameters("$schema", "http://json-schema.org/draft-04/schema#")
				.parameters("type", "object")
				.parameters("properties", props)
				.build();
		ServiceInstanceSchema serviceInstanceSchema = ServiceInstanceSchema.builder()
				.createMethodSchema(methodSchema)
				.updateMethodSchema(methodSchema)
				.build();
		props = new HashMap<String,Map<String,Object>>();
		param_props = new HashMap<String,Object>();
		param_props.put("description", "Name of the Matomo site created for tracking (e.g. \"MySite\")");
		param_props.put("type", "string");
		props.put(BindingService.PARAM_SITENAME, param_props);
		param_props = new HashMap<String,Object>();
		param_props.put("description", "URL of the Web site to be tracked (e.g. \"www.mysite.org\")");
		param_props.put("type", "string");
		props.put(BindingService.PARAM_TRACKEDURL, param_props);
		param_props = new HashMap<String,Object>();
		param_props.put("description", "Email of the admin of this Matomo site (e.g. \"john.smith@mycompany.com\")");
		param_props.put("type", "string");
		props.put(BindingService.PARAM_ADMINEMAIL, param_props);
		methodSchema = MethodSchema.builder()
				.parameters("$schema", "http://json-schema.org/draft-04/schema#")
				.parameters("type", "object")
				.parameters("properties", props)
				.parameters("required", reqBindParams)
				.build();
		ServiceBindingSchema serviceBindingSchema = ServiceBindingSchema.builder()
				.createMethodSchema(methodSchema)
				.build();
		Schemas schemas = Schemas.builder()
				.serviceInstanceSchema(serviceInstanceSchema)
				.serviceBindingSchema(serviceBindingSchema)
				.build();
		Plan plan1 = Plan.builder()
				.name(PLANGLOBSHARDB_NAME)
				.id(PLANGLOBSHARDB_UUID.toString())
				.schemas(schemas)
				.planUpdateable(true)
				.description("\"Matomo Service\" provided as a dedicated CF application with data store in global shared DB service")
				.free(false)
				.build();
		Plan plan2 = Plan.builder()
				.name(PLANMATOMOSHARDB_NAME)
				.id(PLANMATOMOSHARDB_UUID.toString())
				.schemas(schemas)
				.planUpdateable(true)
				.description("\"Matomo Service\" provided as a dedicated CF application with data stored in a dedicated DB shared by these Matomo instances")
				.free(false)
				.build();
		Plan plan3 = Plan.builder()
				.name(PLANDEDICATEDDB_NAME)
				.id(PLANDEDICATEDDB_UUID.toString())
				.schemas(schemas)
				.planUpdateable(true)
				.description("\"Matomo Service\" provided as a dedicated CF application with data stored in a DB dedicated to this instance")
				.free(false)
				.build();
		Plan plan4 = Plan.builder()
				.name(PLANSHARED_NAME)
				.id(PLANSHARED_UUID.toString())
				.schemas(schemas)
				.planUpdateable(true)
				.description("\"Matomo Service\" provided within a shared Matomo instance")
				.free(true)
				.build();
		Manifest manifest = getManifest();
		String version = manifest == null ? "Unknown" : (String)manifest.getMainAttributes().getValue("Implementation-Version");
		ServiceDefinition serviceDefinition = ServiceDefinition.builder()
				.id("matomo-cf-service")
				.name(serviceName)
				.description("CloudFoundry-based \"Matomo as a Service\" (experimental / no SLA)")
				.bindable(true)
				.tags("Matomo", "Web Analytics")
				.plans(plan1, plan2, plan3, plan4)
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
