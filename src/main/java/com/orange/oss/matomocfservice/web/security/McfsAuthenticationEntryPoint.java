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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

/**
 * @author P. Déchamboux
 *
 */
public class McfsAuthenticationEntryPoint extends BasicAuthenticationEntryPoint {
	private final static Logger LOGGER = LoggerFactory.getLogger(McfsAuthenticationEntryPoint.class);

//	@Override
//	public void commence(HttpServletRequest request,
//			HttpServletResponse response,
//			AuthenticationException authException) throws IOException, ServletException {
//		LOGGER.debug("SECU:: authent entry point: {}", request.toString());
//		LOGGER.debug("AuthType: {}", request.getAuthType());
//		LOGGER.debug("ContextPath: {}", request.getContextPath());
//		LOGGER.debug("CharacterEncoding: {}", request.getCharacterEncoding());
//		LOGGER.debug("LocalAddr: {}", request.getLocalAddr());
//		LOGGER.debug("LocalName: {}", request.getLocalName());
//		LOGGER.debug("LocalPort: {}", request.getLocalPort());
//		LOGGER.debug("Method: {}", request.getMethod());
//		Enumeration<String> headerNames = request.getHeaderNames();
//		while(headerNames.hasMoreElements()) {
//			String headerName = headerNames.nextElement();
//			LOGGER.debug("   Header Name - {}, Value - {}", headerName, request.getHeader(headerName));
//		}
//		Enumeration<String> params = request.getParameterNames(); 
//		while(params.hasMoreElements()){
//			String paramName = params.nextElement();
//			LOGGER.debug("   Parameter Name - {}, Value - {}", paramName, request.getParameter(paramName));
//		}
//		response.sendRedirect(request.getContextPath() + "/login");
//	}

	@Override
	public void afterPropertiesSet() throws Exception {
		LOGGER.debug("CONFIG::security: set realm name");
		setRealmName(SecurityConfig.REALM_NAME);
		super.afterPropertiesSet();
	}
}
