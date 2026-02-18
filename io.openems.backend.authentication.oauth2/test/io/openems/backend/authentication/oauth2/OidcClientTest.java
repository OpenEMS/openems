package io.openems.backend.authentication.oauth2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.backend.authentication.oauth2.OidcClient.TokenResponse;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.bridge.http.dummy.DummyEndpointFetcher;

// #SOLTEGRA: Tests for OIDC Client token operations and logout
public class OidcClientTest {

	private static final String DISCOVERY_RESPONSE_FULL = """
			{
			   "issuer":"https://auth.example.com/realms/test",
			   "authorization_endpoint":"https://auth.example.com/realms/test/protocol/openid-connect/auth",
			   "token_endpoint":"https://auth.example.com/realms/test/protocol/openid-connect/token",
			   "userinfo_endpoint":"https://auth.example.com/realms/test/protocol/openid-connect/userinfo",
			   "jwks_uri":"https://auth.example.com/realms/test/protocol/openid-connect/certs",
			   "end_session_endpoint":"https://auth.example.com/realms/test/protocol/openid-connect/logout",
			   "revocation_endpoint":"https://auth.example.com/realms/test/protocol/openid-connect/revoke",
			   "grant_types_supported":[
			      "authorization_code",
			      "refresh_token",
			      "client_credentials",
			      "password"
			   ],
			   "response_types_supported":[
			      "code",
			      "token"
			   ],
			   "scopes_supported":[
			      "openid",
			      "profile",
			      "email"
			   ]
			}
						""";

	private static final String DISCOVERY_RESPONSE_END_SESSION_ONLY = """
			{
			    "issuer": "https://auth.example.com/realms/test",
			    "token_endpoint": "https://auth.example.com/realms/test/protocol/openid-connect/token",
			    "jwks_uri": "https://auth.example.com/realms/test/protocol/openid-connect/certs",
			    "end_session_endpoint": "https://auth.example.com/realms/test/protocol/openid-connect/logout"
			}
			""";

	private static final String DISCOVERY_RESPONSE_REVOCATION_ONLY = """
			{
			    "issuer": "https://auth.example.com/realms/test",
			    "token_endpoint": "https://auth.example.com/realms/test/protocol/openid-connect/token",
			    "jwks_uri": "https://auth.example.com/realms/test/protocol/openid-connect/certs",
			    "revocation_endpoint": "https://auth.example.com/realms/test/protocol/openid-connect/revoke"
			}
			""";

	private static final String DISCOVERY_RESPONSE_MINIMAL = """
			{
			    "issuer": "https://auth.example.com/realms/test",
			    "token_endpoint": "https://auth.example.com/realms/test/protocol/openid-connect/token",
			    "jwks_uri": "https://auth.example.com/realms/test/protocol/openid-connect/certs"
			}
			""";

	private static final String TOKEN_RESPONSE = """
			{
			    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwiTg",
			    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwiTn",
			    "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwiZn",
			    "token_type": "Bearer",
			    "expires_in": 300,
			    "scope": "openid profile email"
			}
			""";

	private static final String TOKEN_RESPONSE_MINIMAL = """
			{
			    "access_token": "minimal_access_token",
			    "token_type": "Bearer",
			    "expires_in": 3600
			}
			""";

	// ===== TokenResponse Parsing Tests =====

	@Test
	public void testTokenResponseFromJson() {
		final var json = JsonParser.parseString(TOKEN_RESPONSE).getAsJsonObject();
		final var response = TokenResponse.fromJson(json);

		assertEquals("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwiTg", response.accessToken());
		assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwiTn", response.refreshToken());
		assertEquals("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwiZn", response.idToken());
		assertEquals("Bearer", response.tokenType());
		assertEquals(300, response.expiresIn());
		assertEquals("openid profile email", response.scope());
	}

