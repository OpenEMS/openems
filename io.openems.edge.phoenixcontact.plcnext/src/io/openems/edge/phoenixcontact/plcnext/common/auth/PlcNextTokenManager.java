package io.openems.edge.phoenixcontact.plcnext.common.auth;

import java.util.concurrent.CompletableFuture;

/**
 * Manages the handling of access tokens.
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
	 * Initialize fetching valid JWT periodically.
	 * 
	 * @param authClientConfig configuration to be used
	 * @return the token
	 */
	CompletableFuture<Void> fetchToken(PlcNextAuthConfig authClientConfig);

	/**
	 * Gets the cached token.
	 * 
	 * @return the cached token
	 */
	String getToken();

}