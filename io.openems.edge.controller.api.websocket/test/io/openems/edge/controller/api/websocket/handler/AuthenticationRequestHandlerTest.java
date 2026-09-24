package io.openems.edge.controller.api.websocket.handler;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.request.AuthenticateWithExternalAuthRequest;
import io.openems.common.jsonrpc.response.AuthenticateResponse;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.Call;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.user.User;
import io.openems.edge.controller.api.websocket.OnRequest;
import io.openems.edge.controller.api.websocket.WsData;

public class AuthenticationRequestHandlerTest {

	private JsonApiBuilder api;

	@Before
	public void before() {
		this.api = new JsonApiBuilder();
		new AuthenticationRequestHandler().buildJsonApiRoutes(this.api);
	}

	@Test
	public void testAuthenticateWithExternalAuth() throws Exception {
		final var externalUser = new User("external:alice", "Alice Example", Language.DEFAULT, Role.INSTALLER);
		final var wsData = new WsData(null, null, externalUser);
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(new GenericJsonrpcRequest(
				AuthenticateWithExternalAuthRequest.METHOD, JsonUtils.buildJsonObject().build()));
		call.put(OnRequest.WS_DATA_KEY, wsData);

		this.api.handle(call);

		final var response = (AuthenticateResponse) call.getResponse();
		assertEquals(externalUser.getId(), response.getResult().get("user").getAsJsonObject().get("id").getAsString());
		assertEquals(Role.INSTALLER, wsData.getUser().orElseThrow().getRole());
	}
}
