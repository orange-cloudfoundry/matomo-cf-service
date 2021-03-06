/**
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
package com.orange.oss.matomocfservice.web.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author P. Déchamboux
 *
 */
@SpringBootTest
public class TestMatomoReleases {
	@Value("${test.currentRelease}")
	private String currentRelease;
	@Value("${test.lowerReleaseNotAvailable}")
	private String lowerReleaseNotAvailable;
	private final static String INSTID1 = "000000001";
	private final static String INSTID2 = "000000002";
	private final static String INSTID3 = "000000003";
	private final static String CONTENTCONFINI = "content for config ini";

	@Test
	void testDefaultRelease() {
		Assertions.assertEquals(currentRelease, MatomoReleases.getDefaultReleaseName());
	}

	@Test
	void testLatestRelease() {
		Assertions.assertEquals(currentRelease, MatomoReleases.getLatestReleaseName());
	}

	@Test
	void testReleaseList() {
		List<MatomoReleaseSpec> rellist = MatomoReleases.getReleaseList(); 
		Assertions.assertNotNull(rellist);
		Assertions.assertEquals(1, rellist.size(), "Size of release list should be 1");
		MatomoReleaseSpec rel1 = rellist.get(0);
		Assertions.assertEquals(currentRelease, rel1.getName());
		Assertions.assertTrue(rel1.isDefault());
		Assertions.assertTrue(rel1.isLatest());
	}

	@Test
	void testReleaseAvailable() {
		Assertions.assertTrue(MatomoReleases.isVersionAvailable(currentRelease), "Current release should be available");
		Assertions.assertFalse(MatomoReleases.isVersionAvailable(lowerReleaseNotAvailable), "Lower release should not exist");
	}

	@Test
	void testHigherVersion() {
		Assertions.assertTrue(MatomoReleases.isHigherVersion(lowerReleaseNotAvailable, currentRelease));
		Assertions.assertFalse(MatomoReleases.isHigherVersion(currentRelease, lowerReleaseNotAvailable));
	}

	@Test
	void testReleasePath() {
		String matchstr = "(.*)matomo(.*)" + INSTID1 + "-" + currentRelease;
		Assertions.assertTrue(MatomoReleases.getVersionPath(currentRelease, INSTID1).matches(matchstr), "Path should match with regex \"" + matchstr + "\"");
	}

	@Test
	void testCreateLinkedTreeUnavailableVersion() {
		Assertions.assertThrows(RuntimeException.class, () -> {
			MatomoReleases.createLinkedTree(lowerReleaseNotAvailable, INSTID1);
		});
	}

	@Test
	void testCreateLinkedTreeCurrentVersion() {
		MatomoReleases.createLinkedTree(currentRelease, INSTID1);
		String relinstp = MatomoReleases.getVersionPath(currentRelease, INSTID1);
		File rootdir = new File(relinstp);
		Assertions.assertTrue(rootdir.exists());
		Assertions.assertTrue(new File(rootdir.getPath(), "matomo.js").exists());
	}

	@Test
	void testDeleteLinkedTreeCurrentVersion() {
		MatomoReleases.createLinkedTree(currentRelease, INSTID2);
		String relinstp = MatomoReleases.getVersionPath(currentRelease, INSTID2);
		File rootdir = new File(relinstp);
		Assertions.assertTrue(rootdir.exists());
		Assertions.assertTrue(new File(rootdir.getPath(), "matomo.js").exists());
		MatomoReleases.deleteLinkedTree(INSTID2);
		Assertions.assertFalse(rootdir.exists());
	}

	@Test
	void testConfigIniSetting() {
		MatomoReleases.createLinkedTree(currentRelease, INSTID3);
		String relinstp = MatomoReleases.getVersionPath(currentRelease, INSTID3);
		File rootdir = new File(relinstp);
		Assertions.assertTrue(rootdir.exists());
		MatomoReleases.setConfigIni(currentRelease, INSTID3, CONTENTCONFINI.getBytes());
		File configini = new File(rootdir.getPath() + "/config/config.ini.php");
		Assertions.assertTrue(configini.exists());
		try {
			byte[] content = Files.readAllBytes(Paths.get(rootdir.getPath() + "/config/config.ini.php"));
			Assertions.assertEquals(CONTENTCONFINI, new String(content), "Should not be able to get the right content for config.ini");
		} catch (IOException e) {
			Assertions.fail(e);
		}
	}
}
