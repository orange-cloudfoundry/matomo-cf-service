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

package com.orange.oss.matomocfservice.web.security;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

/**
 * @author P. Déchamboux
 *
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	public static final int MAX_SESSIONS = 100;
	public static final String PREFIXED_ROLE_ADMIN = "ROLE_ADMIN";
	public static final String ROLE_ADMIN = "ADMIN";
	public static final String REALM_NAME = "MatomoCfService_Realm";
	@Value("${matomo-service.security.adminName}")
	private String adminName;
	@Value("${matomo-service.security.adminPassword}")
	private String adminPassword;

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		LOGGER.debug("CONFIG::security: configureGlobal");
		auth.inMemoryAuthentication()
		.withUser(adminName).password("{noop}" + adminPassword).roles(ROLE_ADMIN)
		;
	}

	public static class McfsLogoutSuccessHandler implements LogoutSuccessHandler {
		private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

		@Override
		public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
				Authentication authentication) throws IOException, ServletException {
			LOGGER.debug("CONFIG::security: logout!!");
	        // Currently logout success url="/"
			response.sendRedirect(request.getContextPath());
		}
	}

    @PostConstruct
    public void afterInitialize() {
    	LOGGER.debug("CONFIG::security: properties 1 - adminName={}, adminPassword={}", adminName, adminPassword);
    }
}
