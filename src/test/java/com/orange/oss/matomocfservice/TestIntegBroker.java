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

import static io.restassured.RestAssured.given;

import java.net.URI;
import java.util.UUID;

import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.web.client.RestTemplate;

import com.orange.oss.matomocfservice.cfmgr.CfMgr4TResponseMask;
import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgr;
import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgr4Test;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;

/**
 * @author P. Déchamboux
 *
 */
@SpringBootTest(webEnvironment = 
SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestIntegBroker {
	private final static Logger LOGGER = LoggerFactory.getLogger(TestIntegBroker.class);
	@LocalServerPort
	private int port = -1;
	private final String BASEPATH = "/v2/";
	private static final Catalog catalog = new Catalog();
	@Autowired
	CloudFoundryMgr cfMgr;
	CloudFoundryMgr4Test cfMgr4T = null;

	private final int GLOBAL_SHARED_DB = 0;
	private final int MATOMO_SHARED_DB = 1;
	private final int DEDICTED_DB = 2;
	static class Plan {
		String id;
		String name;
		String description;
		boolean free;
	}

	static class Catalog {
	    String id = null;
	    String name;
	    String description;
	    boolean bindable;
	    Plan plans[] = new Plan[4];
	    String tags[];
	    String md_longDescription;
	    String md_documentationUrl;
	    String md_providerDisplayName;
	    String md_displayName;
	    
	    Catalog() {
	    	for (int i = 0; i < plans.length; i++) {
	    		plans[i] = new Plan();
	    	}
	    }
	}

	private String createBody(String instname, String planid, String param_version, int nbinst) {
		StringBuffer sb = new StringBuffer("{\"service_id\": \"");
		sb.append(catalog.id);
		sb.append("\", \"plan_id\": \"");
		sb.append(planid);
		sb.append("\", \"context\": {\"platform\": \"cloudfoundry\"");
		sb.append(", \"api_info_location\": \"https://api.cloudfoundry.mycompany\"");
		sb.append(", \"instance_name\": \"");
		sb.append(instname);
		sb.append("\"");
		sb.append(", \"organization_guid\": \"myorg\"");
		sb.append(", \"space_guid\": \"myspace\"");
		// other context fields below
		//sb.append(", \"\": \"\"");
		sb.append("}, \"parameters\": {");
		// parameters below
		String comma = "";
		if (param_version == null) {
			sb.append("\"matomoVersion\": \"");
			sb.append(param_version);
			sb.append("\"");
			comma = ", ";
		}
		if (nbinst == -1) {
			sb.append(comma);
			sb.append("\"matomoInstances\": \"");
			sb.append(nbinst);
			sb.append("\"");
		}
		//sb.append("\"\": \"\"");
		sb.append("}, \"maintenance_info\": {");
		sb.append("}}");
		return sb.toString();
	}

	private String createServiceInstUri(String uuid, String serv_id, String plan_id) {
		StringBuffer sb = new StringBuffer("service_instances/");
		sb.append(uuid);
		sb.append("?accepts_incomplete=true");
		if (serv_id != null) {
			sb.append("&service_id=");
			sb.append(serv_id);
		}
		if (plan_id != null) {
			sb.append("&plan_id=");
			sb.append(plan_id);
		}
		return sb.toString();
	}

	private String createPollServOpUri(String uuid) {
		StringBuffer sb = new StringBuffer("service_instances/");
		sb.append(uuid);
		sb.append("/last_operation");
		return sb.toString();
	}

	@BeforeEach
	void setup() {
		LOGGER.debug("setup: port={}", port);
		RestAssured.baseURI  = "http://localhost";
		RestAssured.port = port;
		RestAssured.basePath = BASEPATH;
		RestAssured.defaultParser = Parser.JSON;
		if (cfMgr4T == null) {
			cfMgr4T = (CloudFoundryMgr4Test)cfMgr;
		}
		if (catalog.id == null) {
			try {
				RestTemplate restTemplate = new RestTemplate();
				URI uri = new URI("http://localhost:" + port + BASEPATH + "catalog");
				LOGGER.debug("Base URI: {}", uri.toString());
				String res = restTemplate.getForObject(uri, String.class);
				JSONObject json_catalog = new JSONObject(res);
				LOGGER.debug("Catalog: {}", json_catalog.toString(3));
				catalog.id = json_catalog.getJSONArray("services").getJSONObject(0).getString("id");
				catalog.name = json_catalog.getJSONArray("services").getJSONObject(0).getString("name");
				catalog.description = json_catalog.getJSONArray("services").getJSONObject(0).getString("description");
				catalog.bindable = json_catalog.getJSONArray("services").getJSONObject(0).getBoolean("bindable");
		    	for (int i = 0; i < catalog.plans.length; i++) {
		    		catalog.plans[i].id = json_catalog.getJSONArray("services").getJSONObject(0).getJSONArray("plans").getJSONObject(i).getString("id");
		    		catalog.plans[i].name = json_catalog.getJSONArray("services").getJSONObject(0).getJSONArray("plans").getJSONObject(i).getString("name");
		    		catalog.plans[i].description = json_catalog.getJSONArray("services").getJSONObject(0).getJSONArray("plans").getJSONObject(i).getString("description");
		    		catalog.plans[i].free = json_catalog.getJSONArray("services").getJSONObject(0).getJSONArray("plans").getJSONObject(i).getBoolean("free");
		    	}
				catalog.md_longDescription = json_catalog.getJSONArray("services").getJSONObject(0).getJSONObject("metadata").getString("longDescription");
				catalog.md_documentationUrl = json_catalog.getJSONArray("services").getJSONObject(0).getJSONObject("metadata").getString("documentationUrl");
				catalog.md_providerDisplayName = json_catalog.getJSONArray("services").getJSONObject(0).getJSONObject("metadata").getString("providerDisplayName");
				catalog.md_displayName = json_catalog.getJSONArray("services").getJSONObject(0).getJSONObject("metadata").getString("displayName");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void testGetCatalog() {
		LOGGER.debug("testGetCatalog");
		Response resp =
				given()
				.header("Content-Type", "application/json").
				when()
				.get("catalog").
				then()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		Assertions.assertEquals("matomo-cf-service", resp.jsonPath().getString("services[0].id"));
	}

	@Test
	void testPollServOpKONoServ() {
		LOGGER.debug("testPollServOpKONoServ");
		String instid = UUID.randomUUID().toString();
		cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
		Response resp = given()
				.header("Content-Type", "application/json").
				when()
				.get(createPollServOpUri(instid)).
				then()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		LOGGER.debug("Poll Resp: {}", resp.prettyPrint());
	}

	@Test
	void testCreateInstanceGlobShared() {
		LOGGER.debug("testCreateInstanceGlobShared");
		String instid = UUID.randomUUID().toString();
		cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
		Response resp =
				given()
				.header("Content-Type", "application/json")
				.body(createBody("M01", catalog.plans[GLOBAL_SHARED_DB].id, null, -1)).
				when()
				.put(createServiceInstUri(instid, null, null)).
				then()
				.statusCode(202)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		resp = given()
				.header("Content-Type", "application/json").
				when()
				.get(createPollServOpUri(instid)).
				then()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		Assertions.assertEquals("succeeded", resp.getBody().jsonPath().getString("state"));
		resp = given()
				.header("Content-Type", "application/json").
				when()
				.delete(createServiceInstUri(instid, catalog.id, catalog.plans[GLOBAL_SHARED_DB].id)).
				then()
				.statusCode(202)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		resp = given()
				.header("Content-Type", "application/json").
				when()
				.get(createPollServOpUri(instid)).
				then()
				.statusCode(410)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		Assertions.assertEquals("succeeded", resp.getBody().jsonPath().getString("state"));
	}

	@Test
	void testGetInstanceGlobShared() {
		LOGGER.debug("testGetInstanceGlobShared");
		String instid = UUID.randomUUID().toString();
		cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
		Response resp =
				given()
				.header("Content-Type", "application/json")
				.body(createBody("M02", catalog.plans[GLOBAL_SHARED_DB].id, null, -1)).
				when()
				.put(createServiceInstUri(instid, null, null)).
				then()
				.statusCode(202)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		resp = given()
				.header("Content-Type", "application/json").
				when()
				.get(createPollServOpUri(instid)).
				then()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		Assertions.assertEquals("succeeded", resp.getBody().jsonPath().getString("state"));
		resp = given()
				.header("Content-Type", "application/json").
				when()
				.get(createServiceInstUri(instid, null, null)).
				then()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		Assertions.assertEquals(catalog.id, resp.getBody().jsonPath().getString("service_id"));
		Assertions.assertEquals(catalog.plans[GLOBAL_SHARED_DB].id, resp.getBody().jsonPath().getString("plan_id"));
		resp = given()
				.header("Content-Type", "application/json").
				when()
				.delete(createServiceInstUri(instid, catalog.id, catalog.plans[GLOBAL_SHARED_DB].id)).
				then()
				.statusCode(202)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		resp = given()
				.header("Content-Type", "application/json").
				when()
				.get(createPollServOpUri(instid)).
				then()
				.statusCode(410)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		Assertions.assertEquals("succeeded", resp.getBody().jsonPath().getString("state"));
	}

	@Test
	void testGetInstanceGlobSharedKONotExist() {
		LOGGER.debug("testGetInstanceGlobSharedKONotExist");
		String instid = UUID.randomUUID().toString();
		cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
		Response resp = given()
				.header("Content-Type", "application/json")
				.body(createBody("M04", catalog.plans[GLOBAL_SHARED_DB].id, null, -1)).
				when()
				.get(createServiceInstUri(instid, null, null)).
				then()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		Assertions.assertEquals("", resp.getBody().jsonPath().getString("service_id"));
		Assertions.assertEquals("", resp.getBody().jsonPath().getString("plan_id"));
	}

	@Test
	void testUpdateInstanceGlobShared() {
		LOGGER.debug("testUpdateInstanceGlobShared");
		String instid = UUID.randomUUID().toString();
		cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
		Response resp =
				given()
				.header("Content-Type", "application/json")
				.body(createBody("M05", catalog.plans[GLOBAL_SHARED_DB].id, null, -1)).
				when()
				.put(createServiceInstUri(instid, null, null)).
				then()
				.statusCode(202)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		resp = given()
				.header("Content-Type", "application/json").
				when()
				.get(createPollServOpUri(instid)).
				then()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		Assertions.assertEquals("succeeded", resp.getBody().jsonPath().getString("state"));
		resp = given()
				.header("Content-Type", "application/json")
				.body(createBody("M06", catalog.plans[GLOBAL_SHARED_DB].id, "3.45.0", -1)).
				when()
				.patch(createServiceInstUri(instid, null, null)).
				then()
				.statusCode(202)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		LOGGER.debug("Resp: {}", resp.prettyPrint());
		resp = given()
				.header("Content-Type", "application/json").
				when()
				.delete(createServiceInstUri(instid, catalog.id, catalog.plans[GLOBAL_SHARED_DB].id)).
				then()
				.statusCode(202)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		resp = given()
				.header("Content-Type", "application/json").
				when()
				.get(createPollServOpUri(instid)).
				then()
				.statusCode(410)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		Assertions.assertEquals("succeeded", resp.getBody().jsonPath().getString("state"));
	}

	@Test
	void testCreateInstanceGlobSharedKOGSDBNotReady() {
		LOGGER.debug("testCreateInstanceGlobSharedKOGSDBNotReady");
		String instid = UUID.randomUUID().toString();
		cfMgr4T.setResponseMask(new CfMgr4TResponseMask().globalSharedReady(false));
		Response resp =
				given()
				.header("Content-Type", "application/json")
				.body(createBody("M07", catalog.plans[GLOBAL_SHARED_DB].id, null, -1)).
				when()
				.put(createServiceInstUri(instid, null, null)).
				then()
				.statusCode(202)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		resp = given()
				.header("Content-Type", "application/json").
				when()
				.get(createPollServOpUri(instid)).
				then()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		Assertions.assertEquals("failed", resp.getBody().jsonPath().getString("state"));
		given()
				.header("Content-Type", "application/json").
				when()
				.delete(createServiceInstUri(instid, catalog.id, catalog.plans[GLOBAL_SHARED_DB].id)).
				then()
				.statusCode(410);
	}

//	@Test
//	void testCreateInstanceGlobSharedKODupInstname() {
//		LOGGER.debug("testCreateInstanceGlobSharedKODupInstname");
//		String instid = UUID.randomUUID().toString();
//		cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
//		Response resp =
//				given()
//				.header("Content-Type", "application/json")
//				.body(createBody("M08", catalog.plans[GLOBAL_SHARED_DB].id, null, -1)).
//				when()
//				.put(createServiceInstUri(instid, null, null)).
//				then()
//				.statusCode(202)
//				.contentType(ContentType.JSON)
//				.extract()
//				.response();
//		resp = given()
//				.header("Content-Type", "application/json")
//				.body(createBody("M08", catalog.plans[GLOBAL_SHARED_DB].id, null, -1)).
//				when()
//				.put(createServiceInstUri(UUID.randomUUID().toString(), null, null)).
//				then()
//				.statusCode(202)
//				.contentType(ContentType.JSON)
//				.extract()
//				.response();
//		Assertions.assertTrue(resp.getBody().jsonPath().getString("operation").startsWith("Matomo Instance with name="));
//		resp = given()
//				.header("Content-Type", "application/json").
//				when()
//				.delete(createServiceInstUri(instid, catalog.id, catalog.plans[GLOBAL_SHARED_DB].id)).
//				then()
//				.statusCode(202)
//				.contentType(ContentType.JSON)
//				.extract()
//				.response();
//	}

	@Test
	void testCreateInstanceMatomoShared() {
		LOGGER.debug("testCreateInstanceMatomoShared");
		String instid = UUID.randomUUID().toString();
		cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
		Response resp =
				given()
				.header("Content-Type", "application/json")
				.body(createBody("M09", catalog.plans[MATOMO_SHARED_DB].id, null, -1)).
				when()
				.put(createServiceInstUri(instid, null, null)).
				then()
				.statusCode(202)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		resp = given()
				.header("Content-Type", "application/json").
				when()
				.get(createPollServOpUri(instid)).
				then()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		Assertions.assertEquals("succeeded", resp.getBody().jsonPath().getString("state"));
		resp = given()
				.header("Content-Type", "application/json").
				when()
				.delete(createServiceInstUri(instid, catalog.id, catalog.plans[GLOBAL_SHARED_DB].id)).
				then()
				.statusCode(202)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		resp = given()
				.header("Content-Type", "application/json").
				when()
				.get(createPollServOpUri(instid)).
				then()
				.statusCode(410)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		Assertions.assertEquals("succeeded", resp.getBody().jsonPath().getString("state"));
	}

	@Test
	void testCreateInstanceDedicated() {
		LOGGER.debug("testCreateInstanceDedicated");
		String instid = UUID.randomUUID().toString();
		cfMgr4T.setResponseMask(new CfMgr4TResponseMask());
		Response resp =
				given()
				.header("Content-Type", "application/json")
				.body(createBody("M10", catalog.plans[DEDICTED_DB].id, null, -1)).
				when()
				.put(createServiceInstUri(instid, null, null)).
				then()
				.statusCode(202)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		resp = given()
				.header("Content-Type", "application/json").
				when()
				.get(createPollServOpUri(instid)).
				then()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		Assertions.assertEquals("succeeded", resp.getBody().jsonPath().getString("state"));
		resp = given()
				.header("Content-Type", "application/json").
				when()
				.delete(createServiceInstUri(instid, catalog.id, catalog.plans[GLOBAL_SHARED_DB].id)).
				then()
				.statusCode(202)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		resp = given()
				.header("Content-Type", "application/json").
				when()
				.get(createPollServOpUri(instid)).
				then()
				.statusCode(410)
				.contentType(ContentType.JSON)
				.extract()
				.response();
		Assertions.assertEquals("succeeded", resp.getBody().jsonPath().getString("state"));
	}
}
