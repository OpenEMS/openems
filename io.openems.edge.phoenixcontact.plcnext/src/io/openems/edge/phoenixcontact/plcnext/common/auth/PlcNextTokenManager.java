package io.openems.edge.phoenixcontact.plcnext.common.auth;

/**
 * TODO
 */
public interface PlcNextTokenManager {

	String PATH_AUTH_TOKEN = "/auth-token";
	String PATH_ACCESS_TOKEN = "/access-token";

	/**
	 * Checks if a valid token has been fetched.
	 * 
	 * @return TRUE if token is valid, FALSE otherwise
	 */
	boolean hasValidToken();

	/**
	 * Initialize fetching valid JWT periodically
	 * 
	 * @param authClientConfig configuration to be used
	 */
	void fetchToken(PlcNextAuthConfig authClientConfig);

	/**
	 * 
	 * @return the cached token
	 */
	String getToken();

}