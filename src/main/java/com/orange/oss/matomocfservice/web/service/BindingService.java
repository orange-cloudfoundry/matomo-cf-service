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

import javax.persistence.EntityManager;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.orange.oss.matomocfservice.cfmgr.CloudFoundryMgrProperties;
import com.orange.oss.matomocfservice.web.domain.PBinding;
import com.orange.oss.matomocfservice.web.domain.PMatomoInstance;
import com.orange.oss.matomocfservice.web.domain.POperationStatus;
import com.orange.oss.matomocfservice.web.domain.PPlatform;
import com.orange.oss.matomocfservice.web.repository.PBindingRepository;
import com.orange.oss.matomocfservice.web.repository.PMatomoInstanceRepository;

import io.jsonwebtoken.lang.Assert;

/**
 * @author P. Déchamboux
 *
 */
@Service
public class BindingService extends OperationStatusService {
	private final static Logger LOGGER = LoggerFactory.getLogger(BindingService.class);
	public final static String CRED_MATOMOURL = "mcfs-matomoUrl";
	public final static String CRED_SITEID = "mcfs-siteId";
	public final static String CRED_USERNAME = "mcfs-userName";
	public final static String CRED_PASSWORD = "mcfs-password";
	public final static String PARAM_SITENAME = "siteName";
	public final static String PARAM_TRACKEDURL = "trackedUrl";
	public final static String PARAM_ADMINEMAIL = "adminEmail";
	
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

	public String createBinding(String bindid, String instid, String appid, Map<String, Object> parameters) {
		Assert.notNull(bindid, "binding uuid mustn't be null");
		Assert.notNull(instid, "instance uuid mustn't be null");
		if (appid == null) {
			appid = "";
		}
		LOGGER.debug("SERV::createBinding: bindId={}, instid={}, appid={}", bindid, instid, appid);
		EntityManager em = beginTx();
		PBinding pb = null;
		try {
			Optional<PBinding> opb = pbindingRepo.findById(bindid);
			if (opb.isPresent()) {
				return "Matomo Instance Binding with ID=" + bindid + " already exists";
			}
			Optional<PMatomoInstance> opmi = pmatomoInstanceRepo.findById(instid);
			if (!opmi.isPresent()) {
				return "Cannot bind: Matomo service instance does not exist!!";
			}
			LOGGER.debug("PARAMETERS: " + parameters.toString());
			String sn = (String) parameters.get(PARAM_SITENAME);
			if (sn == null) {
				return "Cannot bind: site name parameter should be provided";
			}
			String tu = (String) parameters.get(PARAM_TRACKEDURL);
			if (tu == null) {
				return "Cannot bind: tracked URL parameter should be provided";
			}
			String am = (String) parameters.get(PARAM_ADMINEMAIL);
			if (am == null) {
				return "Cannot bind: admin email parameter should be provided";
			}
			for (PBinding epb : pbindingRepo.findByTrackedUrlAndPmatomoInstanceOrderByCreateTimeDesc(tu, opmi.get())) {
				if (epb.getSiteName().equals(sn)) {
					if (epb.isDeleted()) {
						LOGGER.debug("Reactivate an older binding with the same site name");
						pb = new PBinding(bindid, opmi.get(), appid, sn, tu, am, opmi.get().getPlatform(),
								getMatomoUrl(opmi.get().getUuid()), epb.getSiteId(), epb.getUserName(), epb.getPassword());
						pbindingRepo.save(pb);
						break;
					} else {
						return "Cannot bind: binding already exist with this site name";
					}
				}
				if (epb.getAppId().contentEquals("")) {
					continue;
				}
				if (epb.getAppId().equals(appid)) {
					return "Cannot bind: application already bound to this instance";
				}
			}
			if (pb == null) {
				pb = new PBinding(bindid, opmi.get(), appid, sn, tu, am, opmi.get().getPlatform(),
						getMatomoUrl(opmi.get().getUuid()));
				pbindingRepo.save(pb);
				defineNewMatomoSite(opmi.get(), pb, (String) parameters.get(PARAM_SITENAME),
						(String) parameters.get(PARAM_TRACKEDURL), (String) parameters.get(PARAM_ADMINEMAIL));
			}
			pb.setLastOperationState(OperationState.SUCCEEDED);
			pbindingRepo.save(pb);
		} catch (RuntimeException e) {
			if (pb != null) {
				pb.setLastOperationState(OperationState.FAILED);
				pbindingRepo.save(pb);
			}
		} finally {
			commitTx(em);
		}
		return null;
	}

