package io.openems.backend.authentication.oauth2;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.utils.JsonUtils;

/**
 * OIDC Discovery client for fetching OpenID Connect configuration.
 *
 * <p>
 * This class fetches the OpenID Connect Discovery document from the well-known
 * configuration endpoint and provides access to all standard OIDC endpoints.
 * This enables provider-agnostic OAuth2/OIDC integration.
 * </p>
 *
 * <p>
 * Supported providers include: Keycloak, Auth0, Okta, Azure AD, Google,
 * AWS Cognito, and any other OIDC-compliant provider.
 * </p>
 *
 * @see <a href="https://openid.net/specs/openid-connect-discovery-1_0.html">
 *      OpenID Connect Discovery 1.0</a>
 */
public final class OidcDiscovery {

	private static final Logger LOG = LoggerFactory.getLogger(OidcDiscovery.class);
	private static final String WELL_KNOWN_PATH = "/.well-known/openid-configuration";

	// Core OIDC endpoints
	private final String issuer;
	private final String authorizationEndpoint;
	private final String tokenEndpoint;
	private final String userinfoEndpoint;
	private final String jwksUri;

	// Session management endpoints
	private final String endSessionEndpoint;
	private final String revocationEndpoint;

	// Optional endpoints
	private final String registrationEndpoint;
	private final String introspectionEndpoint;

	// Supported features
	private final List<String> grantTypesSupported;
	private final List<String> responseTypesSupported;
	private final List<String> scopesSupported;

	private OidcDiscovery(Builder builder) {
		this.issuer = builder.issuer;
		this.authorizationEndpoint = builder.authorizationEndpoint;
		this.tokenEndpoint = builder.tokenEndpoint;
		this.userinfoEndpoint = builder.userinfoEndpoint;
		this.jwksUri = builder.jwksUri;
		this.endSessionEndpoint = builder.endSessionEndpoint;
		this.revocationEndpoint = builder.revocationEndpoint;
		this.registrationEndpoint = builder.registrationEndpoint;
		this.introspectionEndpoint = builder.introspectionEndpoint;
		this.grantTypesSupported = builder.grantTypesSupported;
		this.responseTypesSupported = builder.responseTypesSupported;
		this.scopesSupported = builder.scopesSupported;
	}

	/**
	 * Fetches the OIDC Discovery document from the issuer's well-known endpoint.
	 *
	 * @param bridgeHttp the {@link BridgeHttp} instance for HTTP requests
	 * @param issuerUrl  the issuer URL (e.g.,
	 *                   https://keycloak.example.com/realms/myrealm)
	 * @return a {@link CompletableFuture} that completes with the
	 *         {@link OidcDiscovery}
	 */
	public static CompletableFuture<OidcDiscovery> fetch(BridgeHttp bridgeHttp, String issuerUrl) {
		final var normalizedIssuer = normalizeIssuerUrl(issuerUrl);
		final var discoveryUrl = normalizedIssuer + WELL_KNOWN_PATH;

		LOG.info("Fetching OIDC Discovery from: {}", discoveryUrl);

		return bridgeHttp.requestJson(BridgeHttp.Endpoint.create(discoveryUrl).build()) //
				.thenApply(response -> {
					final var json = response.data().getAsJsonObject();

					final var builder = new Builder()
							.issuer(JsonUtils.getAsOptionalString(json, "issuer").orElse(normalizedIssuer))
							.authorizationEndpoint(JsonUtils.getAsOptionalString(json, "authorization_endpoint").orElse(null))
							.tokenEndpoint(JsonUtils.getAsOptionalString(json, "token_endpoint").orElse(null))
							.userinfoEndpoint(JsonUtils.getAsOptionalString(json, "userinfo_endpoint").orElse(null))
							.jwksUri(JsonUtils.getAsOptionalString(json, "jwks_uri").orElse(null))
							.endSessionEndpoint(JsonUtils.getAsOptionalString(json, "end_session_endpoint").orElse(null))
							.revocationEndpoint(JsonUtils.getAsOptionalString(json, "revocation_endpoint").orElse(null))
							.registrationEndpoint(JsonUtils.getAsOptionalString(json, "registration_endpoint").orElse(null))
							.introspectionEndpoint(JsonUtils.getAsOptionalString(json, "introspection_endpoint").orElse(null))
							.grantTypesSupported(jsonArrayToList(json.getAsJsonArray("grant_types_supported")))
							.responseTypesSupported(jsonArrayToList(json.getAsJsonArray("response_types_supported")))
							.scopesSupported(jsonArrayToList(json.getAsJsonArray("scopes_supported")));

					final var discovery = builder.build();

					LOG.info("OIDC Discovery loaded successfully for issuer: {}", discovery.issuer);
					LOG.info("  authorization_endpoint: {}", discovery.authorizationEndpoint);
					LOG.info("  token_endpoint: {}", discovery.tokenEndpoint);
					LOG.info("  jwks_uri: {}", discovery.jwksUri);
					LOG.info("  end_session_endpoint: {}", discovery.endSessionEndpoint);
					LOG.info("  revocation_endpoint: {}", discovery.revocationEndpoint);

					return discovery;
				});
	}

