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

package com.orange.oss.matomocfservice.web.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.orange.oss.matomocfservice.api.model.OpCode;
import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgrProperties;
import com.orange.oss.matomocfservice.web.domain.PBinding;
import com.orange.oss.matomocfservice.web.domain.PMatomoInstance;
import com.orange.oss.matomocfservice.web.domain.PPlatform;
import com.orange.oss.matomocfservice.web.repository.PBindingRepository;
import com.orange.oss.matomocfservice.web.repository.PMatomoInstanceRepository;

/**
 * @author P. Déchamboux
 *
 */
@Service
public class BindingService extends OperationStatusService {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	public final static String CRED_MATOMOURL = "mcfs-matomoUrl";
	public final static String CRED_SITEID = "mcfs-siteId";
	public final static String CRED_USERNAME = "mcfs-userName";
	public final static String CRED_PASSWORD = "mcfs-password";
	private final static String PARAM_SITENAME = "siteName";
	private final static String PARAM_TRACKEDURL = "trackedUrl";
	private final static String PARAM_ADMINEMAIL = "adminEmail";
	
	@Autowired
	private PBindingRepository pbindingRepo;
	@Autowired
	private PMatomoInstanceRepository pmatomoInstanceRepo;
	@Autowired
	private CloudFoundryMgrProperties properties;

	public Map<String, Object> getCredentials(String bindid, String instid) {
		Map<String, Object> credentials = new HashMap<String, Object>();
		Optional<PBinding> opb = pbindingRepo.findById(bindid);
		if (! opb.isPresent()) {
			throw new RuntimeException("Cannot retrieve credentials of non existing binding.");
		}
		credentials.put(CRED_MATOMOURL, opb.get().getMatomoUrl());
		credentials.put(CRED_SITEID, opb.get().getSiteId());
		credentials.put(CRED_USERNAME, opb.get().getUserName());
		credentials.put(CRED_PASSWORD, opb.get().getPassword());
		return credentials;
	}

