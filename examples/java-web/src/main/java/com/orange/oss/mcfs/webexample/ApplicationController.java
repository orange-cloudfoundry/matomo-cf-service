package com.orange.oss.mcfs.webexample;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class ApplicationController {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	private Mcfs mcfs = null;

	@RequestMapping(value = { "/index.html", "/" }, method = RequestMethod.GET)
    public String index() {
		LOGGER.debug("CONTROLLER: index.html");
        return "index.html";
    }

	@RequestMapping(value = { "/otherpage.html" }, method = RequestMethod.GET)
    public String releases() {
		LOGGER.debug("CONTROLLER: otherpage.html");
        return "otherpage.html";
    }

	@ModelAttribute("mcfs")
	private Mcfs mcfs() {
		LOGGER.debug("CONTROLLER: feed model with matomo credentials");
		if (mcfs == null) {
			mcfs = new Mcfs();
			LOGGER.debug("CONTROLLER: initialize from VCAP");
			String VCAP_SERVICES = System.getenv("VCAP_SERVICES");
			if (VCAP_SERVICES != null) {
				JSONObject vcap_cred = new JSONObject(VCAP_SERVICES).getJSONArray("matomo-service").getJSONObject(0).getJSONObject("credentials");
				mcfs.siteId = Integer.toString(vcap_cred.getInt("mcfs-siteId"));
				mcfs.matomoUrl = vcap_cred.getString("mcfs-matomoUrl");
			}
		}
		LOGGER.debug("CONTROLLER: mcfs-siteId={}, mcfs-matomoUrl={}", mcfs.siteId, mcfs.matomoUrl);
		return mcfs;
	}

	public static class Mcfs {
		public String siteId;
		public String matomoUrl;
	}
}
