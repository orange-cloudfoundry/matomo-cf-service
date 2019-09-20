/**
 * Orange File HEADER
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
		Optional<PInstanceIdMgr> opiim = piimRepo.findById(PInstanceIdMgr.UNIQUEID);
		if (!opiim.isPresent()) {
			PInstanceIdMgr piim = new PInstanceIdMgr(cfMgrProp.getMaxServiceInstances());
			piimRepo.save(piim);
		}
		for (int i = 0; i < cfMgrProp.getMaxServiceInstances(); i++) {
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
