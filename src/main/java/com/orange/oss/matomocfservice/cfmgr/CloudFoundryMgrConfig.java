/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.cfmgr;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import org.cloudfoundry.uaa.UaaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author P. Déchamboux
 *
 */
@Configuration
public class CloudFoundryMgrConfig {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

	@Bean
	DefaultConnectionContext connectionContext(@Value("${cf.apiHost}") String apiHost) {
		LOGGER.debug("CONFIG - define connectionContext");
	    return DefaultConnectionContext.builder()
	        .apiHost(apiHost)
	        .build();
	}

	@Bean
	PasswordGrantTokenProvider tokenProvider(@Value("${cf.username}") String username,
	                                         @Value("${cf.password}") String password) {
		LOGGER.debug("CONFIG - define tokenProvider");
	    return PasswordGrantTokenProvider.builder()
	        .password(password)
	        .username(username)
	        .build();
	}
	
	@Bean
	ReactorCloudFoundryClient cloudFoundryClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		LOGGER.debug("CONFIG - define cloudFoundryClient");
	    return ReactorCloudFoundryClient.builder()
	        .connectionContext(connectionContext)
	        .tokenProvider(tokenProvider)
	        .build();
	}
	
	@Bean
	ReactorDopplerClient dopplerClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		LOGGER.debug("CONFIG - define dopplerClient");
	    return ReactorDopplerClient.builder()
	        .connectionContext(connectionContext)
	        .tokenProvider(tokenProvider)
	        .build();
	}

	@Bean
	ReactorUaaClient uaaClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		LOGGER.debug("CONFIG - define uaaClient");
	    return ReactorUaaClient.builder()
	        .connectionContext(connectionContext)
	        .tokenProvider(tokenProvider)
	        .build();
	}
	
	@Bean
	DefaultCloudFoundryOperations cloudFoundryOperations(CloudFoundryClient cloudFoundryClient,
	                                                     DopplerClient dopplerClient,
	                                                     UaaClient uaaClient,
	                                                     @Value("${cf.organization}") String organization,
	                                                     @Value("${cf.space}") String space) {
		LOGGER.debug("CONFIG - define cloudFoundryOperations");
	    return DefaultCloudFoundryOperations.builder()
	            .cloudFoundryClient(cloudFoundryClient)
	            .dopplerClient(dopplerClient)
	            .uaaClient(uaaClient)
	            .organization(organization)
	            .space(space)
	            .build();
	}
}
