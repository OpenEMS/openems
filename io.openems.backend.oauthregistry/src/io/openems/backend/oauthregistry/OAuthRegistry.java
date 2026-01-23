package io.openems.backend.oauthregistry;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface OAuthRegistry {

	record OAuthTokens(String accessToken, String refreshToken) {

	}

	record OAuthInitMetadata(String authenticationUrl, String clientId, String redirectUrl) {

	}

	/**
	 * Gets init metadata.
	 *
	 * @param identifier the oauth identifier
	 * @return the metadata
	 */
	CompletableFuture<OAuthInitMetadata> getInitMetadata(String identifier);

	/**
	 * Fetches tokens from a refresh accessToken.
	 *
	 * @param identifier   the oauth identifier
	 * @param refreshToken the refresh accessToken
	 * @param scopes       the scopes
	 * @return a future with the new tokens
	 */
	CompletableFuture<OAuthTokens> fetchTokensFromRefreshToken(String identifier, String refreshToken,
			List<String> scopes);

	/**
	 * Fetches tokens from a code.
	 *
	 * @param identifier   the oauth identifier
	 * @param code         the code
	 * @param scopes       the scopes
	 * @param codeVerifier the code verifier if present
	 * @return the new tokens
	 */
	CompletableFuture<OAuthTokens> fetchTokensFromCode(String identifier, String code, List<String> scopes,
			String codeVerifier);

}
