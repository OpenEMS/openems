package io.openems.backend.authentication.api;

import java.util.concurrent.CompletableFuture;

import io.openems.backend.authentication.api.model.OAuthToken;
import io.openems.backend.authentication.api.model.response.InitiateConnectResponse;

public interface AuthUserAuthorizationCodeFlowService {

	/**
	 * Initiates the OAuth2 connect process.
	 * 
	 * @param oem         the OEM of the ui
	 * @param redirectUri the redirect URI to use
	 * @return a {@link CompletableFuture} with the result
	 */
	CompletableFuture<InitiateConnectResponse> initiateConnect(String oem, String redirectUri);

	/**
	 * Completes the OAuth2 connect process with the given identifier and code.
	 * 
	 * @param oem        the OEM of the ui
	 * @param identifier the identifier from the initiated connect
	 * @param code       the code received from the OAuth2 provider
	 * @return a {@link CompletableFuture} with the {@link OAuthToken}
	 */
	CompletableFuture<OAuthToken> tokenByCode(String oem, String identifier, String code);

	/**
	 * Completes the OAuth2 connect process with the given refresh token.
	 * 
	 * @param oem          the OEM of the ui
	 * @param refreshToken the refresh token received from the OAuth2 provider
	 * @return a {@link CompletableFuture} with the {@link OAuthToken}
	 */
	CompletableFuture<OAuthToken> tokenByRefreshToken(String oem, String refreshToken);

	/**
	 * Validates the given access token by checking its signature and expiration.
	 * 
	 * @param accessToken the access token to validate
	 * @return a {@link CompletableFuture} that completes when the validation is
	 *         done
	 */
	CompletableFuture<Void> validate(String accessToken);

}
