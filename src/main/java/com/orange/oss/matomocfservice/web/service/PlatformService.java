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

package com.orange.oss.matomocfservice.web.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.orange.oss.matomocfservice.web.domain.PPlatform;
import com.orange.oss.matomocfservice.web.repository.PPlatformRepository;

/**
 * @author P. Déchamboux
 * 
 */
@Service
public class PlatformService {
	private final static Logger LOGGER = LoggerFactory.getLogger(PlatformService.class);
	public final static String UNKNOWNPLATFORM_NAME = "UnknownPlatform";
	private final static String UNKNOWNPLATFORM_DESC = "This is the platform to which to attach service instances that are created with no platform.";
	@Autowired
	PPlatformRepository pfRepo;
	private String unknownPlatformId;

	public void initialize() {
		LOGGER.debug("SERV::PlatformService-initialize");
		PPlatform unknownPlatform = pfRepo.findByName(UNKNOWNPLATFORM_NAME);
		if (unknownPlatform != null) {
			LOGGER.debug("SERV::   unknown platform already exists");
		} else {
			unknownPlatform = new PPlatform(UUID.randomUUID().toString(), UNKNOWNPLATFORM_NAME, UNKNOWNPLATFORM_DESC);
			pfRepo.save(unknownPlatform);
			LOGGER.debug("SERV::   unknown platform has been created");
		}
		unknownPlatformId = unknownPlatform.getId();
	}

	public String getUnknownPlatformId() {
		return unknownPlatformId;
	}

	public PPlatform createPlatform(String uuid, String name, String desc) {
		LOGGER.debug("SERV::createPlatform: platform={}", name);
		PPlatform ppf = new PPlatform(uuid, name, desc);
		return pfRepo.save(ppf);
	}

	public PPlatform getPlatform(String uuid) {
		LOGGER.debug("SERV::getPlatform: platformId={}", uuid);
		return pfRepo.getOne(uuid);
	}

	public List<PPlatform> findPlatform() {
		LOGGER.debug("SERV::findPlatform");
		return pfRepo.findAll();
	}

	public void deletePlatform(String platformId) {
		LOGGER.debug("SERV::deletePlatform: platformId={}", platformId);
		pfRepo.delete(pfRepo.getOne(platformId));
	}

	public PPlatform updatePlatform(String uuid) {
		LOGGER.debug("SERV::updatePlatform: platformId={}", uuid);
		// TODO: not implemented
		return pfRepo.getOne(uuid);
	}

//	private Platform toApiModel(PPlatform ppf) {
//		return new Platform()
//				.uuid(ppf.getId())
//				.name(ppf.getName())
//				.description(ppf.getDescription())
//				.createTime(ppf.getCreateTime().format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
//				.updateTime(ppf.getUpdateTime().format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
//	}
}
