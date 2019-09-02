/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.orange.oss.matomocfservice.cfmgr.MatomoReleases;
import com.orange.oss.matomocfservice.cfmgr.MatomoReleases.MatomoReleaseSpec;

/**
 * @author P. Déchamboux
 *
 */
@Configuration
public class ServiceCatalogConfiguration {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	public final static String PLANGLOBSHARDB_NAME = "global-shared-db";
	public final static String PLANGLOBSHARDB_UUID = "2f95934e-23c5-4f73-bdd3-f3383febb59a";
	public final static String PLANMATOMOSHARDB_NAME = "matomo-shared-db";
	public final static String PLANMATOMOSHARDB_UUID = "c0be2cce-aa25-4368-830f-cd671083723f";
	public final static String PLANDEDICATEDDB_NAME = "dedicated-db";
	public final static String PLANDEDICATEDDB_UUID = "44ea64fc-c7fb-49b9-bbef-d12a928c7613";

	@Bean
	public Catalog catalog() {
		LOGGER.debug("CONFIG - retrieve OSB catalog information.");
		Plan plan1 = Plan.builder()
				.name(PLANGLOBSHARDB_NAME)
				.id(PLANGLOBSHARDB_UUID.toString())
				.description("\"Matomo Service\" provided as a dedicated CF application with data store in global shared DB service")
				.free(true)
				.build();
		Plan plan2 = Plan.builder()
				.name(PLANMATOMOSHARDB_NAME)
				.id(PLANMATOMOSHARDB_UUID.toString())
				.description("\"Matomo Service\" provided as a dedicated CF application with data stored in a dedicated DB shared by these Matomo instances")
				.free(true)
				.build();
		Plan plan3 = Plan.builder()
				.name(PLANDEDICATEDDB_NAME)
				.id(PLANDEDICATEDDB_UUID.toString())
				.description("\"Matomo Service\" provided as a dedicated CF application with data stored in a DB dedicated to this instance")
				.free(false)
				.build();
		ServiceDefinition serviceDefinition = ServiceDefinition.builder()
				.id("matomo-cf-service")
				.name("matomo")
				.description("CloudFoundry-based \"Matomo as a Service\"")
				.bindable(true)
				.tags("Matomo", "web analytics")
				.plans(plan1, plan2, plan3)
				.metadata("displayName", "Matomo")
				.metadata("longDescription", "Matomo as a Service")
				.metadata("providerDisplayName", "Orange")
				.build();
		return Catalog.builder()
				.serviceDefinitions(serviceDefinition)
				.build();
	}
}
