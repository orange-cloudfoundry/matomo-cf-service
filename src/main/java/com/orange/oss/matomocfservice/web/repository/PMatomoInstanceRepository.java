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
	Optional<PMatomoInstance> findByUuid(String id);
	List<PMatomoInstance> findByPlatform(PPlatform pf);
	List<PMatomoInstance> findByPlatformAndLastOperation(PPlatform pf, String lastop);
}
