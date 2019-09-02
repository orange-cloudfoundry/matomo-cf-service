/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author P. Déchamboux
 *
 */
public class ApplicationInformation {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	private final String baseUrl;
	private final String releaseBaseUrl;

	public ApplicationInformation(String baseUrl) {
		LOGGER.debug("CONFIG - set app info / base URL=" + baseUrl);
		this.baseUrl = baseUrl;
		this.releaseBaseUrl = baseUrl.replaceFirst("matomoserv", "matomorel");
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public String getReleaseBaseUrl() {
		return releaseBaseUrl;
	}
}
