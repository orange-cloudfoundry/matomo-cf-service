/**
 * Copyright 2020 Orange and the original author or authors.
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
package com.orange.oss.matomocfservice.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;

/**
 * @author P. Déchamboux
 *
 */
@Configuration
public class SecurityAuthorities {
	private final static Logger LOGGER = LoggerFactory.getLogger(SecurityAuthorities.class);
	public static final String ROLE_ADMIN = "ADMIN";
	public static final String ROLE_BROKER_USER = "BROKER_USER";
	public static final String ROLE_FULL_ACCESS = "FULL_ACCESS";
	public static final String ROLE_READ_ONLY = "READ_ONLY";
	@Value("${matomo-service.security.adminName}")
	private String adminName;
	@Value("${matomo-service.security.adminPassword}")
	private String adminPassword;

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		LOGGER.debug("CONFIG::security: configureGlobal");
		auth.inMemoryAuthentication()
		.withUser(adminName)
		.password(/*"{noop}" + */adminPassword)
		.roles(ROLE_ADMIN, ROLE_BROKER_USER);
	}
}
