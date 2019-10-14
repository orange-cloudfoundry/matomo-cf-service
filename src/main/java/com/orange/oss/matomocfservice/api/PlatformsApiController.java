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
import com.orange.oss.matomocfservice.web.service.MatomoInstanceService;
import com.orange.oss.matomocfservice.config.ApplicationConfiguration;
import com.orange.oss.matomocfservice.web.service.PlatformService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import javax.validation.constraints.*;
import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2019-10-07T11:08:00.231+02:00[Europe/Paris]")

@Api(tags = {"Platforms and Matomo Instances Admin"})
@RestController
@RequestMapping(value=ApplicationConfiguration.ADMIN_API_PATH)
//@CrossOrigin
public class PlatformsApiController implements PlatformsApi {
    private static final Logger log = LoggerFactory.getLogger(PlatformsApiController.class);
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;
	@Autowired
	private PlatformService platformService;
	@Autowired
	private MatomoInstanceService matomoInstanceService;

    @org.springframework.beans.factory.annotation.Autowired
    public PlatformsApiController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }

    public ResponseEntity<Void> matomoInstanceDelete(@ApiParam(value = "",required=true) @PathVariable("platformId") String platformId,@ApiParam(value = "",required=true) @PathVariable("matomoInstanceId") String matomoInstanceId) {
    	log.debug("API::matomoInstanceDelete");
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
            	matomoInstanceService.deleteMatomoInstance(platformId, matomoInstanceId);
            	return new ResponseEntity<Void>(HttpStatus.OK);
            } catch (Exception e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        log.error("Malformed request: need application/json");
        return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<List<MatomoInstance>> matomoInstanceFind(@ApiParam(value = "",required=true) @PathVariable("platformId") String platformId) {
    	log.debug("API::matomoInstanceFind");
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
            	return new ResponseEntity<List<MatomoInstance>>(matomoInstanceService.findMatomoInstance(platformId), HttpStatus.OK);
            } catch (Exception e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<List<MatomoInstance>>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        log.error("Malformed request: need application/json");
        return new ResponseEntity<List<MatomoInstance>>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<MatomoInstance> matomoInstanceGet(@ApiParam(value = "",required=true) @PathVariable("platformId") String platformId,@ApiParam(value = "",required=true) @PathVariable("matomoInstanceId") String matomoInstanceId) {
    	log.debug("API::matomoInstanceGet");
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<MatomoInstance>(matomoInstanceService.getMatomoInstance(platformId, matomoInstanceId), HttpStatus.OK);
            } catch (Exception e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<MatomoInstance>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        log.error("Malformed request: need application/json");
        return new ResponseEntity<MatomoInstance>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<MatomoInstance> matomoInstanceUpdate(@ApiParam(value = "" ,required=true )  @Valid @RequestBody MatomoInstance body,@ApiParam(value = "",required=true) @PathVariable("platformId") String platformId,@ApiParam(value = "",required=true) @PathVariable("matomoInstanceId") String matomoInstanceId) {
    	log.debug("API::matomoInstanceUpdate");
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                // TODO
                return new ResponseEntity<MatomoInstance>(HttpStatus.NOT_IMPLEMENTED);
            } catch (Exception e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<MatomoInstance>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        log.error("Malformed request: need application/json");
        return new ResponseEntity<MatomoInstance>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<Platform> platformCreate(@ApiParam(value = "" ,required=true )  @Valid @RequestBody Platform body) {
    	log.debug("API::platformCreate");
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<Platform>(platformService.createPlatform(body), HttpStatus.OK);
            } catch (Exception e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<Platform>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        log.error("Malformed request: need application/json");
        return new ResponseEntity<Platform>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<Void> platformDelete(@ApiParam(value = "",required=true) @PathVariable("platformId") String platformId) {
    	log.debug("API::platformDelete");
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                platformService.deletePlatform(platformId);
                return new ResponseEntity<Void>(HttpStatus.OK);
            } catch (Exception e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        log.error("Malformed request: need application/json");
        return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<List<Platform>> platformFind() {
    	log.debug("API::platformFind");
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
            	List<Platform> res = platformService.findPlatform();
                return new ResponseEntity<List<Platform>>(res, HttpStatus.OK);
            } catch (Exception e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<List<Platform>>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        log.error("Malformed request: need application/json");
        return new ResponseEntity<List<Platform>>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<Platform> platformGet(@ApiParam(value = "",required=true) @PathVariable("platformId") String platformId) {
    	log.debug("API::platformGet");
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
            	return new ResponseEntity<Platform>(platformService.getPlatform(platformId), HttpStatus.OK);
            } catch (Exception e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<Platform>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        log.error("Malformed request: need application/json");
        return new ResponseEntity<Platform>(HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<Platform> platformUpdate(@ApiParam(value = "" ,required=true )  @Valid @RequestBody Platform body,@ApiParam(value = "",required=true) @PathVariable("platformId") String platformId) {
    	log.debug("API::platformUpdate");
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
            	return new ResponseEntity<Platform>(platformService.updatePlatform(platformId, body), HttpStatus.OK);
            } catch (Exception e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<Platform>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        log.error("Malformed request: need application/json");
        return new ResponseEntity<Platform>(HttpStatus.BAD_REQUEST);
    }

}
