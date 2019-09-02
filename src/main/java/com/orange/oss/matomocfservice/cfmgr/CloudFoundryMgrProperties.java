/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.cfmgr;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author P. Déchamboux
 *
 */
@Configuration
@PropertySource(name = "MatomoServiceProperties", value = "classpath:application.yml")
public class CloudFoundryMgrProperties {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	@Value("${matomo-service.service-domain}")
	private String serviceDomain;
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
