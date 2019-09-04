/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.web.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orange.oss.matomocfservice.web.domain.PMatomoInstance;
import com.orange.oss.matomocfservice.web.domain.PPlatform;

/**
 * @author P. Déchamboux
 *
 */
@Repository
public interface PMatomoInstanceRepository extends JpaRepository<PMatomoInstance, String> {
	Optional<PMatomoInstance> findById(String id);
	List<PMatomoInstance> findByPlatform(PPlatform pf);
	List<PMatomoInstance> findByPlatformAndLastOperation(PPlatform pf, String lastop);
}
