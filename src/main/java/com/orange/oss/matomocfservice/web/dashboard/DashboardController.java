/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.web.dashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.orange.oss.matomocfservice.cfmgr.MatomoReleases;

/**
 * @author P. Déchamboux
 *
 */
@Controller
public class DashboardController {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	@Autowired
	MatomoReleases matomoReleases;

	@RequestMapping(value = { "/", "/index" }, method = RequestMethod.GET)
    public String index(Model model) {
		LOGGER.debug("DASHBOARD::index");
		model.addAttribute("defRelease", matomoReleases.getDefaultReleaseName());
		model.addAttribute("releases", matomoReleases.getReleaseList());
        return "index";
    }

//	@RequestMapping(value = { ApplicationConfiguration.DASHBOARD_PATH + "/{instid}" }, method = RequestMethod.GET)
//    public String dashboard(Model model, @PathVariable("instid") String instid) {
//		LOGGER.debug("DASHBOARD::dashboard: instid={}", instid);
//        model.addAttribute("message", "ça marche - instid=" + instid);
//        return "dashboard";
//    }
}
