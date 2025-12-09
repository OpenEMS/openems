package io.openems.edge.io.phoenixcontact.auth;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttp;
import io.openems.common.types.HttpStatus;

public class PlcNextTokenManagerTest {
	private PlcNextTokenManagerConfig authClientConfig;

	private DummyBridgeHttp dummyAuthBridgeHttp;

	private PlcNextTokenManager tokenManager;

	@Before
	public void setup() {
		authClientConfig = new PlcNextTokenManagerConfig("https://localhost/auth", "junit", "junit");

		dummyAuthBridgeHttp = new DummyBridgeHttp() {
			@Override
			public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint) {
				if (endpoint.url().contains(PlcNextTokenManager.PATH_AUTH_TOKEN)) {
					return CompletableFuture.supplyAsync(() -> new HttpResponse<String>(HttpStatus.OK, Map.of(),
							"{'code': 'dummy_auth', 'expires_in': 600 }"));
				} else if (endpoint.url().contains(PlcNextTokenManager.PATH_ACCESS_TOKEN)) {
					return CompletableFuture.supplyAsync(() -> new HttpResponse<String>(HttpStatus.OK, Map.of(),
							"{'access_token': 'dummy_access'}"));
				} else {
					throw new IllegalStateException("Use not suitable!");
				}
			}
		};
		tokenManager = new PlcNextTokenManager(dummyAuthBridgeHttp);
	}

	@Test
	public void testFetchAccessToken_Successfully() {
		// test
		tokenManager.fetchToken(authClientConfig);
		String accessToken = tokenManager.getToken();

		// check
		Assert.assertNotNull(accessToken);
		Assert.assertEquals("dummy_access", accessToken);
	}

	@Test
	public void testBuildAuthTokenEndpoint_Successfully() {
		// prep
		String expectedRequestUrl = authClientConfig.authUrl() + PlcNextTokenManager.PATH_AUTH_TOKEN;
		String expectedRequestBody = "{\"scope\":\"variables\" }";

		// test
		Endpoint result = tokenManager.buildAuthTokenEndpointRepresentation(authClientConfig);

		// check
		Assert.assertEquals(expectedRequestUrl, result.url());
		Assert.assertEquals(expectedRequestBody, result.body());
	}

	@Test
	public void testBuildAccessTokenEndpoint_Successfully() {
		// prep
		String expectedRequestUrl = authClientConfig.authUrl() + PlcNextTokenManager.PATH_ACCESS_TOKEN;
		String expectedRequestBody = "{ \"code\": \"4711\", \"grant_type\": \"authorization_code\", \"username\": \""
				+ authClientConfig.username() + "\", " + "\"password\": \"" + authClientConfig.password() + "\" }";
		PlcNextAuthAndAccessTokenDTO authToken = new PlcNextAuthAndAccessTokenDTO("4711", 0);

		// test
		Endpoint result = tokenManager.buildAccessTokenEndpointRepresentation(authToken, authClientConfig);

		// check
		Assert.assertEquals(expectedRequestUrl, result.url());
		Assert.assertEquals(expectedRequestBody, result.body());

	}

	@Test
	public void testFetchAccessToken_AuthTokenCallFailed() {
		// prep
		DummyBridgeHttp dummyAuthBridgeHttpFailing = new DummyBridgeHttp() {
			@Override
			public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint) {
				if (endpoint.url().contains(PlcNextTokenManager.PATH_AUTH_TOKEN)) {
					return CompletableFuture.failedFuture(new IllegalStateException());
				} else if (endpoint.url().contains(PlcNextTokenManager.PATH_ACCESS_TOKEN)) {
					return CompletableFuture
							.supplyAsync(() -> new HttpResponse<String>(HttpStatus.UNAUTHORIZED, Map.of(), "{}"));
				} else {
					throw new IllegalStateException("Use not suitable!");
				}
			}
		};

		// test
		PlcNextTokenManager tokenManagerFailing = new PlcNextTokenManager(dummyAuthBridgeHttpFailing);

		tokenManagerFailing.fetchToken(authClientConfig);
		String accessToken = tokenManagerFailing.getToken();

		// check
		Assert.assertNull(accessToken);
	}

	@Test
	public void testFetchAccessToken_AccessTokenCallFailedWithInvalidResponse() {
		// prep
		DummyBridgeHttp dummyAuthBridgeHttpFailing = new DummyBridgeHttp() {
			@Override
			public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint) {
				if (endpoint.url().contains(PlcNextTokenManager.PATH_AUTH_TOKEN)) {
					return CompletableFuture.supplyAsync(() -> new HttpResponse<String>(HttpStatus.OK, Map.of(),
							"{'code': 'dummy_auth', 'expires_in': 600}"));
				} else if (endpoint.url().contains(PlcNextTokenManager.PATH_ACCESS_TOKEN)) {
					return CompletableFuture
							.supplyAsync(() -> new HttpResponse<String>(HttpStatus.UNAUTHORIZED, Map.of(), "{}"));
				} else {
					throw new IllegalStateException("Use not suitable!");
				}
			}
		};

		// test
		PlcNextTokenManager tokenManagerFailing = new PlcNextTokenManager(dummyAuthBridgeHttpFailing);

		tokenManagerFailing.fetchToken(authClientConfig);
		String accessToken = tokenManagerFailing.getToken();

		// check
		Assert.assertNull(accessToken);
	}
}
