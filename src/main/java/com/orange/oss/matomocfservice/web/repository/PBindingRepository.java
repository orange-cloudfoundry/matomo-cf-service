/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.web.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.orange.oss.matomocfservice.web.domain.PBinding;

/**
 * @author P. Déchamboux
 *
 */
public interface PBindingRepository extends JpaRepository<PBinding, String> {
	Optional<PBinding> findById(String id);
}