	@Test
	public void testTokenResponseFromJsonMinimal() {
		final var json = JsonParser.parseString(TOKEN_RESPONSE_MINIMAL).getAsJsonObject();
		final var response = TokenResponse.fromJson(json);

		assertEquals("minimal_access_token", response.accessToken());
		assertNull(response.refreshToken());
		assertNull(response.idToken());
		assertEquals("Bearer", response.tokenType());
		assertEquals(3600, response.expiresIn());
		assertNull(response.scope());
	}

	@Test
	public void testTokenResponseFromJsonWithNullValues() {
		final var json = new JsonObject();
		json.addProperty("access_token", "test_token");
		json.addProperty("token_type", "Bearer");
		json.add("refresh_token", null); // Explicit null
		// expires_in missing

		final var response = TokenResponse.fromJson(json);

		assertEquals("test_token", response.accessToken());
		assertNull(response.refreshToken());
		assertEquals(0, response.expiresIn()); // Default when missing
	}

	// ===== Client Credentials Grant Tests =====

	@Test
	public void testGetTokenWithClientCredentials() throws InterruptedException, ExecutionException, TimeoutException {
		final var endpointFetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();
		final var capturedRequest = new AtomicReference<String>();

		addDiscoveryAndTokenHandlers(endpointFetcher, DISCOVERY_RESPONSE_FULL, endpoint -> {
			capturedRequest.set(endpoint.url());
			return HttpResponse.ok(TOKEN_RESPONSE);
		});

		final var bridgeHttpFactory = DummyBridgeHttpFactory.ofBridgeImpl(//
				() -> endpointFetcher, //
				() -> DummyBridgeHttpFactory.dummyBridgeHttpExecutor(true));

		final var bridgeHttp = bridgeHttpFactory.get();

		try {
			final var discovery = OidcDiscovery.fetch(bridgeHttp, "https://auth.example.com/realms/test").get(5,
					TimeUnit.SECONDS);
			final var client = new OidcClient(bridgeHttp, discovery);

			final var response = client.getTokenWithClientCredentials("test-client", "test-secret").get(5,
					TimeUnit.SECONDS);

			assertNotNull(response);
			assertEquals("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwiTg", response.accessToken());
			assertTrue(capturedRequest.get().contains("/token"));
		} finally {
			bridgeHttpFactory.unget(bridgeHttp);
		}
	}

	// ===== Password Grant Tests =====

	@Test
	public void testGetTokenWithPassword() throws InterruptedException, ExecutionException, TimeoutException {
		final var endpointFetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();

		addDiscoveryAndTokenHandlers(endpointFetcher, DISCOVERY_RESPONSE_FULL,
				endpoint -> HttpResponse.ok(TOKEN_RESPONSE));

		final var bridgeHttpFactory = DummyBridgeHttpFactory.ofBridgeImpl(//
				() -> endpointFetcher, //
				() -> DummyBridgeHttpFactory.dummyBridgeHttpExecutor(true));

		final var bridgeHttp = bridgeHttpFactory.get();

		try {
			final var discovery = OidcDiscovery.fetch(bridgeHttp, "https://auth.example.com/realms/test").get(5,
					TimeUnit.SECONDS);
			final var client = new OidcClient(bridgeHttp, discovery);

			final var response = client.getTokenWithPassword("test-client", "test-secret", "testuser", "testpass")
					.get(5, TimeUnit.SECONDS);

			assertNotNull(response);
			assertEquals("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwiTg", response.accessToken());
		} finally {
			bridgeHttpFactory.unget(bridgeHttp);
		}
	}

	// ===== Refresh Token Tests =====

	@Test
	public void testRefreshToken() throws InterruptedException, ExecutionException, TimeoutException {
		final var endpointFetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();

		addDiscoveryAndTokenHandlers(endpointFetcher, DISCOVERY_RESPONSE_FULL,
				endpoint -> HttpResponse.ok(TOKEN_RESPONSE));

		final var bridgeHttpFactory = DummyBridgeHttpFactory.ofBridgeImpl(//
				() -> endpointFetcher, //
				() -> DummyBridgeHttpFactory.dummyBridgeHttpExecutor(true));

		final var bridgeHttp = bridgeHttpFactory.get();

		try {
			final var discovery = OidcDiscovery.fetch(bridgeHttp, "https://auth.example.com/realms/test").get(5,
					TimeUnit.SECONDS);
			final var client = new OidcClient(bridgeHttp, discovery);

			final var response = client.refreshToken("test-client", "test-secret", "old-refresh-token").get(5,
					TimeUnit.SECONDS);

			assertNotNull(response);
			assertNotNull(response.refreshToken());
		} finally {
			bridgeHttpFactory.unget(bridgeHttp);
		}
	}

