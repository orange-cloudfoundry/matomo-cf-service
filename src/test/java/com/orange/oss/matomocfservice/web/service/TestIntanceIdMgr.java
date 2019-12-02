/**
 * 
 */
package com.orange.oss.matomocfservice.web.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgrProperties;

/**
 * @author P. Déchamboux
 *
 */
@SpringBootTest
public class TestIntanceIdMgr {
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
	void testAllocateOne() {
		int id = allocateOk();
		freeOk(id);
 	}

	@Test
	void testAllocateAll() {
		while (nb_allocated < max_instid) {
			allocateOk();
		}
		for (int i = 0; i < max_instid; i++) {
			freeOk(i);
		}
	}

	@Test
	void testAllocateKO() {
		int id = allocateOk();
		freeOk(id);
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
