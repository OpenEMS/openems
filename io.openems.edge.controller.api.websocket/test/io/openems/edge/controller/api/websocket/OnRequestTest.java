package io.openems.edge.controller.api.websocket;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.OpenemsConstants;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.request.GetEdgeRequest;
import io.openems.common.jsonrpc.request.GetEdgesRequest;
import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
import io.openems.common.utils.JsonUtils;

public class OnRequestTest {

	@Test
	public void testHandleGetEdgesRequest() throws Exception {
		final var response = OnRequest.handleGetEdgesRequest(DUMMY_ADMIN,
				GetEdgesRequest.from(new GenericJsonrpcRequest(GetEdgesRequest.METHOD, JsonUtils.buildJsonObject() //
						.addProperty("page", 0) //
						.build())))
				.get();

		final var edges = response.edgeMetadata;
		assertEquals(1, edges.size());

		this.validateLocalEdgeMetadata(edges.get(0));
	}

	@Test
	public void testHandleGetEdgeRequest() throws Exception {
		final var response = OnRequest.handleGetEdgeRequest(DUMMY_ADMIN,
				GetEdgeRequest.from(new GenericJsonrpcRequest(GetEdgeRequest.METHOD, JsonUtils.buildJsonObject() //
						.addProperty("edgeId", ControllerApiWebsocket.EDGE_ID) //
						.build())))
				.get();

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
