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

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import com.orange.oss.matomocfservice.web.security.SecurityConfig.McfsLogoutSuccessHandler;

/**
 * @author P. Déchamboux
 *
 */
@Configuration
@Order(1)
public class McfsSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
	private final static Logger LOGGER = LoggerFactory.getLogger(McfsSecurityConfigurerAdapter.class);
	@Value("${matomo-service.security.adminSessionTimeout:15}")
	private int adminSessionTimeout;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		LOGGER.debug("CONFIG::security: configure security for admin accesses");
		http
		.authorizeRequests()
			.antMatchers("/", "/img/**", "/css/**", "/index.html", "/releases.html", "/actuator/**", "/login").permitAll()
//			.anyRequest().hasRole(SecurityConfig.ROLE_ADMIN)
		.and().httpBasic()
			.realmName(SecurityConfig.REALM_NAME).authenticationEntryPoint(new McfsAuthenticationEntryPoint())
        .and().formLogin()
            .successHandler(new McfsAuthenticationSuccessHandler(adminSessionTimeout))
        .and().logout().permitAll()
            .logoutSuccessHandler(new McfsLogoutSuccessHandler())
            .invalidateHttpSession(true)
		.and().csrf().disable();
		http.sessionManagement().maximumSessions(SecurityConfig.MAX_SESSIONS).expiredUrl("/login?expired=true");
	}

    @PostConstruct
    public void afterInitialize() {
    	LOGGER.debug("CONFIG::security: properties 2 - adminSessionTimeout={}", adminSessionTimeout);
    }
}
