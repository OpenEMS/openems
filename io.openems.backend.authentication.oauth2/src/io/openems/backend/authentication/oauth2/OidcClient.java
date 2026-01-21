package io.openems.backend.authentication.oauth2;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.utils.FunctionUtils;

/**
 * Generic OIDC/OAuth2 client that uses endpoints from OIDC Discovery.
 *
 * <p>
 * This client is provider-agnostic and works with any OIDC-compliant provider
 * including Keycloak, Auth0, Okta, Azure AD, Google, and AWS Cognito.
 * </p>
 *
 * <p>
 * All endpoints are obtained from the {@link OidcDiscovery} document, ensuring
 * compatibility without hardcoded URLs.
 * </p>
 */
public final class OidcClient {

	private static final Logger LOG = LoggerFactory.getLogger(OidcClient.class);

	private final BridgeHttp bridgeHttp;
	private final OidcDiscovery discovery;

	/**
	 * Creates a new OIDC client.
	 *
	 * @param bridgeHttp the HTTP client
	 * @param discovery  the OIDC Discovery document
	 */
	public OidcClient(BridgeHttp bridgeHttp, OidcDiscovery discovery) {
		this.bridgeHttp = bridgeHttp;
		this.discovery = discovery;
	}

	/**
	 * Gets the OIDC Discovery document.
	 *
	 * @return the discovery document
	 */
	public OidcDiscovery getDiscovery() {
		return this.discovery;
	}

	// ===== Token Operations =====

	/**
	 * Gets a token using the client credentials grant type (for service accounts).
	 *
	 * @param clientId     the client ID
	 * @param clientSecret the client secret
	 * @return a future with the token response
	 */
	public CompletableFuture<TokenResponse> getTokenWithClientCredentials(String clientId, String clientSecret) {
		return this.requestToken(Map.of(//
				"grant_type", "client_credentials", //
				"client_id", clientId, //
				"client_secret", clientSecret));
	}

	/**
	 * Gets a token using the password grant type (Resource Owner Password
	 * Credentials).
	 *
	 * @param clientId     the client ID
	 * @param clientSecret the client secret
	 * @param username     the username
	 * @param password     the password
	 * @return a future with the token response
	 */
	public CompletableFuture<TokenResponse> getTokenWithPassword(String clientId, String clientSecret, String username,
			String password) {
		return this.requestToken(Map.of(//
				"grant_type", "password", //
				"client_id", clientId, //
				"client_secret", clientSecret, //
				"username", username, //
				"password", password));
	}

	/**
	 * Exchanges an authorization code for tokens.
	 *
	 * @param clientId     the client ID
	 * @param clientSecret the client secret
	 * @param code         the authorization code
	 * @param redirectUri  the redirect URI used in the authorization request
	 * @param codeVerifier the PKCE code verifier (optional, null if not using PKCE)
	 * @return a future with the token response
	 */
	public CompletableFuture<TokenResponse> exchangeCode(String clientId, String clientSecret, String code,
			String redirectUri, String codeVerifier) {
		final var params = new HashMap<String, String>();
		params.put("grant_type", "authorization_code");
		params.put("client_id", clientId);
		params.put("client_secret", clientSecret);
		params.put("code", code);
		params.put("redirect_uri", redirectUri);
		if (codeVerifier != null) {
			params.put("code_verifier", codeVerifier);
		}
		return this.requestToken(params);
	}

	/**
	 * Refreshes an access token using a refresh token.
	 *
	 * @param clientId     the client ID
	 * @param clientSecret the client secret
	 * @param refreshToken the refresh token
	 * @return a future with the token response
	 */
	public CompletableFuture<TokenResponse> refreshToken(String clientId, String clientSecret, String refreshToken) {
		return this.requestToken(Map.of(//
				"grant_type", "refresh_token", //
				"client_id", clientId, //
				"client_secret", clientSecret, //
				"refresh_token", refreshToken));
	}

