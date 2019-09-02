/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.web.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.orange.oss.matomocfservice.api.model.Platform;
import com.orange.oss.matomocfservice.web.domain.PPlatform;
import com.orange.oss.matomocfservice.web.repository.PPlatformRepository;

/**
 * @author P. Déchamboux
 * 
 */
@Service
public class PlatformService {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
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

	public Platform createPlatform(Platform platform) {
		LOGGER.debug("SERV::createPlatform: platform={}", platform.toString());
		PPlatform ppf = new PPlatform(platform.getUuid(), platform.getName(), platform.getDescription());
		pfRepo.save(ppf);
		return toApiModel(ppf);
	}

	public Platform getPlatform(String platformId) {
		LOGGER.debug("SERV::getPlatform: platformId={}", platformId);
		return toApiModel(pfRepo.getOne(platformId));
	}

	public List<Platform> findPlatform() {
		LOGGER.debug("SERV::findPlatform");
		List<Platform> pfs = new ArrayList<Platform>();
		for (PPlatform ppf : pfRepo.findAll()) {
			pfs.add(toApiModel(ppf));
		}
		return pfs;
	}

	public Platform deletePlatform(String platformId) {
		LOGGER.debug("SERV::deletePlatform: platformId={}", platformId);
		PPlatform ppf = pfRepo.getOne(platformId);
		Platform pf = toApiModel(ppf);
		pfRepo.delete(pfRepo.getOne(platformId));
		return pf;
	}

	public Platform updatePlatform(String platformId, Platform pf) {
		LOGGER.debug("SERV::updatePlatform: platformId={} platform={}", platformId, pf.toString());
		// TODO: not implemented
		return pf;
	}

	private Platform toApiModel(PPlatform ppf) {
		return new Platform()
				.uuid(ppf.getId())
				.name(ppf.getName())
				.description(ppf.getDescription())
				.createTime(ppf.getCreateTime().format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
				.updateTime(ppf.getUpdateTime().format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
	}
}
