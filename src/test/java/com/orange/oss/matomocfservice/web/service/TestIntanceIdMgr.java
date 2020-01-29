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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgrProperties;

/**
 * @author P. Déchamboux
 *
 */
@SpringBootTest
public class TestIntanceIdMgr {
	private final static Logger LOGGER = LoggerFactory.getLogger(TestIntanceIdMgr.class);
	@Autowired
	CloudFoundryMgrProperties cfMgrProp;
	@Autowired
	InstanceIdMgr instIdMgr;
	private int max_instid = -1;
	private int nb_allocated;
	private boolean allocated[];

	@BeforeEach
	void beforeTest() {
		if (max_instid == -1) {
			max_instid = cfMgrProp.getMaxServiceInstances();
			allocated = new boolean[max_instid];
			for (int i = 0; i < max_instid; i++) {
				allocated[i] = false;
				try {
					instIdMgr.freeInstanceId(i);
				} catch (RuntimeException e) {
				}
			}
		}
	}

	private int allocateOk() {
		int id = instIdMgr.allocateInstanceId();
		if ((id < 0) || (id >= max_instid)) {
			Assertions.fail("ID=" + id + ": should e in range " + 0 + ".." + new Integer(max_instid - 1));
		} else {
			nb_allocated++;
			allocated[id] = true;
		}
		return id;
	}

	private void allocateKO() {
		int id;
		try {
			id = instIdMgr.allocateInstanceId();
			nb_allocated++;
			allocated[id] = true;
			Assertions.fail("Should not be able to allocate more ID");
		} catch (RuntimeException e) {
		}
	}

	private void freeOk(int id) {
		try {
			instIdMgr.freeInstanceId(id);
		} catch (RuntimeException e) {
			Assertions.fail("Pb in freeing ID", e);
		}
	}

	private void freeKO(int id) {
		try {
			instIdMgr.freeInstanceId(id);
			Assertions.fail("Should not be able to free ID");
		} catch (RuntimeException e) {
		}
	}

	@Test
	void testAllocateFreeOne() {
		int id = allocateOk();
		freeOk(id);
 	}

	@Test
	void testAllocateFreeAll() {
		while (nb_allocated < max_instid) {
			allocateOk();
		}
		for (int i = 0; i < max_instid; i++) {
			freeOk(i);
		}
	}

	@Test
	void testAllocateKO() {
		while (nb_allocated < max_instid) {
			allocateOk();
		}
		allocateKO();
		for (int i = 0; i < max_instid; i++) {
			freeOk(i);
		}
 	}

	@Test
	void testFreeKO() {
		freeKO(0);
 	}

	@Test
	void testFreeWrongIdLow() {
		freeKO(-2);
 	}

	@Test
	void testFreeWrongIdHigh() {
		freeKO(max_instid + 2);
 	}
}
