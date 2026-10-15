package io.openems.common.jsonrpc.request;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.utils.JsonUtils;

public class AuthenticateWithExternalAuthRequestTest {

	@Test
	public void testRequest() throws Exception {
		final var request = new AuthenticateWithExternalAuthRequest();

		assertEquals(AuthenticateWithExternalAuthRequest.METHOD, request.getMethod());
		assertEquals(JsonUtils.buildJsonObject().build(), request.getParams());
	}

	@Test
	public void testFromGenericRequest() throws Exception {
		final var request = AuthenticateWithExternalAuthRequest.from(new GenericJsonrpcRequest(
				AuthenticateWithExternalAuthRequest.METHOD, JsonUtils.buildJsonObject().build()));

		assertEquals(AuthenticateWithExternalAuthRequest.METHOD, request.getMethod());
		assertEquals(JsonUtils.buildJsonObject().build(), request.getParams());
	}
}