	// ===== Authorization Code Exchange Tests =====

	@Test
	public void testExchangeCode() throws InterruptedException, ExecutionException, TimeoutException {
		final var endpointFetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();

		addDiscoveryAndTokenHandlers(endpointFetcher, DISCOVERY_RESPONSE_FULL,
				endpoint -> HttpResponse.ok(TOKEN_RESPONSE));

		final var bridgeHttpFactory = DummyBridgeHttpFactory.ofBridgeImpl(//
				() -> endpointFetcher, //
				() -> DummyBridgeHttpFactory.dummyBridgeHttpExecutor(true));

		final var bridgeHttp = bridgeHttpFactory.get();

		try {
			final var discovery = OidcDiscovery.fetch(bridgeHttp, "https://auth.example.com/realms/test").get(5,
					TimeUnit.SECONDS);
			final var client = new OidcClient(bridgeHttp, discovery);

			final var response = client.exchangeCode(//
					"test-client", //
					"test-secret", //
					"authorization_code_123", //
					"https://app.example.com/callback", //
					"pkce_verifier_abc" //
			).get(5, TimeUnit.SECONDS);

			assertNotNull(response);
			assertEquals("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwiTg", response.accessToken());
		} finally {
			bridgeHttpFactory.unget(bridgeHttp);
		}
	}

	@Test
	public void testExchangeCodeWithoutPkce() throws InterruptedException, ExecutionException, TimeoutException {
		final var endpointFetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();

		addDiscoveryAndTokenHandlers(endpointFetcher, DISCOVERY_RESPONSE_FULL,
				endpoint -> HttpResponse.ok(TOKEN_RESPONSE));

		final var bridgeHttpFactory = DummyBridgeHttpFactory.ofBridgeImpl(//
				() -> endpointFetcher, //
				() -> DummyBridgeHttpFactory.dummyBridgeHttpExecutor(true));

		final var bridgeHttp = bridgeHttpFactory.get();

		try {
			final var discovery = OidcDiscovery.fetch(bridgeHttp, "https://auth.example.com/realms/test").get(5,
					TimeUnit.SECONDS);
			final var client = new OidcClient(bridgeHttp, discovery);

			// codeVerifier is null (no PKCE)
			final var response = client.exchangeCode(//
					"test-client", //
					"test-secret", //
					"authorization_code_123", //
					"https://app.example.com/callback", //
					null //
			).get(5, TimeUnit.SECONDS);

			assertNotNull(response);
		} finally {
			bridgeHttpFactory.unget(bridgeHttp);
		}
	}

	// ===== Logout Tests =====

	@Test
	public void testLogoutWithEndSessionEndpoint() throws InterruptedException, ExecutionException, TimeoutException {
		final var endpointFetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();
		final var capturedEndpoint = new AtomicReference<String>();

		// Discovery handler
		endpointFetcher.addEndpointHandler(endpoint -> {
			if (endpoint.url().contains("/.well-known/openid-configuration")) {
				return HttpResponse.ok(DISCOVERY_RESPONSE_END_SESSION_ONLY);
			}
			// Capture logout endpoint
			capturedEndpoint.set(endpoint.url());
			return HttpResponse.ok("");
		});

		final var bridgeHttpFactory = DummyBridgeHttpFactory.ofBridgeImpl(//
				() -> endpointFetcher, //
				() -> DummyBridgeHttpFactory.dummyBridgeHttpExecutor(true));

		final var bridgeHttp = bridgeHttpFactory.get();

		try {
			final var discovery = OidcDiscovery.fetch(bridgeHttp, "https://auth.example.com/realms/test").get(5,
					TimeUnit.SECONDS);
			final var client = new OidcClient(bridgeHttp, discovery);

			client.logout("test-client", "test-secret", "refresh_token_123").get(5, TimeUnit.SECONDS);

			// Should use end_session_endpoint when available
			assertTrue(capturedEndpoint.get().contains("/logout"));
		} finally {
			bridgeHttpFactory.unget(bridgeHttp);
		}
	}

