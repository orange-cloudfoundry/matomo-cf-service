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

package com.orange.oss.matomocfservice.api;

import com.orange.oss.matomocfservice.api.model.Error;
import com.orange.oss.matomocfservice.api.model.MatomoInstance;
import com.orange.oss.matomocfservice.api.model.Platform;
import com.orange.oss.matomocfservice.config.ApplicationConfiguration;
import com.orange.oss.matomocfservice.web.service.MatomoInstanceService;
import com.orange.oss.matomocfservice.web.service.PlatformService;

import io.swagger.annotations.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import javax.validation.constraints.*;
import javax.validation.Valid;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-06-27T12:09:14.874+02:00")

@Api(tags = {"Platforms and Matomo Instances Admin"})
@RestController
@RequestMapping(value=ApplicationConfiguration.ADMIN_API_PATH)
@CrossOrigin
public class PlatformsApiController implements PlatformsApi {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private PlatformService platformService;
	@Autowired
	private MatomoInstanceService matomoInstanceService;

    public ResponseEntity<Void> matomoInstanceDelete(
    		@ApiParam(value = "", required=true)
    		@PathVariable("platformId")
    		String platformId,
    		@ApiParam(value = "", required=true)
    		@PathVariable("matomoInstanceId")
    		String instanceId) {
    	LOGGER.debug("API::matomoInstanceDelete");
    	matomoInstanceService.deleteMatomoInstance(platformId, instanceId);
    	return new ResponseEntity<Void>(HttpStatus.OK);
    }

    public ResponseEntity<List<MatomoInstance>> matomoInstanceFind(
    		@ApiParam(value = "", required=true)
    		@PathVariable("platformId")
    		String platformId) {
    	LOGGER.debug("API::matomoInstanceFind");
    	List<MatomoInstance> res = matomoInstanceService.findMatomoInstance(platformId);
        return new ResponseEntity<List<MatomoInstance>>(res, HttpStatus.OK);
    }

    public ResponseEntity<MatomoInstance> matomoInstanceGet(
    		@ApiParam(value = "", required=true)
    		@PathVariable("platformId")
    		String platformId,
    		@ApiParam(value = "", required=true)
    		@PathVariable("matomoInstanceId")
    		String matomoInstanceId) {
    	LOGGER.debug("API::matomoInstanceGet");
    	MatomoInstance res = matomoInstanceService.getMatomoInstance(platformId, matomoInstanceId);
        return new ResponseEntity<MatomoInstance>(res, HttpStatus.OK);
    }

    public ResponseEntity<MatomoInstance> matomoInstanceUpdate(
    		@ApiParam(value = "", required=true)
    		@PathVariable("platformId")
    		String platformId,
    		@ApiParam(value = "", required=true)
    		@Valid
    		@RequestBody
    		MatomoInstance matomoInstance,
    		@ApiParam(value = "", required=true)
    		@PathVariable("matomoInstanceId")
    		String matomoInstanceId) {
    	LOGGER.debug("API::matomoInstanceUpdate");
        // TODO: do some magic!
        return new ResponseEntity<MatomoInstance>(HttpStatus.OK);
    }

    public ResponseEntity<Platform> platformCreate(
    		@ApiParam(value = "", required=true)
    		@Valid
    		@RequestBody
    		Platform platform) {
    	LOGGER.debug("API::platformCreate");
    	Platform res = platformService.createPlatform(platform);
        return new ResponseEntity<Platform>(res, HttpStatus.OK);
    }

    public ResponseEntity<Void> platformDelete(
    		@ApiParam(value = "", required=true)
    		@PathVariable("platformId")
    		String platformId) {
    	LOGGER.debug("API::platformDelete");
        platformService.deletePlatform(platformId);
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    public ResponseEntity<List<Platform>> platformFind() {
    	LOGGER.debug("API::platformFind");
    	List<Platform> res = platformService.findPlatform();
        return new ResponseEntity<List<Platform>>(res, HttpStatus.OK);
    }

    public ResponseEntity<Platform> platformGet(
    		@ApiParam(value = "", required=true)
    		@PathVariable("platformId")
    		String platformId) {
    	LOGGER.debug("API::platformGet");
    	Platform res = platformService.getPlatform(platformId);
        return new ResponseEntity<Platform>(res, HttpStatus.OK);
    }

    public ResponseEntity<Platform> platformUpdate(
    		@ApiParam(value = "", required=true)
    		@Valid
    		@RequestBody
    		Platform platform,
    		@ApiParam(value = "", required=true)
    		@PathVariable("platformId")
    		String platformId) {
    	LOGGER.debug("API::platformUpdate");
    	Platform res = platformService.updatePlatform(platformId, platform);
        return new ResponseEntity<Platform>(res, HttpStatus.OK);
    }
}
