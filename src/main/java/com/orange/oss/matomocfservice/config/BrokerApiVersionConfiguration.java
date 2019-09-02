/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.config;

import org.springframework.cloud.servicebroker.model.BrokerApiVersion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author P. Déchamboux
 *
 */
@Configuration
public class BrokerApiVersionConfiguration {
	@Bean
	public BrokerApiVersion brokerApiVersion() {
		return new BrokerApiVersion("2.14");
	}
}
