/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.web.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orange.oss.matomocfservice.web.domain.PInstanceIdMgr;

/**
 * @author P. Déchamboux
 *
 */
@Repository
public interface PInstanceIdMgrRepository extends JpaRepository<PInstanceIdMgr, String> {
	Optional<PInstanceIdMgr> findById(int id);
}
