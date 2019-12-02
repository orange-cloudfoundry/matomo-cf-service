/**
 * 
 */
package com.orange.oss.matomocfservice.cfmgr;

/**
 * @author P. Déchamboux
 *
 */
public abstract class CloudFoundryMgrAbs implements CloudFoundryMgr {
	final static String MATOMO_ANPREFIX = "MATOMO_";
	final static String MATOMO_AUPREFIX = "M";

	public String getAppName(String appcode) {
		return MATOMO_ANPREFIX + appcode;
	}

	public String getAppUrlPrefix(String appcode) {
		return MATOMO_AUPREFIX + appcode;
	}
}