	@Test
	public void testLogoutFallbackToRevocationEndpoint()
			throws InterruptedException, ExecutionException, TimeoutException {
		final var endpointFetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();
		final var capturedEndpoint = new AtomicReference<String>();

		// Discovery handler
		endpointFetcher.addEndpointHandler(endpoint -> {
			if (endpoint.url().contains("/.well-known/openid-configuration")) {
				return HttpResponse.ok(DISCOVERY_RESPONSE_REVOCATION_ONLY);
			}
			// Capture logout endpoint
			capturedEndpoint.set(endpoint.url());
			return HttpResponse.ok("");
		});

		final var bridgeHttpFactory = DummyBridgeHttpFactory.ofBridgeImpl(//
				() -> endpointFetcher, //
				() -> DummyBridgeHttpFactory.dummyBridgeHttpExecutor(true));

		final var bridgeHttp = bridgeHttpFactory.get();

		try {
			final var discovery = OidcDiscovery.fetch(bridgeHttp, "https://auth.example.com/realms/test").get(5,
					TimeUnit.SECONDS);
			final var client = new OidcClient(bridgeHttp, discovery);

			client.logout("test-client", "test-secret", "refresh_token_123").get(5, TimeUnit.SECONDS);

			// Should fallback to revocation_endpoint
			assertTrue(capturedEndpoint.get().contains("/revoke"));
		} finally {
			bridgeHttpFactory.unget(bridgeHttp);
		}
	}

	@Test
	public void testLogoutNoEndpointAvailable() throws InterruptedException, ExecutionException, TimeoutException {
		final var endpointFetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();
		final var requestCount = new AtomicReference<Integer>(0);

		// Discovery handler
		endpointFetcher.addEndpointHandler(endpoint -> {
			requestCount.set(requestCount.get() + 1);
			if (endpoint.url().contains("/.well-known/openid-configuration")) {
				return HttpResponse.ok(DISCOVERY_RESPONSE_MINIMAL);
			}
			return HttpResponse.ok("");
		});

		final var bridgeHttpFactory = DummyBridgeHttpFactory.ofBridgeImpl(//
				() -> endpointFetcher, //
				() -> DummyBridgeHttpFactory.dummyBridgeHttpExecutor(true));

		final var bridgeHttp = bridgeHttpFactory.get();

		try {
			final var discovery = OidcDiscovery.fetch(bridgeHttp, "https://auth.example.com/realms/test").get(5,
					TimeUnit.SECONDS);
			final var client = new OidcClient(bridgeHttp, discovery);

			// Reset counter after discovery fetch
			requestCount.set(0);

			// Should complete without error even if no logout endpoint available
			client.logout("test-client", "test-secret", "refresh_token_123").get(5, TimeUnit.SECONDS);

			// No HTTP request should have been made for logout
			assertEquals(Integer.valueOf(0), requestCount.get());
		} finally {
			bridgeHttpFactory.unget(bridgeHttp);
		}
	}

	@FunctionalInterface
	private interface TokenHandler {
		HttpResponse<String> handle(io.openems.common.bridge.http.api.BridgeHttp.Endpoint endpoint);
	}

	private static void addDiscoveryAndTokenHandlers(DummyEndpointFetcher fetcher, String discoveryResponse,
			TokenHandler tokenHandler) {
		fetcher.addEndpointHandler(endpoint -> {
			if (endpoint.url().contains("/.well-known/openid-configuration")) {
				return HttpResponse.ok(discoveryResponse);
			}
			if (endpoint.url().contains("/token")) {
				return tokenHandler.handle(endpoint);
			}
			return null;
		});
	}
}
