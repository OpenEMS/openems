package io.openems.backend.authentication.oauth2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.bridge.http.dummy.DummyEndpointFetcher;

// #SOLTEGRA: Tests for OIDC Discovery document parsing
public class OidcDiscoveryTest {

	private static final String KEYCLOAK_DISCOVERY_RESPONSE = """
			{
			   "issuer":"https://auth.example.com/realms/test",
			   "authorization_endpoint":"https://auth.example.com/realms/test/protocol/openid-connect/auth",
			   "token_endpoint":"https://auth.example.com/realms/test/protocol/openid-connect/token",
			   "userinfo_endpoint":"https://auth.example.com/realms/test/protocol/openid-connect/userinfo",
			   "jwks_uri":"https://auth.example.com/realms/test/protocol/openid-connect/certs",
			   "end_session_endpoint":"https://auth.example.com/realms/test/protocol/openid-connect/logout",
			   "revocation_endpoint":"https://auth.example.com/realms/test/protocol/openid-connect/revoke",
			   "introspection_endpoint":"https://auth.example.com/realms/test/protocol/openid-connect/token/introspect",
			   "grant_types_supported":[
			      "authorization_code",
			      "refresh_token",
			      "client_credentials",
			      "password"
			   ],
			   "response_types_supported":[
			      "code",
			      "token",
			      "id_token"
			   ],
			   "scopes_supported":[
			      "openid",
			      "profile",
			      "email",
			      "offline_access"
			   ]
			}
						""";

	private static final String MINIMAL_DISCOVERY_RESPONSE = """
			{
			   "issuer":"https://minimal.example.com",
			   "token_endpoint":"https://minimal.example.com/oauth/token",
			   "jwks_uri":"https://minimal.example.com/.well-known/jwks.json"
			}
						""";

	@Test
	public void testFetchOidcDiscovery() throws InterruptedException, ExecutionException, TimeoutException {
		final var endpointFetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();
		addDiscoveryHandler(endpointFetcher, KEYCLOAK_DISCOVERY_RESPONSE);

		final var bridgeHttpFactory = DummyBridgeHttpFactory.ofBridgeImpl(//
				() -> endpointFetcher, //
				() -> DummyBridgeHttpFactory.dummyBridgeHttpExecutor(true));

		final var bridgeHttp = bridgeHttpFactory.get();

		try {
			final var discovery = OidcDiscovery.fetch(bridgeHttp, "https://auth.example.com/realms/test").get(5,
					TimeUnit.SECONDS);

			assertEquals("https://auth.example.com/realms/test", discovery.getIssuer());
			assertEquals("https://auth.example.com/realms/test/protocol/openid-connect/auth",
					discovery.getAuthorizationEndpoint());
			assertEquals("https://auth.example.com/realms/test/protocol/openid-connect/token",
					discovery.getTokenEndpoint());
			assertEquals("https://auth.example.com/realms/test/protocol/openid-connect/certs", discovery.getJwksUri());
			assertTrue(discovery.getEndSessionEndpoint().isPresent());
			assertEquals("https://auth.example.com/realms/test/protocol/openid-connect/logout",
					discovery.getEndSessionEndpoint().get());
			assertTrue(discovery.getRevocationEndpoint().isPresent());
			assertTrue(discovery.isLogoutSupported());
			assertTrue(discovery.isGrantTypeSupported("authorization_code"));
			assertTrue(discovery.isGrantTypeSupported("refresh_token"));
			assertTrue(discovery.isGrantTypeSupported("client_credentials"));
			assertFalse(discovery.isGrantTypeSupported("implicit"));
		} finally {
			bridgeHttpFactory.unget(bridgeHttp);
		}
	}

	@Test
	public void testFetchMinimalDiscovery() throws InterruptedException, ExecutionException, TimeoutException {
		final var endpointFetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();
		addDiscoveryHandler(endpointFetcher, MINIMAL_DISCOVERY_RESPONSE);

		final var bridgeHttpFactory = DummyBridgeHttpFactory.ofBridgeImpl(//
				() -> endpointFetcher, //
				() -> DummyBridgeHttpFactory.dummyBridgeHttpExecutor(true));

		final var bridgeHttp = bridgeHttpFactory.get();

		try {
			final var discovery = OidcDiscovery.fetch(bridgeHttp, "https://minimal.example.com/").get(5,
					TimeUnit.SECONDS);

			assertEquals("https://minimal.example.com", discovery.getIssuer());
			assertEquals("https://minimal.example.com/oauth/token", discovery.getTokenEndpoint());
			assertEquals("https://minimal.example.com/.well-known/jwks.json", discovery.getJwksUri());
			assertFalse(discovery.getEndSessionEndpoint().isPresent());
			assertFalse(discovery.getRevocationEndpoint().isPresent());
			assertFalse(discovery.isLogoutSupported());
			assertTrue(discovery.getGrantTypesSupported().isEmpty());
		} finally {
			bridgeHttpFactory.unget(bridgeHttp);
		}
	}

	@Test
	public void testNormalizeIssuerUrl() throws InterruptedException, ExecutionException, TimeoutException {
		final var endpointFetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();
		addDiscoveryHandler(endpointFetcher, KEYCLOAK_DISCOVERY_RESPONSE);

		final var bridgeHttpFactory = DummyBridgeHttpFactory.ofBridgeImpl(//
				() -> endpointFetcher, //
				() -> DummyBridgeHttpFactory.dummyBridgeHttpExecutor(true));

		final var bridgeHttp = bridgeHttpFactory.get();

		try {
			// URL with trailing slash should be normalized
			final var discovery = OidcDiscovery.fetch(bridgeHttp, "https://auth.example.com/realms/test/").get(5,
					TimeUnit.SECONDS);

			// Issuer should come from the response, not the normalized input
			assertEquals("https://auth.example.com/realms/test", discovery.getIssuer());
		} finally {
			bridgeHttpFactory.unget(bridgeHttp);
		}
	}

	private static void addDiscoveryHandler(DummyEndpointFetcher fetcher, String responseBody) {
		fetcher.addEndpointHandler(endpoint -> {
			if (endpoint.url().contains("/.well-known/openid-configuration")) {
				return HttpResponse.ok(responseBody);
			}
			return null;
		});
	}
}
