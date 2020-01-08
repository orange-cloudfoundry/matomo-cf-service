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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.web.util.UriComponentsBuilder;

import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgr;
import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgrImpl;
import com.orange.oss.matomocfservice.web.service.ApplicationInformation;
import com.orange.oss.matomocfservice.web.service.InstanceIdMgr;
import com.orange.oss.matomocfservice.web.service.MatomoInstanceService;
import com.orange.oss.matomocfservice.web.service.MatomoReleases;
import com.orange.oss.matomocfservice.web.service.PlatformService;

/**
 * @author P. Déchamboux
 *
 */
@Configuration
public class ApplicationConfiguration {
	private final static Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);
	@Autowired
	MatomoInstanceService matomoInstanceService;
	@Autowired
	PlatformService platformService;
	@Autowired
	MatomoReleases matomoReleases;
	@Autowired
	CloudFoundryMgr cfMgr;
	@Autowired
	InstanceIdMgr instanceIdMgr;
	private ApplicationInformation applicationInformation;

	@Bean
	@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
	public ApplicationInformation cloudFoundryApplicationInformation(Environment environment) {
		LOGGER.debug("CONFIG - run in Cloud Foundry");
		String uri = environment.getProperty("vcap.application.uris[0]");
		String baseUrl = UriComponentsBuilder.newInstance()
				.scheme("https")
				.host(uri)
				.build()
				.toUriString();
		applicationInformation = new ApplicationInformation(baseUrl);
		return applicationInformation;
	}

	@Bean
	@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
	public CloudFoundryMgr cloudFoundryMgr() {
		LOGGER.debug("CONFIG - run in Cloud Foundry");
		return new CloudFoundryMgrImpl();
	}

	@Bean
	@ConditionalOnMissingBean(ApplicationInformation.class)
	public ApplicationInformation defaultApplicationInformation() {
		LOGGER.debug("CONFIG - run in local mode");
		String baseUrl = UriComponentsBuilder.newInstance()
				.scheme("http")
				.host("localhost")
				.port(8080)
				.build()
				.toUriString();
		applicationInformation = new ApplicationInformation(baseUrl);
		return applicationInformation;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void initializeAfterStartup() {
		LOGGER.debug("CONFIG - run initialization code after application startup has completed");
		matomoReleases.initialize();
		cfMgr.initialize();
		instanceIdMgr.initialize();
		platformService.initialize();
		matomoInstanceService.initialize();
	}
}
