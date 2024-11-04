package io.openems.edge.controller.api.websocket;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.OpenemsConstants;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.request.GetEdgeRequest;
import io.openems.common.jsonrpc.request.GetEdgesRequest;
import io.openems.common.jsonrpc.response.GetEdgeResponse;
import io.openems.common.jsonrpc.response.GetEdgesResponse;
import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.Call;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.websocket.handler.EdgeRequestHandler;

public class OnRequestTest {

	private JsonApiBuilder api;

	@Before
	public void before() {
		this.api = new JsonApiBuilder();
		new EdgeRequestHandler().buildJsonApiRoutes(this.api);
	}

	@Test
	public void testHandleGetEdgesRequest() throws Exception {
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(
				GetEdgesRequest.from(new GenericJsonrpcRequest(GetEdgesRequest.METHOD, JsonUtils.buildJsonObject() //
						.addProperty("page", 0) //
						.build())));
		call.put(EdgeKeys.USER_KEY, DUMMY_ADMIN);

		this.api.handle(call);

		final var response = (GetEdgesResponse) call.getResponse();

		final var edges = response.edgeMetadata;
		assertEquals(1, edges.size());

		this.validateLocalEdgeMetadata(edges.get(0));
	}

	@Test
	public void testHandleGetEdgeRequest() throws Exception {
		final var call = new Call<JsonrpcRequest, JsonrpcResponse>(
				GetEdgeRequest.from(new GenericJsonrpcRequest(GetEdgeRequest.METHOD, JsonUtils.buildJsonObject() //
						.addProperty("edgeId", ControllerApiWebsocket.EDGE_ID) //
						.build())));
		call.put(EdgeKeys.USER_KEY, DUMMY_ADMIN);

		this.api.handle(call);

		final var response = (GetEdgeResponse) call.getResponse();

		this.validateLocalEdgeMetadata(response.edgeMetadata);
	}

	private void validateLocalEdgeMetadata(EdgeMetadata edge) {
		assertEquals(ControllerApiWebsocket.EDGE_ID, edge.id());
		assertEquals(ControllerApiWebsocket.EDGE_COMMENT, edge.comment());
		assertEquals(ControllerApiWebsocket.EDGE_PRODUCT_TYPE, edge.producttype());
		assertEquals(ControllerApiWebsocket.SUM_STATE, edge.sumState());
		assertEquals(OpenemsConstants.VERSION, edge.version());
		assertTrue(edge.isOnline());
		assertEquals(DUMMY_ADMIN.getGlobalRole(), edge.role());
		assertNull(edge.firstSetupProtocol());
	}

}
