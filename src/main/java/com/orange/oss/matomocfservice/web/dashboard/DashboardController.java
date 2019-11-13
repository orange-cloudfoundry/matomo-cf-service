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

package com.orange.oss.matomocfservice.web.dashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.orange.oss.matomocfservice.cfmgr.MatomoReleases;

/**
 * @author P. Déchamboux
 *
 */
@Controller
public class DashboardController {
	private final static Logger LOGGER = LoggerFactory.getLogger(DashboardController.class);
	@Autowired
	MatomoReleases matomoReleases;

	@RequestMapping(value = { "/index.html", "/" }, method = RequestMethod.GET)
    public String index() {
		LOGGER.debug("DASHBOARD::index");
        return "index.html";
    }

	@RequestMapping(value = { "/releases.html" }, method = RequestMethod.GET)
    public String releases(Model model) {
		LOGGER.debug("DASHBOARD::releases");
		model.addAttribute("defRelease", matomoReleases.getDefaultReleaseName());
		model.addAttribute("releases", matomoReleases.getReleaseList());
        return "releases.html";
    }
}
