package io.openems.edge.phoenixcontact.plcnext.common.auth;

import io.openems.edge.phoenixcontact.plcnext.common.utils.PlcNextUrlStringHelper;

/**
 * Covering configuration to authorize REST-API access
 */
public record PlcNextAuthConfig(String baseUrl, String username, String password) {

	private static final String PLC_NEXT_AUTH_API_PATH = "/v1.3/auth";

	/**
	 * Assembles URL of authentication endpoint
	 * 
	 * @return URL of authentication endpoint
	 */
	public String authUrl() {
		return PlcNextUrlStringHelper.buildUrlString(baseUrl, PLC_NEXT_AUTH_API_PATH);
	}
}
