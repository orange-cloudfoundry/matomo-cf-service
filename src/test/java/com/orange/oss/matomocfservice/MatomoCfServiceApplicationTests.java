package com.orange.oss.matomocfservice;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MatomoCfServiceApplicationTests {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	public void testGetServiceInstance() {
		webTestClient.get().uri("/tweets")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentType(MediaType.APPLICATION_JSON)
			.expectBody();
//			.jsonPath("$.id")
//			.isNotEmpty()
//			.jsonPath("$.text")
//			.isEqualTo("This is a Test Tweet");
	}

}
