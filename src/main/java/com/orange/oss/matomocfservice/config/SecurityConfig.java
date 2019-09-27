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

package com.orange.oss.matomocfservice.config;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

/**
 * @author P. Déchamboux
 *
 */
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	private static final String ROLE_ADMIN = "ADMIN";
	public static final String REALM_NAME = "MatomoCfService_Realm";
	@Value("${matomo-service.security.adminName}")
	private String adminName;
	@Value("${matomo-service.security.adminPassword}")
	private String adminPassword;

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		LOGGER.debug("CONFIG::security: configureGlobal - adminName={}, adminPassword={}", adminName, adminPassword);
		auth.inMemoryAuthentication()
		.withUser(adminName).password("{noop}" + adminPassword).roles(ROLE_ADMIN)
		;
	}

	@Configuration
	@Order(1)
	public static class MatomoServiceSecurity1 extends WebSecurityConfigurerAdapter {
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http
			.antMatcher("/admin/**")
			.antMatcher("/swagger-ui.html")
			.authorizeRequests()
			.anyRequest().hasRole(ROLE_ADMIN)
			.and()
			.httpBasic()
			.realmName(REALM_NAME).authenticationEntryPoint(new CustomAuthenticationEntryPoint())
			.and()
			.csrf().disable();
		}
	}

	@Configuration
	@Order(2)
	public static class MatomoServiceSecurity2 extends WebSecurityConfigurerAdapter {
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http
			.authorizeRequests()
			.antMatchers("/").permitAll()
			.antMatchers("/img/**").permitAll()
			.antMatchers("/css/**").permitAll()
			.antMatchers("/index.html").permitAll()
			.antMatchers("/releases.html").permitAll()
			.anyRequest().authenticated()
			.and()
			.httpBasic()
			.realmName(REALM_NAME).authenticationEntryPoint(new CustomAuthenticationEntryPoint())
			.and()
			.csrf().disable();
		}
	}

	public static class CustomAuthenticationEntryPoint extends BasicAuthenticationEntryPoint {
		@Override
		public void commence(HttpServletRequest request,
				HttpServletResponse response,
				AuthenticationException authException) throws IOException, ServletException {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setHeader("WWW-Authenticate", "Basic realm=" + getRealmName());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			PrintWriter writer = response.getWriter();
			writer.println("HTTP Status 401: " + authException.getMessage());
		}
		@Override
		public void afterPropertiesSet() {
			setRealmName(SecurityConfig.REALM_NAME);
			super.afterPropertiesSet();
		}
	}
}