	/**
	 * Normalizes the issuer URL by removing trailing slashes.
	 */
	private static String normalizeIssuerUrl(String issuerUrl) {
		if (issuerUrl == null) {
			return "";
		}
		return issuerUrl.endsWith("/") ? issuerUrl.substring(0, issuerUrl.length() - 1) : issuerUrl;
	}

	/**
	 * Converts a JsonArray to a List of Strings.
	 */
	private static List<String> jsonArrayToList(JsonArray array) {
		if (array == null) {
			return List.of();
		}
		return array.asList().stream() //
				.map(e -> e.getAsString()) //
				.toList();
	}

	// ===== Getters for Core Endpoints =====

	/**
	 * Gets the issuer identifier.
	 *
	 * @return the issuer URL
	 */
	public String getIssuer() {
		return this.issuer;
	}

	/**
	 * Gets the authorization endpoint URL (for OAuth2 authorization code flow).
	 *
	 * @return the authorization endpoint
	 * @throws IllegalStateException if not available
	 */
	public String getAuthorizationEndpoint() {
		if (this.authorizationEndpoint == null) {
			throw new IllegalStateException("authorization_endpoint not available in OIDC Discovery");
		}
		return this.authorizationEndpoint;
	}

	/**
	 * Gets the token endpoint URL (for exchanging codes/credentials for tokens).
	 *
	 * @return the token endpoint
	 * @throws IllegalStateException if not available
	 */
	public String getTokenEndpoint() {
		if (this.tokenEndpoint == null) {
			throw new IllegalStateException("token_endpoint not available in OIDC Discovery");
		}
		return this.tokenEndpoint;
	}

	/**
	 * Gets the JWKS URI (for JWT signature verification).
	 *
	 * @return the JWKS URI
	 * @throws IllegalStateException if not available
	 */
	public String getJwksUri() {
		if (this.jwksUri == null) {
			throw new IllegalStateException("jwks_uri not available in OIDC Discovery");
		}
		return this.jwksUri;
	}

	/**
	 * Gets the userinfo endpoint URL.
	 *
	 * @return the userinfo endpoint, or empty if not available
	 */
	public Optional<String> getUserinfoEndpoint() {
		return Optional.ofNullable(this.userinfoEndpoint);
	}

	// ===== Getters for Session Management Endpoints =====

	/**
	 * Gets the end session (logout) endpoint URL.
	 *
	 * <p>
	 * This is the OIDC RP-Initiated Logout endpoint.
	 * </p>
	 *
	 * @return the end session endpoint, or empty if not available
	 */
	public Optional<String> getEndSessionEndpoint() {
		return Optional.ofNullable(this.endSessionEndpoint);
	}

	/**
	 * Gets the token revocation endpoint URL (RFC 7009).
	 *
	 * @return the revocation endpoint, or empty if not available
	 */
	public Optional<String> getRevocationEndpoint() {
		return Optional.ofNullable(this.revocationEndpoint);
	}

	// ===== Getters for Optional Endpoints =====

