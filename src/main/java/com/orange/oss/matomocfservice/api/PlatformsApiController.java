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
import com.orange.oss.matomocfservice.web.service.PlatformService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.*;
import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2019-10-07T11:08:00.231+02:00[Europe/Paris]")
@Controller
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
        String accept = request.getHeader("Accept");
    	log.debug("API::matomoInstanceDelete");
    	matomoInstanceService.deleteMatomoInstance(platformId, matomoInstanceId);
    	return new ResponseEntity<Void>(HttpStatus.OK);
    }

    public ResponseEntity<List<MatomoInstance>> matomoInstanceFind(@ApiParam(value = "",required=true) @PathVariable("platformId") String platformId) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<List<MatomoInstance>>(objectMapper.readValue("[ {\r\n  \"matomoVersion\" : \"matomoVersion\",\r\n  \"dashboardUrl\" : \"dashboardUrl\",\r\n  \"platformKind\" : \"CLOUDFOUNDRY\",\r\n  \"serviceDefinitionId\" : \"serviceDefinitionId\",\r\n  \"updateTime\" : \"updateTime\",\r\n  \"platformId\" : \"platformId\",\r\n  \"uuid\" : \"uuid\",\r\n  \"lastOperationState\" : \"lastOperationState\",\r\n  \"platformApiLocation\" : \"platformApiLocation\",\r\n  \"createTime\" : \"createTime\",\r\n  \"lastOperation\" : \"CREATE\",\r\n  \"name\" : \"name\",\r\n  \"tenantId\" : \"tenantId\",\r\n  \"subtenantId\" : \"subtenantId\",\r\n  \"planId\" : \"planId\"\r\n}, {\r\n  \"matomoVersion\" : \"matomoVersion\",\r\n  \"dashboardUrl\" : \"dashboardUrl\",\r\n  \"platformKind\" : \"CLOUDFOUNDRY\",\r\n  \"serviceDefinitionId\" : \"serviceDefinitionId\",\r\n  \"updateTime\" : \"updateTime\",\r\n  \"platformId\" : \"platformId\",\r\n  \"uuid\" : \"uuid\",\r\n  \"lastOperationState\" : \"lastOperationState\",\r\n  \"platformApiLocation\" : \"platformApiLocation\",\r\n  \"createTime\" : \"createTime\",\r\n  \"lastOperation\" : \"CREATE\",\r\n  \"name\" : \"name\",\r\n  \"tenantId\" : \"tenantId\",\r\n  \"subtenantId\" : \"subtenantId\",\r\n  \"planId\" : \"planId\"\r\n} ]", List.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<List<MatomoInstance>>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    	log.debug("API::matomoInstanceFind");
    	List<MatomoInstance> res = matomoInstanceService.findMatomoInstance(platformId);
        return new ResponseEntity<List<MatomoInstance>>(res, HttpStatus.OK);
    }

    public ResponseEntity<MatomoInstance> matomoInstanceGet(@ApiParam(value = "",required=true) @PathVariable("platformId") String platformId,@ApiParam(value = "",required=true) @PathVariable("matomoInstanceId") String matomoInstanceId) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<MatomoInstance>(objectMapper.readValue("{\r\n  \"matomoVersion\" : \"matomoVersion\",\r\n  \"dashboardUrl\" : \"dashboardUrl\",\r\n  \"platformKind\" : \"CLOUDFOUNDRY\",\r\n  \"serviceDefinitionId\" : \"serviceDefinitionId\",\r\n  \"updateTime\" : \"updateTime\",\r\n  \"platformId\" : \"platformId\",\r\n  \"uuid\" : \"uuid\",\r\n  \"lastOperationState\" : \"lastOperationState\",\r\n  \"platformApiLocation\" : \"platformApiLocation\",\r\n  \"createTime\" : \"createTime\",\r\n  \"lastOperation\" : \"CREATE\",\r\n  \"name\" : \"name\",\r\n  \"tenantId\" : \"tenantId\",\r\n  \"subtenantId\" : \"subtenantId\",\r\n  \"planId\" : \"planId\"\r\n}", MatomoInstance.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<MatomoInstance>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    	log.debug("API::matomoInstanceGet");
    	MatomoInstance res = matomoInstanceService.getMatomoInstance(platformId, matomoInstanceId);
        return new ResponseEntity<MatomoInstance>(res, HttpStatus.OK);
    }

    public ResponseEntity<MatomoInstance> matomoInstanceUpdate(@ApiParam(value = "" ,required=true )  @Valid @RequestBody MatomoInstance body,@ApiParam(value = "",required=true) @PathVariable("platformId") String platformId,@ApiParam(value = "",required=true) @PathVariable("matomoInstanceId") String matomoInstanceId) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<MatomoInstance>(objectMapper.readValue("{\r\n  \"matomoVersion\" : \"matomoVersion\",\r\n  \"dashboardUrl\" : \"dashboardUrl\",\r\n  \"platformKind\" : \"CLOUDFOUNDRY\",\r\n  \"serviceDefinitionId\" : \"serviceDefinitionId\",\r\n  \"updateTime\" : \"updateTime\",\r\n  \"platformId\" : \"platformId\",\r\n  \"uuid\" : \"uuid\",\r\n  \"lastOperationState\" : \"lastOperationState\",\r\n  \"platformApiLocation\" : \"platformApiLocation\",\r\n  \"createTime\" : \"createTime\",\r\n  \"lastOperation\" : \"CREATE\",\r\n  \"name\" : \"name\",\r\n  \"tenantId\" : \"tenantId\",\r\n  \"subtenantId\" : \"subtenantId\",\r\n  \"planId\" : \"planId\"\r\n}", MatomoInstance.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<MatomoInstance>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        // TODO
    	log.debug("API::matomoInstanceUpdate");
        return new ResponseEntity<MatomoInstance>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<Platform> platformCreate(@ApiParam(value = "" ,required=true )  @Valid @RequestBody Platform body) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<Platform>(objectMapper.readValue("{\r\n  \"createTime\" : \"createTime\",\r\n  \"name\" : \"name\",\r\n  \"description\" : \"description\",\r\n  \"updateTime\" : \"updateTime\",\r\n  \"uuid\" : \"uuid\"\r\n}", Platform.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<Platform>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    	log.debug("API::platformCreate");
    	Platform res = platformService.createPlatform(body);
        return new ResponseEntity<Platform>(res, HttpStatus.OK);
    }

    public ResponseEntity<Void> platformDelete(@ApiParam(value = "",required=true) @PathVariable("platformId") String platformId) {
        String accept = request.getHeader("Accept");
    	log.debug("API::platformDelete");
        platformService.deletePlatform(platformId);
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    public ResponseEntity<List<Platform>> platformFind() {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<List<Platform>>(objectMapper.readValue("[ {\r\n  \"createTime\" : \"createTime\",\r\n  \"name\" : \"name\",\r\n  \"description\" : \"description\",\r\n  \"updateTime\" : \"updateTime\",\r\n  \"uuid\" : \"uuid\"\r\n}, {\r\n  \"createTime\" : \"createTime\",\r\n  \"name\" : \"name\",\r\n  \"description\" : \"description\",\r\n  \"updateTime\" : \"updateTime\",\r\n  \"uuid\" : \"uuid\"\r\n} ]", List.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<List<Platform>>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    	log.debug("API::platformFind");
    	List<Platform> res = platformService.findPlatform();
        return new ResponseEntity<List<Platform>>(res, HttpStatus.OK);
    }

    public ResponseEntity<Platform> platformGet(@ApiParam(value = "",required=true) @PathVariable("platformId") String platformId) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<Platform>(objectMapper.readValue("{\r\n  \"createTime\" : \"createTime\",\r\n  \"name\" : \"name\",\r\n  \"description\" : \"description\",\r\n  \"updateTime\" : \"updateTime\",\r\n  \"uuid\" : \"uuid\"\r\n}", Platform.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<Platform>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    	log.debug("API::platformGet");
    	Platform res = platformService.getPlatform(platformId);
        return new ResponseEntity<Platform>(res, HttpStatus.OK);
    }

    public ResponseEntity<Platform> platformUpdate(@ApiParam(value = "" ,required=true )  @Valid @RequestBody Platform body,@ApiParam(value = "",required=true) @PathVariable("platformId") String platformId) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<Platform>(objectMapper.readValue("{\r\n  \"createTime\" : \"createTime\",\r\n  \"name\" : \"name\",\r\n  \"description\" : \"description\",\r\n  \"updateTime\" : \"updateTime\",\r\n  \"uuid\" : \"uuid\"\r\n}", Platform.class), HttpStatus.NOT_IMPLEMENTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<Platform>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    	log.debug("API::platformUpdate");
    	Platform res = platformService.updatePlatform(platformId, body);
        return new ResponseEntity<Platform>(res, HttpStatus.OK);
    }

}
