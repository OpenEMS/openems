package io.openems.edge.io.phoenixcontact;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.dummy.DummyBridgeHttp;

public class PlcNextAuthClientTest {

	private TestConfig testConfig;

	private PlcNextAuthClient authClient;

	@Before
	public void setup() {
		testConfig = TestConfig.create().build();
		authClient = new PlcNextAuthClient(new DummyBridgeHttp(), testConfig);
	}

	@Test
	public void testBuildAuthTokenEndpoint_Successfully() {
		String expectedRequestUrl = testConfig.authUrl() + PlcNextAuthClient.PATH_AUTH_TOKEN;
		String expectedRequestBody = "{\"scope\":\"variables\" }";

		Endpoint result = authClient.buildAuthTokenEndpointRepresentation();

		Assert.assertEquals(expectedRequestUrl, result.url());
		Assert.assertEquals(expectedRequestBody, result.body());
	}

	@Test
	public void testBuildAccessTokenEndpoint_Successfully() {
		String expectedRequestUrl = testConfig.authUrl() + PlcNextAuthClient.PATH_ACCESS_TOKEN;
		String expectedRequestBody = "{ \"code\": \"4711\", \"grant_type\": \"authorization_code\", \"username\": \""
				+ testConfig.username() + "\", " + "\"password\": \"" + testConfig.password() + "\" }";

		Endpoint result = authClient.buildAccessTokenEndpointRepresentation("4711");

		Assert.assertEquals(expectedRequestUrl, result.url());
		Assert.assertEquals(expectedRequestBody, result.body());

	}

}
