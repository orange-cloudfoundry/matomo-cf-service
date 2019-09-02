/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.web.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orange.oss.matomocfservice.web.domain.PInstanceId;

/**
 * @author P. Déchamboux
 *
 */
@Repository
public interface PInstanceIdRepository extends JpaRepository<PInstanceId, String> {
	Optional<PInstanceId> findById(int id);
	List<PInstanceId> findByAllocatedAndIdGreaterThan(boolean alloc, int firstid);
}