	public Map<String, Object> getParameters(String bindid, String instid) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		Optional<PBinding> opb = pbindingRepo.findById(bindid);
		if (! opb.isPresent()) {
			throw new RuntimeException("Cannot retrieve parameters of non existing binding.");
		}
		parameters.put(PARAM_SITENAME, opb.get().getSiteName());
		parameters.put(PARAM_TRACKEDURL, opb.get().getTrackedUrl());
		parameters.put(PARAM_ADMINEMAIL, opb.get().getAdminEmail());
		return parameters;
	}

	public boolean createBinding(String bindid, String instid, String appid, Map<String, Object> parameters) {
		LOGGER.debug("SERV::createBinding: bindId={}, instid={}, appid={}", bindid, instid, appid);
		Optional<PBinding> opb = pbindingRepo.findById(bindid);
		if (opb.isPresent()) {
			return true;
		}
		Optional<PMatomoInstance> opmi = pmatomoInstanceRepo.findById(instid);
		if (!opmi.isPresent()) {
			throw new RuntimeException("Cannot bind: Matomo service instance does not exist!!");
		}
		PBinding pb = null;
		LOGGER.debug("PARAMETERS: " + parameters.toString());
		String sn = (String) parameters.get(PARAM_SITENAME);
		if (sn == null) {
			throw new RuntimeException("Cannot bind: site name parameter should be provided");
		}
		String tu = (String) parameters.get(PARAM_TRACKEDURL);
		if (tu == null) {
			throw new RuntimeException("Cannot bind: tracked URL parameter should be provided");
		}
		String am = (String) parameters.get(PARAM_ADMINEMAIL);
		if (am == null) {
			throw new RuntimeException("Cannot bind: admin email parameter should be provided");
		}
		try {
			opb = pbindingRepo.findByTrackedUrl(tu);
			if (opb.isPresent()) {
				if (!opb.get().getSiteName().equals(sn)) {
					LOGGER.warn("A binding already exists for this tracked URL with another site name: cannot change, keep existing one <{}>!!", opb.get().getSiteName());
				}
				if (!opb.get().getAdminEmail().equals(am)) {
					LOGGER.warn("A binding already exists for this tracked URL with another admin email: cannot change, keep existing one <{}>!!", opb.get().getAdminEmail());
				}
				return true;
			}
			pb = new PBinding(
					bindid,
					opmi.get(),
					appid,
					sn,
					tu,
					am,
					opmi.get().getPlatform(),
					getMatomoUrl(opmi.get().getId()));
			pbindingRepo.save(pb);
			defineNewMatomoSite(opmi.get(), pb, (String) parameters.get(PARAM_SITENAME), (String) parameters.get(PARAM_TRACKEDURL), (String) parameters.get(PARAM_ADMINEMAIL));
			pb.setLastOperation(OpCode.CREATE);
			pb.setLastOperationState(OperationState.SUCCEEDED);
			pbindingRepo.save(pb);
		} catch (RuntimeException e) {
			if (pb != null) {
				pb.setLastOperation(OpCode.CREATE);
				pb.setLastOperationState(OperationState.FAILED);
				pbindingRepo.save(pb);
			}
		}
		return false;
	}

	public String deleteBinding(String platformId, String instanceId) {
		LOGGER.debug("SERV::deleteBinding: platformId={} instanceId={}", platformId, instanceId);
		PPlatform ppf = getPPlatform(platformId);
		Optional<PBinding> opb = pbindingRepo.findById(instanceId);
		if (!opb.isPresent()) {
			return "Error: Matomo service instance binding does not exist.";
		}
		PBinding pb = opb.get();
		if (pb.getPlatform() != ppf) {
			return "Error: wrong platform with ID=" + platformId + " for Matomo service instance binding with ID=" + instanceId + ".";
		}
		if (pb.getLastOperationState() == OperationState.IN_PROGRESS) {
			return "Error: cannot delete Matomo service instance binding with ID=" + pb.getId() + ": operation already in progress.";
		}
		pb.setLastOperation(OpCode.DELETE);
//		pb.setLastOperationState(OperationState.IN_PROGRESS);
//		pbindingRepo.save(pb);
//		deleteMatomoSite(pb);
		pb.setLastOperationState(OperationState.SUCCEEDED);
		pbindingRepo.save(pb);
		return null;
	}

	private void defineNewMatomoSite(PMatomoInstance pmi, PBinding pb, String sitename, String trackedurl, String adminemail) {
		LOGGER.debug("SERV::defineNewMatomoSite: instId={}, siteName{}, trackedUrl={}", pmi.getId(), sitename, trackedurl);
		RestTemplate restTemplate = new RestTemplate();
		try {
			URI uri = new URI("https://" + getMatomoUrl(pb.getPMatomoInstance().getId()) + "/index.php");
			MultipartBodyBuilder mbb = new MultipartBodyBuilder();
			mbb.part("module", "API");
			mbb.part("method", "SitesManager.addSite");
			mbb.part("siteName", sitename);
			mbb.part("urls", trackedurl);
			mbb.part("format", "json");
			mbb.part("token_auth", pmi.getTokenAuth());
			String res = restTemplate.postForObject(uri, mbb.build(), String.class);
			LOGGER.debug("AddSite Response: " + res);
			JSONObject jres = new JSONObject(res);
			pb.setSiteId((Integer) jres.get("value"));
			mbb = new MultipartBodyBuilder();
			mbb.part("module", "API");
			mbb.part("method", "UsersManager.addUser");
			mbb.part("userLogin", pb.getUserName());
			mbb.part("password", pb.getPassword());
			mbb.part("email", adminemail);
			mbb.part("initialIdSite", "");
			mbb.part("format", "json");
			mbb.part("token_auth", pmi.getTokenAuth());
			res = restTemplate.postForObject(uri, mbb.build(), String.class);
			LOGGER.debug("AddUser Response: " + res);
			mbb = new MultipartBodyBuilder();
			mbb.part("module", "API");
			mbb.part("method", "UsersManager.setUserAccess");
			mbb.part("userLogin", pb.getUserName());
			mbb.part("access", "admin");
			mbb.part("idSites", Integer.toString(pb.getSiteId()));
			mbb.part("format", "json");
			mbb.part("token_auth", pmi.getTokenAuth());
			res = restTemplate.postForObject(uri, mbb.build(), String.class);
			LOGGER.debug("SetUserAccess Response: " + res);
		} catch (RestClientException e) {
			throw new RuntimeException("Fail to create a new site when binding.", e);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Fail to create a new site when binding.", e);
		}
	}

	private void deleteMatomoSite(PBinding pb) {
		LOGGER.debug("SERV::deleteMatomoSite: instId={}", pb.getPMatomoInstance().getId());
		RestTemplate restTemplate = new RestTemplate();
		try {
			URI uri = new URI("https://" + getMatomoUrl(pb.getPMatomoInstance().getId()) + "/index.php");
			MultipartBodyBuilder mbb = new MultipartBodyBuilder();
			mbb.part("module", "API");
			mbb.part("method", "SitesManager.deleteSite");
			mbb.part("idSite", Integer.toString(pb.getSiteId()));
			mbb.part("format", "json");
			mbb.part("token_auth", pb.getPMatomoInstance().getTokenAuth());
			String res = restTemplate.postForObject(uri, mbb.build(), String.class);
			LOGGER.debug("DeleteSite Response: " + res);
			mbb = new MultipartBodyBuilder();
			mbb.part("module", "API");
			mbb.part("method", "UsersManager.deleteUser");
			mbb.part("userLogin", pb.getUserName());
			mbb.part("format", "json");
			mbb.part("token_auth", pb.getPMatomoInstance().getTokenAuth());
			res = restTemplate.postForObject(uri, mbb.build(), String.class);
			LOGGER.debug("DeleteUser Response: " + res);
		} catch (RestClientException e) {
			throw new RuntimeException("Fail to delete a site when unbinding.", e);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Fail to delete a site when unbinding.", e);
		}
	}

	private String getMatomoUrl(String miid) {
		return miid + "." + properties.getDomain();
	}
}
