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
     * @return future representing the request/response loop of fetching auth token
     */
    CompletableFuture<Void> fetchToken(PlcNextAuthConfig authClientConfig);

    /**
     * Returns the latest fetched and cached auth token.
     *
     * @return the cached token.
     */
    String getToken();

}