	/**
	 * Internal method to request tokens from the token endpoint.
	 */
	private CompletableFuture<TokenResponse> requestToken(Map<String, String> params) {
		final var tokenEndpoint = this.discovery.getTokenEndpoint();
		LOG.debug("Requesting token from: {}", tokenEndpoint);

		return this.bridgeHttp.requestJson(BridgeHttp.Endpoint.create(tokenEndpoint) //
				.setBodyFormEncoded(params) //
				.build()) //
				.thenApply(response -> {
					final var json = response.data().getAsJsonObject();
					return TokenResponse.fromJson(json);
				});
	}

	// ===== Logout/Session Management =====

	/**
	 * Logs out by ending the session at the OIDC provider.
	 *
	 * <p>
	 * Uses the end_session_endpoint if available, otherwise falls back to the
	 * revocation_endpoint.
	 * </p>
	 *
	 * @param clientId     the client ID
	 * @param clientSecret the client secret
	 * @param refreshToken the refresh token to revoke
	 * @return a future that completes when logout is done
	 */
	public CompletableFuture<Void> logout(String clientId, String clientSecret, String refreshToken) {
		// Prefer end_session_endpoint, fallback to revocation_endpoint
		final var endSessionEndpoint = this.discovery.getEndSessionEndpoint();
		final var revocationEndpoint = this.discovery.getRevocationEndpoint();

		if (endSessionEndpoint.isPresent()) {
			LOG.debug("Logging out via end_session_endpoint: {}", endSessionEndpoint.get());
			return this.logoutWithEndSession(endSessionEndpoint.get(), clientId, clientSecret, refreshToken);
		} else if (revocationEndpoint.isPresent()) {
			LOG.debug("Logging out via revocation_endpoint: {}", revocationEndpoint.get());
			return this.revokeToken(revocationEndpoint.get(), clientId, clientSecret, refreshToken);
		} else {
			LOG.warn("No logout endpoint available - skipping provider logout");
			return CompletableFuture.completedFuture(null);
		}
	}

	/**
	 * Logout using the OIDC end_session_endpoint.
	 */
	private CompletableFuture<Void> logoutWithEndSession(String endpoint, String clientId, String clientSecret,
			String refreshToken) {
		return this.bridgeHttp.request(BridgeHttp.Endpoint.create(endpoint) //
				.setBodyFormEncoded(Map.of(//
						"client_id", clientId, //
						"client_secret", clientSecret, //
						"refresh_token", refreshToken)) //
				.build()) //
				.thenAccept(FunctionUtils::doNothing);
	}

	/**
	 * Revoke a token using the OAuth2 revocation_endpoint (RFC 7009).
	 */
	private CompletableFuture<Void> revokeToken(String endpoint, String clientId, String clientSecret,
			String refreshToken) {
		return this.bridgeHttp.request(BridgeHttp.Endpoint.create(endpoint) //
				.setBodyFormEncoded(Map.of(//
						"client_id", clientId, //
						"client_secret", clientSecret, //
						"token", refreshToken, //
						"token_type_hint", "refresh_token")) //
				.build()) //
				.thenAccept(FunctionUtils::doNothing);
	}

	// ===== Token Response =====

	/**
	 * Represents a token response from the OIDC provider.
	 */
	public record TokenResponse(//
			String accessToken, //
			String refreshToken, //
			String idToken, //
			String tokenType, //
			int expiresIn, //
			String scope //
	) {

		/**
		 * Parses a token response from JSON.
		 */
		public static TokenResponse fromJson(JsonObject json) {
			return new TokenResponse(//
					getStringOrNull(json, "access_token"), //
					getStringOrNull(json, "refresh_token"), //
					getStringOrNull(json, "id_token"), //
					getStringOrNull(json, "token_type"), //
					json.has("expires_in") ? json.get("expires_in").getAsInt() : 0, //
					getStringOrNull(json, "scope") //
			);
		}

		private static String getStringOrNull(JsonObject json, String key) {
			return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : null;
		}
	}

}
