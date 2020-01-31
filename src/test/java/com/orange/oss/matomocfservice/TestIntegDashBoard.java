/**
 * Copyright 2020 Orange and the original author or authors.
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
package com.orange.oss.matomocfservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.get;

/**
 * @author P. Déchamboux
 *
 */
@SpringBootTest(webEnvironment = 
SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestIntegDashBoard {
	private final static Logger LOGGER = LoggerFactory.getLogger(TestIntegDashBoard.class);
	@LocalServerPort
	private int port = -1;
	private final String BASEPATH = "/";

	@BeforeEach
	void setup() {
		LOGGER.debug("setup: port={}", port);
		RestAssured.baseURI  = "http://localhost";
		RestAssured.port = port;
		RestAssured.basePath = BASEPATH;
	}

	@Test
	void testGetDashBoardRoot() {
		LOGGER.debug("testGetDashBoardRoot");
		get("").then().statusCode(200);
	}

	@Test
	void testGetDashBoardIndex() {
		LOGGER.debug("testGetDashBoardIndex");
		get("index.html").then().statusCode(200);
	}

	@Test
	void testGetDashBoardReleases() {
		LOGGER.debug("testGetDashBoardReleases");
		get("releases.html").then().statusCode(200);
	}

	@Test
	void testGetDashBoardOtherKO() {
		LOGGER.debug("testGetDashBoardOtherKO");
		get("other.html").then().statusCode(404);
	}
}
