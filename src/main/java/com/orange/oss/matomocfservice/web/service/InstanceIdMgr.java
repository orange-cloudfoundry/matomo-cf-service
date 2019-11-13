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
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgrProperties;
import com.orange.oss.matomocfservice.web.domain.PInstanceId;
import com.orange.oss.matomocfservice.web.domain.PInstanceIdMgr;
import com.orange.oss.matomocfservice.web.repository.PInstanceIdMgrRepository;
import com.orange.oss.matomocfservice.web.repository.PInstanceIdRepository;

/**
 * @author P. Déchamboux
 *
 */
@Service
public class InstanceIdMgr {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	@Autowired
	PInstanceIdRepository piiRepo;
	@Autowired
	PInstanceIdMgrRepository piimRepo;
	@Autowired
	CloudFoundryMgrProperties cfMgrProp;

	@Transactional
	public void initialize() {
		LOGGER.debug("SERV::InstanceIdMgr-initialize");
		int startnew;
		Optional<PInstanceIdMgr> opiim = piimRepo.findById(PInstanceIdMgr.UNIQUEID);
		if (!opiim.isPresent()) {
			LOGGER.info("Initialize service capacity to {} max instances",
					cfMgrProp.getMaxServiceInstances());
			PInstanceIdMgr piim = new PInstanceIdMgr(cfMgrProp.getMaxServiceInstances());
			piimRepo.save(piim);
			startnew = 0;
		} else {
			startnew = opiim.get().getMaxAllocatable();
			if (cfMgrProp.getMaxServiceInstances() > opiim.get().getMaxAllocatable()) {
				LOGGER.info("Increase service capacity from {} instances to {}",
						opiim.get().getMaxAllocatable(), cfMgrProp.getMaxServiceInstances());
				opiim.get().setMaxAllocatable(cfMgrProp.getMaxServiceInstances());
				piimRepo.save(opiim.get());
			} else if (cfMgrProp.getMaxServiceInstances() < opiim.get().getMaxAllocatable()) {
				LOGGER.info("Cannot decrease service capacity: keep max instances to {}",
						opiim.get().getMaxAllocatable());
			}
		}
		for (int i = startnew; i < cfMgrProp.getMaxServiceInstances(); i++) {
			PInstanceId pii = new PInstanceId(i);
			piiRepo.save(pii);
		}
	}

	@Transactional
	public int allocateInstanceId() {
		LOGGER.debug("SERV::InstanceIdMgr-allocateInstanceId");
		int id = ThreadLocalRandom.current().nextInt(0, cfMgrProp.getMaxServiceInstances());
		Optional<PInstanceId> opii = piiRepo.findById(id);
		PInstanceId pii = null;
		if (!opii.get().isAllocated()) {
			pii = opii.get();
		} else {
			List<PInstanceId> freePiis = piiRepo.findByAllocatedAndIdGreaterThan(false, id);
			for (PInstanceId spii : freePiis) {
				if (!spii.isAllocated()) {
					pii = spii;
					break;
				}
			}
			if (pii == null) {
				freePiis = piiRepo.findByAllocatedAndIdGreaterThan(false, -1);
				for (PInstanceId spii : freePiis) {
					if (!spii.isAllocated()) {
						pii = spii;
						break;
					}
				}
			}
		}
		if (pii == null) {
			throw new RuntimeException("Cannot allocate more Matomo service instance");
		}
		pii.setAllocated(true);
		piiRepo.save(pii);
		Optional<PInstanceIdMgr> opiim = piimRepo.findById(PInstanceIdMgr.UNIQUEID);
		opiim.get().incNbAllocated();
		piimRepo.save(opiim.get());
		return pii.getId();
	}

	@Transactional
	public void freeInstanceId(int id) {
		LOGGER.debug("SERV::InstanceIdMgr-freeInstanceId");
		Optional<PInstanceId> opii = piiRepo.findById(id);
		if (!opii.get().isAllocated()) {
			return;
		}
		opii.get().setAllocated(false);
		piiRepo.save(opii.get());
		Optional<PInstanceIdMgr> opiim = piimRepo.findById(PInstanceIdMgr.UNIQUEID);
		opiim.get().decNbAllocated();
		piimRepo.save(opiim.get());		
	}
}
