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
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

/**
 * @author P. Déchamboux
 *
 */
public class McfsAuthenticationSuccessHandler
	extends SimpleUrlAuthenticationSuccessHandler 
	implements AuthenticationSuccessHandler {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	private int adminSessionTimeout;

	McfsAuthenticationSuccessHandler(int adminSessionTimeout) {
        super();
        setUseReferer(true);
		this.adminSessionTimeout = adminSessionTimeout;
	}

	@Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
		LOGGER.debug("SECU:: authentication OK");
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
        LOGGER.debug("SECU:: roles={}", roles.toString());
        if (roles.contains(SecurityConfig.PREFIXED_ROLE_ADMIN)) {
        	LOGGER.debug("SECU::security: timeout for admin session={}", adminSessionTimeout);
            request.getSession(false).setMaxInactiveInterval(adminSessionTimeout);
        } else {
        	LOGGER.debug("SECU::security: no timeout for the authenticated session");
            request.getSession(false).setMaxInactiveInterval(0);
        }
        // Currently login success url="/swagger-ui.html"
        LOGGER.debug("SECU::security: URL after authentication={}", request.getContextPath() + "/swagger-ui.html");
        response.sendRedirect(request.getContextPath() + "/swagger-ui.html");
    }
}
