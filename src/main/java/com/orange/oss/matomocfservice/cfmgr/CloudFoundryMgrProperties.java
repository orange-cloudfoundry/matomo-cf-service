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

package com.orange.oss.matomocfservice.cfmgr;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author P. Déchamboux
 *
 */
@Configuration
public class CloudFoundryMgrProperties {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	@Value("${matomo-service.domain}")
	private String serviceDomain;
	@Value("${matomo-service.phpBuildpack}")
	private String servicePhpBuildpack;
	@Value("${matomo-service.max-service-instances}")
	private int maxServiceInstances;
	@Value("${matomo-service.shared-db.service-name}")
	private String sharedDbServiceName;
	@Value("${matomo-service.shared-db.plan-name}")
	private String sharedDbPlanName;
	@Value("${matomo-service.dedicated-db.service-name}")
	private String dedicatedDbServiceName;
	@Value("${matomo-service.dedicated-db.plan-name}")
	private String dedicatedDbPlanName;

	public String getDomain() {
		return serviceDomain;
	}

	public String getPhpBuildpack() {
		return servicePhpBuildpack;
	}

	public int getMaxServiceInstances() {
		return maxServiceInstances;
	}

	public String getSharedDbServiceName() {
		return sharedDbServiceName;
	}

	public String getSharedDbPlanName() {
		return sharedDbPlanName;
	}

	public String getDedicatedDbServiceName() {
		return dedicatedDbServiceName;
	}

	public String getDedicatedDbPlanName() {
		return dedicatedDbPlanName;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("{service-domain: \"");
		sb.append(serviceDomain);
		sb.append("\", shared-db: {service-name: \"");
		sb.append(sharedDbServiceName);
		sb.append("\", plan-name: \"");
		sb.append(sharedDbPlanName);
		sb.append("\"}, dedicated-db: {service-name: \"");
		sb.append(dedicatedDbServiceName);
		sb.append("\", plan-name: \"");
		sb.append(dedicatedDbPlanName);
		sb.append("\"}}");
		return sb.toString();
	}

    @PostConstruct
    public void afterInitialize() {
    	LOGGER.debug("CONFIG::properties: " + this.toString());
    }
}
