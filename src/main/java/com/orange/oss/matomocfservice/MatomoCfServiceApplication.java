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

package com.orange.oss.matomocfservice;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author P. Déchamboux
 *
 */
@SpringBootApplication
public class MatomoCfServiceApplication {
	private static final Logger LOGGER = LoggerFactory.getLogger(MatomoCfServiceApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(MatomoCfServiceApplication.class, args);
		Manifest manifest = getManifest();
		if (manifest != null) {
			String title = (String)manifest.getMainAttributes().getValue("Implementation-Title");
			String version = (String)manifest.getMainAttributes().getValue("Implementation-Version");
			LOGGER.info("--------------------- Starting {} version {} ---------------------", title, version);
		} else {
			LOGGER.info("--------------------- Starting matomo-cf-service version unknown ---------------------");
		}
	}

	private static Manifest getManifest() {
	    Enumeration<URL> resEnum;
	    try {
	        resEnum = Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF");
	        while (resEnum.hasMoreElements()) {
	            try {
	                URL url = resEnum.nextElement();
	                if (url.toString().equals("file:/home/vcap/app/META-INF/MANIFEST.MF")) {
	                    InputStream is = url.openStream();
	                    if (is != null) {
	                        Manifest manifest = new Manifest(is);
	                        return manifest;
	                    }
	                }
	            }
	            catch (Exception e) {
	                // Silently ignore wrong manifests on classpath?
	            }
	        }
	    } catch (IOException e1) {
	        // Silently ignore wrong manifests on classpath?
	    }
	    return null;
	}
}