	public String deleteBinding(String bindid, String instanceId) {
		LOGGER.debug("SERV::deleteBinding: bindingId={} instanceId={}", bindid, instanceId);
		EntityManager em = beginTx();
		PBinding pb = null;
		try {
			Optional<PBinding> opb = pbindingRepo.findById(bindid);
			if (!opb.isPresent()) {
				return "Error: Matomo service instance binding does not exist.";
			}
			pb = opb.get();
			if (pb.getLastOperationState() == OperationState.IN_PROGRESS) {
				return "Error: cannot delete Matomo service instance binding with ID=" + pb.getUuid() + ": operation already in progress.";
			}
			pb.setLastOperation(POperationStatus.OpCode.DELETE_SERVICE_INSTANCE_APP_BINDING);
			pb.setLastOperationState(OperationState.IN_PROGRESS);
			pbindingRepo.save(pb);
//			deleteMatomoSite(pb);
			pb.markDeleted();
			LOGGER.debug("SERV::deleteBinding: marked deleted!!");
			pb.setLastOperationState(OperationState.SUCCEEDED);
			pbindingRepo.save(pb);
		} catch (Exception e) {
			if (pb != null) {
				pb.setLastOperationState(OperationState.FAILED);
				pbindingRepo.save(pb);
				return "Error: exception while deleting binding (" + e.getMessage() + ")";
			}
		} finally {
			commitTx(em);
		}
		return null;
	}

	private void defineNewMatomoSite(PMatomoInstance pmi, PBinding pb, String sitename, String trackedurl, String adminemail) {
		LOGGER.debug("SERV::defineNewMatomoSite: instId={}, siteName{}, trackedUrl={}", pmi.getUuid(), sitename, trackedurl);
		RestTemplate restTemplate = new RestTemplate();
		try {
			URI uri = new URI("https://" + getMatomoUrl(pb.getPMatomoInstance().getUuid()) + "/index.php");
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

//	private void deleteMatomoSite(PBinding pb) {
//		LOGGER.debug("SERV::deleteMatomoSite: instId={}", pb.getPMatomoInstance().getId());
//		RestTemplate restTemplate = new RestTemplate();
//		try {
//			URI uri = new URI("https://" + getMatomoUrl(pb.getPMatomoInstance().getId()) + "/index.php");
//			MultipartBodyBuilder mbb = new MultipartBodyBuilder();
//			mbb.part("module", "API");
//			mbb.part("method", "SitesManager.deleteSite");
//			mbb.part("idSite", Integer.toString(pb.getSiteId()));
//			mbb.part("format", "json");
//			mbb.part("token_auth", pb.getPMatomoInstance().getTokenAuth());
//			String res = restTemplate.postForObject(uri, mbb.build(), String.class);
//			LOGGER.debug("DeleteSite Response: " + res);
//			mbb = new MultipartBodyBuilder();
//			mbb.part("module", "API");
//			mbb.part("method", "UsersManager.deleteUser");
//			mbb.part("userLogin", pb.getUserName());
//			mbb.part("format", "json");
//			mbb.part("token_auth", pb.getPMatomoInstance().getTokenAuth());
//			res = restTemplate.postForObject(uri, mbb.build(), String.class);
//			LOGGER.debug("DeleteUser Response: " + res);
//		} catch (RestClientException e) {
//			throw new RuntimeException("Fail to delete a site when unbinding.", e);
//		} catch (URISyntaxException e) {
//			throw new RuntimeException("Fail to delete a site when unbinding.", e);
//		}
//	}

	private String getMatomoUrl(String miid) {
		return miid + "." + properties.getDomain();
	}
}
