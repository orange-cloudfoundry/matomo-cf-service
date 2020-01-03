/**
 * 
 */
package com.orange.oss.matomocfservice.cfmgr;

import com.orange.oss.matomocfservice.servicebroker.ServiceCatalogConfiguration;

/**
 * @author P. Déchamboux
 *
 */
public abstract class CloudFoundryMgrAbs implements CloudFoundryMgr {
	final static String MATOMO_ANPREFIX = "MATOMO_";
	final static String MATOMO_AUPREFIX = "M";
	final static String MATOMO_DEDDBPREFIX = "MCFS";

	public String getAppName(String appcode) {
		return MATOMO_ANPREFIX + appcode;
	}

	public String getTablePrefix(String appcode, String planid) {
		if (planid.equals(ServiceCatalogConfiguration.PLANGLOBSHARDB_UUID)) {
			return MATOMO_AUPREFIX + appcode;
		}
		// else planid is either PLANMATOMOSHARDB_UUID or PLANDEDICATEDDB_UUID
		return MATOMO_DEDDBPREFIX;
	}
}
