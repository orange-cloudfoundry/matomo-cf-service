/**
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

import javax.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.orange.oss.matomocfservice.web.domain.PPlatform;

/**
 * @author P. Déchamboux
 *
 */
@SpringBootTest
public class TestPlatformService {
	@Autowired
	PlatformService platformService;
	private final static String ID_NO_PF = UUID.randomUUID().toString();

	@Test
	void testUnknownPlatformExist() {
		String uuid = platformService.getUnknownPlatformId();
		Assertions.assertNotNull(uuid);
		PPlatform ppf = platformService.getPlatform(uuid);
		Assertions.assertNotNull(ppf);
	}

	@Test
	void testGetNonExistingPlatform() {
		try {
			platformService.getPlatform(ID_NO_PF);
			Assertions.fail("Non existing platform: exception EntityNotFoundException should have been thrown");
		} catch (EntityNotFoundException e) {
		} catch (Exception e) {
			Assertions.fail("Should have thrown EntityNotFoundException", e);
		}
	}

	@Test
	void testGetPlatformWithNullId() {
		try {
			platformService.getPlatform(null);
			Assertions.fail("Should not be able to get platform with null id");
		} catch (IllegalArgumentException e) {
		} catch (Exception e) {
			Assertions.fail("Should have thrown IllegalArgumentException", e);
		}
	}

	@Test
	void testFindPlatform() {
		List<PPlatform> lppf = platformService.findPlatform();
		Assertions.assertEquals(1, lppf.size());
		PPlatform ppf = lppf.get(0);
		Assertions.assertNotNull(ppf);
		Assertions.assertEquals(platformService.getUnknownPlatformId(), ppf.getId());
	}

	@Test
	void testCreatePlatformWithNullId() {
		try {
			platformService.createPlatform(null, "PFNAME", "PFDESC");
			Assertions.fail("Should not be able to create platform with null id");
		} catch (IllegalArgumentException e) {
		} catch (Exception e) {
			Assertions.fail("Should have thrown IllegalArgumentException", e);
		}
	}

	@Test
	void testCreatePlatformWithTooLongId() {
		String uuid = "0123456789012345678901234567890123456789";
		try {
			platformService.createPlatform(uuid, "PFNAME", "PFDESC");
			Assertions.fail("Should not be able to create platform with too long id");
		} catch (IllegalArgumentException e) {
		} catch (Exception e) {
			Assertions.fail("Should have thrown IllegalArgumentException", e);
		}
	}

	@Test
	void testCreatePlatformWithNullName() {
		String uuid = UUID.randomUUID().toString();
		try {
			platformService.createPlatform(uuid, null, "PFDESC");
			Assertions.fail("Should not be able to create platform with null name");
		} catch (IllegalArgumentException e) {
		} catch (Exception e) {
			Assertions.fail("Should have thrown IllegalArgumentException", e);
		}
	}

	@Test
	void testCreatePlatformWithNullDesc() {
		String uuid = UUID.randomUUID().toString();
		try {
			platformService.createPlatform(uuid, "PFNAME", null);
			Assertions.fail("Should not be able to create platform with null desc");
		} catch (IllegalArgumentException e) {
		} catch (Exception e) {
			Assertions.fail("Should have thrown IllegalArgumentException", e);
		}
	}

	@Test
	void testCreateDeletePlatform() {
		String uuid = UUID.randomUUID().toString();
		PPlatform ppf = platformService.createPlatform(uuid, "PFNAME", "PFDESC");
		Assertions.assertNotNull(ppf);
		Assertions.assertNotNull(platformService.getPlatform(uuid));
		List<PPlatform> lppf = platformService.findPlatform();
		Assertions.assertEquals(2, lppf.size());
		platformService.deletePlatform(uuid);
		try {
			ppf = platformService.getPlatform(uuid);
			Assertions.fail("Non existing platform: exception EntityNotFoundException should have been thrown");
		} catch (EntityNotFoundException e) {
		}
	}

	@Test
	void testDeleteNonExistingPlatform() {
		String uuid = UUID.randomUUID().toString();
		try {
			platformService.deletePlatform(uuid);
			Assertions.fail("Non existing platform: exception EntityNotFoundException should have been thrown");
		} catch (EntityNotFoundException e) {
		} catch (Exception e) {
			Assertions.fail("Should have thrown EntityNotFoundException", e);
		}
	}	

	@Test
	void testDeletePlatformWithNullId() {
		try {
			platformService.deletePlatform(null);
			Assertions.fail("Should not be able to delete platform with null id");
		} catch (IllegalArgumentException e) {
		} catch (Exception e) {
			Assertions.fail("Should have thrown IllegalArgumentException", e);
		}
	}	

	@Test
	void testUpdatePlatform() {
		PPlatform ppf = platformService.updatePlatform(platformService.getUnknownPlatformId());
		Assertions.assertNotNull(ppf, "Should yield the modified platform");
	}	

	@Test
	void testUpdateNonExistingPlatform() {
		String uuid = UUID.randomUUID().toString();
		try {
			platformService.updatePlatform(uuid);
			Assertions.fail("Non existing platform: exception EntityNotFoundException should have been thrown");
		} catch (EntityNotFoundException e) {
		} catch (Exception e) {
			Assertions.fail("Should have thrown EntityNotFoundException", e);
		}
	}	

	@Test
	void testUpdatePlatformWithNullId() {
		try {
			platformService.updatePlatform(null);
			Assertions.fail("Should not be able to delete platform with null id");
		} catch (IllegalArgumentException e) {
		} catch (Exception e) {
			Assertions.fail("Should have thrown IllegalArgumentException", e);
		}
	}	
}
