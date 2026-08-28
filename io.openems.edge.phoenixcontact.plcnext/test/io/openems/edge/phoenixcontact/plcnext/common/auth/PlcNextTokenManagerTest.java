package io.openems.edge.phoenixcontact.plcnext.common.auth;

import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttp;
import io.openems.common.types.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PlcNextTokenManagerTest {
	private PlcNextAuthConfig authClientConfig;

    private PlcNextTokenManagerImpl tokenManager;

	@Before
	public void setup() {
		this.authClientConfig = new PlcNextAuthConfig("https://localhost/auth", "/v1.3/auth", "junit", "junit");

        var dummyAuthBridgeHttp = new DummyBridgeHttp() {
            @Override
            public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint) {
                if (endpoint.url().contains(PlcNextTokenManager.PATH_AUTH_TOKEN)) {
                    return CompletableFuture.supplyAsync(() -> new HttpResponse<>(HttpStatus.OK, Map.of(),
                            "{'code': 'dummy_auth', 'expires_in': 600 }"));
                } else if (endpoint.url().contains(PlcNextTokenManager.PATH_ACCESS_TOKEN)) {
                    return CompletableFuture.supplyAsync(() -> new HttpResponse<>(HttpStatus.OK, Map.of(),
                            "{'access_token': 'dummy_access'}"));
                } else {
                    throw new IllegalStateException("Use not suitable!");
                }
            }
        };
		this.tokenManager = new PlcNextTokenManagerImpl(dummyAuthBridgeHttp);
	}

	@Test
	public void testFetchAccessToken_Successfully() {
		// test
		this.tokenManager.fetchToken(this.authClientConfig).join();

		// check
		var accessToken = this.tokenManager.getToken();
		assertNotNull(accessToken);
		assertEquals("dummy_access", accessToken);
	}

	@Test
	public void testBuildAuthTokenEndpoint_Successfully() {
		// prep
		var expectedRequestUrl = this.authClientConfig.authUrl() + PlcNextTokenManager.PATH_AUTH_TOKEN;
		var expectedRequestBody = "{\"scope\":\"variables\" }";

		// test
		var result = this.tokenManager.buildAuthTokenEndpointRepresentation(this.authClientConfig);

		// check
		assertEquals(expectedRequestUrl, result.url());
		assertEquals(expectedRequestBody, result.body());
	}

	@Test
	public void testBuildAccessTokenEndpoint_Successfully() {
		// prep
		var expectedRequestUrl = this.authClientConfig.authUrl() + PlcNextTokenManager.PATH_ACCESS_TOKEN;
		var expectedRequestBody = new StringBuilder("{ ")
				.append("\"code\": \"4711\", ")
				.append("\"grant_type\": \"authorization_code\", ")
				.append("\"username\": \"").append(this.authClientConfig.username()).append("\", ")
				.append("\"password\": \"").append(this.authClientConfig.password()).append("\" ")
				.append("}").toString();
		var authToken = new PlcNextAuthAndAccessTokenDto("4711", 0);

		// test
		var result = this.tokenManager.buildAccessTokenEndpointRepresentation(authToken, this.authClientConfig);

		// check
		assertEquals(expectedRequestUrl, result.url());
		assertEquals(expectedRequestBody, result.body());

	}

	@Test
	public void testFetchAccessToken_AuthTokenCallFailed() {
		// prep
		var dummyAuthBridgeHttpFailing = new DummyBridgeHttp() {
			@Override
			public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint) {
				if (endpoint.url().contains(PlcNextTokenManager.PATH_AUTH_TOKEN)) {
					return CompletableFuture.failedFuture(new IllegalStateException());
				} else if (endpoint.url().contains(PlcNextTokenManager.PATH_ACCESS_TOKEN)) {
					return CompletableFuture
							.supplyAsync(() -> new HttpResponse<>(HttpStatus.UNAUTHORIZED, Map.of(), "{}"));
				} else {
					throw new IllegalStateException("Use not suitable!");
				}
			}
		};
		var tokenManagerFailing = new PlcNextTokenManagerImpl(dummyAuthBridgeHttpFailing);

		// test
		assertThrows(CompletionException.class, //
				() -> tokenManagerFailing.fetchToken(this.authClientConfig).join());


		// check
		var accessToken = tokenManagerFailing.getToken();
		assertNull(accessToken);
	}

	@Test
	public void testFetchAccessToken_AuthTokenCallFailedDueToException() {
		// prep
		var dummyAuthBridgeHttpFailing = new DummyBridgeHttp() {
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
		var tokenManagerFailing = new PlcNextTokenManagerImpl(dummyAuthBridgeHttpFailing);
		tokenManagerFailing.fetchToken(this.authClientConfig);

		// check
		var accessToken = tokenManagerFailing.getToken();
		assertNull(accessToken);
	}

	@Test
	public void testFetchAccessToken_AccessTokenCallFailedWithInvalidResponse() {
		// prep
		var dummyAuthBridgeHttpFailing = new DummyBridgeHttp() {
			@Override
			public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint) {
				if (endpoint.url().contains(PlcNextTokenManager.PATH_AUTH_TOKEN)) {
					return CompletableFuture.supplyAsync(() -> new HttpResponse<>(HttpStatus.OK, Map.of(),
							"{'code': 'dummy_auth', 'expires_in': 600}"));
				} else if (endpoint.url().contains(PlcNextTokenManager.PATH_ACCESS_TOKEN)) {
					return CompletableFuture
							.supplyAsync(() -> new HttpResponse<>(HttpStatus.UNAUTHORIZED, Map.of(), "{}"));
				} else {
					throw new IllegalStateException("Use not suitable!");
				}
			}
		};

		// test
		var tokenManagerFailing = new PlcNextTokenManagerImpl(dummyAuthBridgeHttpFailing);
		tokenManagerFailing.fetchToken(this.authClientConfig).join();

		// check
		var accessToken = tokenManagerFailing.getToken();
		assertNull(accessToken);
	}
}
