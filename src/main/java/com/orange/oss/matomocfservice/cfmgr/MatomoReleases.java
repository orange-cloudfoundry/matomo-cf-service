/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.cfmgr;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.orange.oss.matomocfservice.config.ApplicationConfiguration;

/**
 * @author P. Déchamboux
 *
 */
@Service
public class MatomoReleases {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	private String defaultRel;
	private Path tempDir;
	private List<MatomoReleaseSpec> releases = new ArrayList<MatomoReleaseSpec>();
	private static final String RELEASEPATH = "/home/vcap/app/BOOT-INF/classes/static/matomo-releases";
	private static final String VERSIONSFILE = File.separator + "Versions";
	private static final String DEFVERSIONFILE = File.separator + "DefaultVersion";
	@Autowired
	ApplicationConfiguration applicationConfiguration;

	public void initialize() {
		LOGGER.debug("CFMGR::MatomoReleases: initialize");
		try {
			this.defaultRel = null;
			this.tempDir = Files.createTempDirectory("matomo");
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
		} catch (IOException e) {
			LOGGER.error("CFMGR::MatomoReleases: cannot create matomo temporary directory -> " + e.getMessage());
		}
		getReleaseList(true);
	}

	public List<MatomoReleaseSpec> getReleaseList(boolean forcereload) {
		LOGGER.debug("CFMGR::getReleaseList: forcedload=" + Boolean.toString(forcereload));
		try {
			if (forcereload) {
				this.defaultRel = null;
				this.releases = new ArrayList<MatomoReleaseSpec>();
			}
			if (this.defaultRel != null) {
				return this.releases;
			}
			defaultRel = new String(Files.readAllBytes(Paths.get(RELEASEPATH + DEFVERSIONFILE)));
			LOGGER.debug("CFMGR::getReleaseList: defaultVersion=" + defaultRel);
			this.releases.add(new MatomoReleaseSpec(defaultRel).defaultRel());
			String versions = new String(Files.readAllBytes(Paths.get(RELEASEPATH + VERSIONSFILE)));
			LOGGER.debug("CFMGR::getReleaseList: versions=" + versions);
			for (String vers : versions.split(";")) {
				this.releases.add(new MatomoReleaseSpec(vers));
			}
		} catch (IOException e) {
			// TODO
			LOGGER.debug("CFMGR::getReleaseList: problem while retrieving Matomo versions from release service -> " + e.getMessage());
			e.printStackTrace();
		}
		return this.releases;
	}

	public String getDefaultReleaseName() {
		LOGGER.debug("CFMGR::getDefaultReleaseName");
		// defaultRel should not be null here
		return defaultRel;
	}

	public String getVersionPath(String version, String instId) {
		return tempDir.toString() + File.separator + instId + "-" + version;
	}

	public void activateVersionPath(String version, String instId) {
		LOGGER.debug("CFMGR::activateVersionPath: version={}, instId={}", version, instId);
		String versdir = RELEASEPATH + File.separator + version;
		String instdir = getVersionPath(version, instId);
		try {
			// create a copy of the version dir for the instance
			createLinkedDir(versdir, instdir);
		} catch (IOException e) {
			throw new RuntimeException("IO pb in CFMGR::getVersionPath", e);
		}		
	}

	public class MatomoReleaseSpec {
		private String versName;
		private boolean versDefault = false;

		MatomoReleaseSpec(String n) {
			versName = n;
		}

		public String getName() {
			return versName;
		}

		public boolean  isDefault() {
			return versDefault;
		}

		MatomoReleaseSpec defaultRel() {
			versDefault = true;
			return this;
		}
	}

	private void createLinkedDir(String sourcedir, String targetdir) throws IOException {
		if (new File(targetdir).exists()) {
			return;
		}
		Path sourcePath = Paths.get(sourcedir);
		Path targetPath = Paths.get(targetdir);
		Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
					throws IOException {
				Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
				Files.createLink(targetPath.resolve(sourcePath.relativize(file)), file);
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
