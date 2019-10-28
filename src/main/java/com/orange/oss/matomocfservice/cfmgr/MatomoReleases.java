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

package com.orange.oss.matomocfservice.cfmgr;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * @author P. Déchamboux
 *
 */
@Service
public class MatomoReleases {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	private final static String RELEASEPATH = "/home/vcap/app/BOOT-INF/classes/static/matomo-releases";
	private final static String VERSIONSFILE = File.separator + "Versions";
	private final static String DEFVERSIONFILE = File.separator + "DefaultVersion";
	private final static String LATESTVERSIONFILE = File.separator + "LatestVersion";
	private Path tempDir;
	private String defaultRel;
	private String latestRel;
	private List<MatomoReleaseSpec> releases;

	public void initialize() {
		LOGGER.debug("CFMGR::MatomoReleases: initialize");
		try {
			this.defaultRel = null;
			this.tempDir = Files.createTempDirectory("matomo");
			LOGGER.debug("CFMGR::MatomoReleases: initialize - tempDir={}", this.tempDir.toString());
			File fsshd = new File("/home/vcap/.ssh");
			if (!fsshd.exists()) {
				fsshd.mkdir();
				fsshd.setExecutable(true, true);
				fsshd.setReadable(true, true);
				fsshd.setWritable(true, true);
			}
			File fkh = new File(fsshd, "known_hosts");
			if (fkh.createNewFile()) {
				fkh.setExecutable(false, false);
				fkh.setReadable(true, true);
				fkh.setWritable(true, true);
			}
			this.defaultRel = new String(Files.readAllBytes(Paths.get(RELEASEPATH + DEFVERSIONFILE))).trim();
			this.latestRel = new String(Files.readAllBytes(Paths.get(RELEASEPATH + LATESTVERSIONFILE))).trim();
			this.releases = new ArrayList<MatomoReleaseSpec>();
			LOGGER.debug("CFMGR:: defaultVersion=\"{}\", latest=\"{}\"", this.defaultRel, this.latestRel);
			if (this.defaultRel.equals(this.latestRel)) {
				this.releases.add(new MatomoReleaseSpec(defaultRel).defaultRel().latestRel());
			} else {
				this.releases.add(new MatomoReleaseSpec(defaultRel).defaultRel());
			}
			String versions = new String(Files.readAllBytes(Paths.get(RELEASEPATH + VERSIONSFILE)));
			LOGGER.debug("CFMGR:: versions=" + versions);
			for (String vers : versions.split(";")) {
				if (! vers.equals(defaultRel)) {
					if (vers.equals(this.latestRel)) {
						this.releases.add(new MatomoReleaseSpec(vers).latestRel());
					} else {
						this.releases.add(new MatomoReleaseSpec(vers));
					}
				}
			}
		} catch (IOException e) {
			LOGGER.error("CFMGR::MatomoReleases: initialize: problem while manipulating files within service container -> " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Cannot read versions file from the service bundle or create temp dir.");
		}
	}

	public String getDefaultReleaseName() {
		return this.defaultRel;
	}

	public String getLatestReleaseName() {
		return this.latestRel;
	}

	public List<MatomoReleaseSpec> getReleaseList() {
		return this.releases;
	}

	public boolean isVersionAvailable(String instversion) {
		Assert.notNull(instversion, "a version should be defined");
		LOGGER.debug("CFMGR::isVersionAvailable: version={}", instversion);
		for (MatomoReleaseSpec relsp : this.releases) {
			LOGGER.debug("CFMGR::isVersionAvailable: available version={}", relsp.getName());
			if (relsp.getName().equals(instversion)) {
				return true;
			}
		}
		return false;
	}

	public boolean isHigherVersion(String vers1, String vers2) {
		String[] l_mmc = vers1.split("\\.");
		String[] c_mmc = vers2.split("\\.");
		if ((l_mmc.length != 3) || (c_mmc.length != 3)) {
			LOGGER.debug("CFMGR::isHigherVersion: cannot decompose versions - {} or {} is malformed (i.e., M.m.c)", vers1, vers2);
			return false;
		}
		if (l_mmc[0].compareTo(c_mmc[0]) > 0) {
			return true;
		}
		if (l_mmc[0].compareTo(c_mmc[0]) < 0) {
			return false;
		}
		if (l_mmc[1].compareTo(c_mmc[1]) > 0) {
			return true;
		}
		if (l_mmc[1].compareTo(c_mmc[1]) < 0) {
			return false;
		}
		if (l_mmc[2].compareTo(c_mmc[2]) > 0) {
			return true;
		}
		return false;
	}

	public String getVersionPath(String version, String instId) {
		if (version == null) {
			File [] files = new File(this.tempDir.toString()).listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.startsWith(instId);
				}
			});
			if (files.length != 1) {
				return null;
			}
			if (!files[0].isDirectory()) {
				return null;
			}
			return files[0].getAbsolutePath();
		}
		return this.tempDir.toString() + File.separator + instId + "-" + version;
	}

	public void setConfigIni(String instId, String version, byte filecontent[]) {
		LOGGER.debug("CFMGR::setConfigIni: version={}, instId={}", version, instId);
		try {
			Files.write(Paths.get(getVersionPath(version, instId) + "/config/config.ini.php"), filecontent, StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			LOGGER.error("CFMGR::MatomoReleases:setConfigIni: problem while manipulating files within service container -> " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("IO pb in CFMGR::setConfigIni", e);
		}
	}

	public void createLinkedTree(String instId, String version) {
		LOGGER.debug("CFMGR::createLinkedTree: version={}, instId={}", version, instId);
		String versdir = RELEASEPATH + File.separator + version;
		String instdir = getVersionPath(version, instId);
		if (instdir == null) {
			throw new RuntimeException("Matomo " + version + ": unknow release");
		}
		LOGGER.debug("CFMGR::createLinkedTree: copy from <{}> to <{}>", versdir, instdir);
		try {
			// create a copy of the version dir for the instance
			if (new File(instdir).exists()) {
				// if exist, cleanup situation
				deleteLinkedTree(instId);
			}
			Path sourcePath = Paths.get(versdir);
			Path targetPath = Paths.get(instdir);
			Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
						throws IOException {
					Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));
					return FileVisitResult.CONTINUE;
				}
				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
					CopyOption[] options = new CopyOption[]{
						      StandardCopyOption.REPLACE_EXISTING,
						      StandardCopyOption.COPY_ATTRIBUTES
						    };
					Files.copy(file, targetPath.resolve(sourcePath.relativize(file)), options);
//					Files.createLink(targetPath.resolve(sourcePath.relativize(file)), file);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			LOGGER.error("CFMGR::MatomoReleases: createLinkedTree: problem while manipulating files within service container.", e);
			throw new RuntimeException("IO pb in CFMGR::createLinkedTree", e);
		}
	}

	public void deleteLinkedTree(String instId) {
		LOGGER.debug("CFMGR::deleteLinkedTree: instId={}", instId);
		String vpath = getVersionPath(null, instId);
		Path sourcePath = Paths.get(vpath);
		try {
			Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
					new File(file.toString()).delete();
					return FileVisitResult.CONTINUE;
				}
				@Override
				public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
					new File(dir.toString()).delete();
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			LOGGER.error("CFMGR::MatomoReleases: deleteLinkedTree: problem while manipulating files within service container.", e);
			throw new RuntimeException("IO pb in CFMGR::deleteLinkedTree", e);
		}
		new File(vpath).delete();
	}

	public class MatomoReleaseSpec {
		public String name;
		public boolean isDefault = false;
		public boolean isLatest = false;

		MatomoReleaseSpec(String n) {
			name = n;
		}

		public String getName() {
			return name;
		}

		public boolean  isDefault() {
			return isDefault;
		}

		public boolean  isLatest() {
			return isLatest;
		}

		MatomoReleaseSpec defaultRel() {
			isDefault = true;
			return this;
		}

		MatomoReleaseSpec latestRel() {
			isLatest = true;
			return this;
		}
	}
}