	/**
	 * Gets the dynamic client registration endpoint URL.
	 *
	 * @return the registration endpoint, or empty if not available
	 */
	public Optional<String> getRegistrationEndpoint() {
		return Optional.ofNullable(this.registrationEndpoint);
	}

	/**
	 * Gets the token introspection endpoint URL (RFC 7662).
	 *
	 * @return the introspection endpoint, or empty if not available
	 */
	public Optional<String> getIntrospectionEndpoint() {
		return Optional.ofNullable(this.introspectionEndpoint);
	}

	// ===== Getters for Supported Features =====

	/**
	 * Gets the list of supported grant types.
	 *
	 * @return list of grant types (e.g., "authorization_code", "refresh_token")
	 */
	public List<String> getGrantTypesSupported() {
		return this.grantTypesSupported;
	}

	/**
	 * Gets the list of supported response types.
	 *
	 * @return list of response types (e.g., "code", "token")
	 */
	public List<String> getResponseTypesSupported() {
		return this.responseTypesSupported;
	}

	/**
	 * Gets the list of supported scopes.
	 *
	 * @return list of scopes (e.g., "openid", "profile", "email")
	 */
	public List<String> getScopesSupported() {
		return this.scopesSupported;
	}

	// ===== Utility Methods =====

	/**
	 * Checks if logout is supported by this provider.
	 *
	 * @return true if either end_session_endpoint or revocation_endpoint is
	 *         available
	 */
	public boolean isLogoutSupported() {
		return this.endSessionEndpoint != null || this.revocationEndpoint != null;
	}

	/**
	 * Checks if a specific grant type is supported.
	 *
	 * @param grantType the grant type to check
	 * @return true if supported
	 */
	public boolean isGrantTypeSupported(String grantType) {
		return this.grantTypesSupported.contains(grantType);
	}

	@Override
	public String toString() {
		return "OidcDiscovery [issuer=" + this.issuer + "]";
	}

	// ===== Builder =====

	private static class Builder {
		private String issuer;
		private String authorizationEndpoint;
		private String tokenEndpoint;
		private String userinfoEndpoint;
		private String jwksUri;
		private String endSessionEndpoint;
		private String revocationEndpoint;
		private String registrationEndpoint;
		private String introspectionEndpoint;
		private List<String> grantTypesSupported = List.of();
		private List<String> responseTypesSupported = List.of();
		private List<String> scopesSupported = List.of();

		Builder issuer(String issuer) {
			this.issuer = issuer;
			return this;
		}

		Builder authorizationEndpoint(String authorizationEndpoint) {
			this.authorizationEndpoint = authorizationEndpoint;
			return this;
		}

		Builder tokenEndpoint(String tokenEndpoint) {
			this.tokenEndpoint = tokenEndpoint;
			return this;
		}

		Builder userinfoEndpoint(String userinfoEndpoint) {
			this.userinfoEndpoint = userinfoEndpoint;
			return this;
		}

		Builder jwksUri(String jwksUri) {
			this.jwksUri = jwksUri;
			return this;
		}

		Builder endSessionEndpoint(String endSessionEndpoint) {
			this.endSessionEndpoint = endSessionEndpoint;
			return this;
		}

		Builder revocationEndpoint(String revocationEndpoint) {
			this.revocationEndpoint = revocationEndpoint;
			return this;
		}

		Builder registrationEndpoint(String registrationEndpoint) {
			this.registrationEndpoint = registrationEndpoint;
			return this;
		}

		Builder introspectionEndpoint(String introspectionEndpoint) {
			this.introspectionEndpoint = introspectionEndpoint;
			return this;
		}

		Builder grantTypesSupported(List<String> grantTypesSupported) {
			this.grantTypesSupported = grantTypesSupported;
			return this;
		}

		Builder responseTypesSupported(List<String> responseTypesSupported) {
			this.responseTypesSupported = responseTypesSupported;
			return this;
		}

		Builder scopesSupported(List<String> scopesSupported) {
			this.scopesSupported = scopesSupported;
			return this;
		}

		OidcDiscovery build() {
			return new OidcDiscovery(this);
		}
	}

}
