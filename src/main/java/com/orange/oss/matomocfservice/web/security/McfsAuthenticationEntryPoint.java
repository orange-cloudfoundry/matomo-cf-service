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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

/**
 * @author P. Déchamboux
 *
 */
public class McfsAuthenticationEntryPoint extends BasicAuthenticationEntryPoint {
	private final static Logger LOGGER = LoggerFactory.getLogger(McfsAuthenticationEntryPoint.class);

	@Override
	public void commence(HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {
		LOGGER.debug("SECU:: authent entry point");
		response.sendRedirect(request.getContextPath() + "/login");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		LOGGER.debug("CONFIG::security: set realm name");
		setRealmName(SecurityConfig.REALM_NAME);
		super.afterPropertiesSet();
	}
}
