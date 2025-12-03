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

public class PlcNextAuthClientTest {

	private PlcNextAuthClientConfig authClientConfig;

	private DummyBridgeHttp dummyAuthBridgeHttp;

	private PlcNextAuthClient authClient;

	@Before
	public void setup() {
		authClientConfig = new PlcNextAuthClientConfig("https://localhost/auth", "junit", "junit");

		dummyAuthBridgeHttp = new DummyBridgeHttp() {
			@Override
			public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint) {
				if (endpoint.url().contains(PlcNextAuthClient.PATH_AUTH_TOKEN)) {
					return CompletableFuture.supplyAsync(
							() -> new HttpResponse<String>(HttpStatus.OK, Map.of(), "{'code': 'dummy_auth'}"));
				} else if (endpoint.url().contains(PlcNextAuthClient.PATH_ACCESS_TOKEN)) {
					return CompletableFuture.supplyAsync(() -> new HttpResponse<String>(HttpStatus.OK, Map.of(),
							"{'access_token': 'dummy_access'}"));
				} else {
					throw new IllegalStateException("Use not suitable!");
				}
			}
		};
		authClient = new PlcNextAuthClient(dummyAuthBridgeHttp);
	}

	@Test
	public void testFetchAccessToken() {
		String accessToken = authClient.fetchSingleAuthentication(authClientConfig);

		Assert.assertNotNull(accessToken);
		System.out.println("ECHO: accessToken = " + accessToken);
	}

	@Test
	public void testBuildAuthTokenEndpoint_Successfully() {
		String expectedRequestUrl = authClientConfig.authUrl() + PlcNextAuthClient.PATH_AUTH_TOKEN;
		String expectedRequestBody = "{\"scope\":\"variables\" }";

		Endpoint result = authClient.buildAuthTokenEndpointRepresentation(authClientConfig);

		Assert.assertEquals(expectedRequestUrl, result.url());
		Assert.assertEquals(expectedRequestBody, result.body());
	}

	@Test
	public void testBuildAccessTokenEndpoint_Successfully() {
		String expectedRequestUrl = authClientConfig.authUrl() + PlcNextAuthClient.PATH_ACCESS_TOKEN;
		String expectedRequestBody = "{ \"code\": \"4711\", \"grant_type\": \"authorization_code\", \"username\": \""
				+ authClientConfig.username() + "\", " + "\"password\": \"" + authClientConfig.password() + "\" }";

		Endpoint result = authClient.buildAccessTokenEndpointRepresentation("4711", authClientConfig);

		Assert.assertEquals(expectedRequestUrl, result.url());
		Assert.assertEquals(expectedRequestBody, result.body());

	}
}
