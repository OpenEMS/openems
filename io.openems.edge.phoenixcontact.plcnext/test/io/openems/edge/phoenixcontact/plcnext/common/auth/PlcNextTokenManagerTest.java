package io.openems.edge.phoenixcontact.plcnext.common.auth;

import static org.junit.Assert.assertThrows;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttp;
import io.openems.common.types.HttpStatus;

public class PlcNextTokenManagerTest {
	private PlcNextAuthConfig authClientConfig;

	private DummyBridgeHttp dummyAuthBridgeHttp;

	private PlcNextTokenManagerImpl tokenManager;

	@Before
	public void setup() {
		authClientConfig = new PlcNextAuthConfig("https://localhost/auth", "junit", "junit");

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
		tokenManager = new PlcNextTokenManagerImpl(dummyAuthBridgeHttp);
	}

	@Test
	public void testFetchAccessToken_Successfully() {
		// test
		tokenManager.fetchToken(authClientConfig).join();
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
		PlcNextTokenManager tokenManagerFailing = new PlcNextTokenManagerImpl(dummyAuthBridgeHttpFailing);

		// test
		assertThrows(CompletionException.class, //
				() -> tokenManagerFailing.fetchToken(authClientConfig).join());

		String accessToken = tokenManagerFailing.getToken();

		// check
		Assert.assertNull(accessToken);
	}

	@Test
	public void testFetchAccessToken_AuthTokenCallFailedDueToException() {
		// prep
		DummyBridgeHttp dummyAuthBridgeHttpFailing = new DummyBridgeHttp() {
			@Override
			public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint) {
				if (endpoint.url().contains(PlcNextTokenManager.PATH_AUTH_TOKEN)) {
					return CompletableFuture.failedFuture(new IllegalStateException());
				} else if (endpoint.url().contains(PlcNextTokenManager.PATH_ACCESS_TOKEN)) {
					return CompletableFuture.failedFuture(new HttpError.ResponseError(HttpStatus.UNAUTHORIZED, null));
				} else {
					throw new IllegalStateException("Use not suitable!");
				}
			}
		};

		// test
		PlcNextTokenManager tokenManagerFailing = new PlcNextTokenManagerImpl(dummyAuthBridgeHttpFailing);

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
		PlcNextTokenManager tokenManagerFailing = new PlcNextTokenManagerImpl(dummyAuthBridgeHttpFailing);

		tokenManagerFailing.fetchToken(authClientConfig).join();
		String accessToken = tokenManagerFailing.getToken();

		// check
		Assert.assertNull(accessToken);
	}
}
