package io.openems.edge.common.oauth;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.openems.common.jsonrpc.response.OAuthRegistryGetInitMetadataResponse;
import io.openems.common.jsonrpc.response.OAuthRegistryTokenResponse;

public interface OAuthBackend {

	record OAuthClientBackendRegistration(String identifier, List<String> scopes) {

	}

	/**
	 * Gets init metadata.
	 *
	 * @param identifier the oauth identifier
	 * @return the metadata
	 */
	CompletableFuture<OAuthRegistryGetInitMetadataResponse.OAuthInitMetadata> getInitMetadata(String identifier);

	/**
	 * Fetches tokens from a refresh token.
	 *
	 * @param backendRegistration the backend registration
	 * @param refreshToken        the refresh token
	 * @return a future with the new tokens
	 */
	CompletableFuture<OAuthRegistryTokenResponse.OAuthToken> fetchTokensFromRefreshToken(
			OAuthClientBackendRegistration backendRegistration, String refreshToken);

	/**
	 * Fetches tokens from a code.
	 *
	 * @param backendRegistration the backend registration
	 * @param code                the code
	 * @param codeVerifier        the code verifier if present
	 * @return the new tokens
	 */
	CompletableFuture<OAuthRegistryTokenResponse.OAuthToken> fetchTokensFromCode(
			OAuthClientBackendRegistration backendRegistration, String code, String codeVerifier);

}
