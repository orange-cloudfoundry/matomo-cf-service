/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.web.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orange.oss.matomocfservice.web.domain.PPlatform;

/**
 * @author P. Déchamboux
 *
 */
@Repository
public interface PPlatformRepository extends JpaRepository<PPlatform, String> {
	Optional<PPlatform> findById(String id);
	PPlatform findByName(String name);
}
