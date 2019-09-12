/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.web.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orange.oss.matomocfservice.web.domain.POperationStatus;

/**
 * @author P. Déchamboux
 *
 */
@Repository
public interface POperationStatusRepository extends JpaRepository<POperationStatus, String> {
	Optional<POperationStatus> findById(String id);
}
