package io.openems.edge.controller.api.websocket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.OpenemsConstants;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.request.GetEdgeRequest;
import io.openems.common.jsonrpc.request.GetEdgesRequest;
import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.common.user.User;

public class OnRequestTest {

	private final User user = new DummyUser("id", "password", Language.DEFAULT, Role.ADMIN);

	@Test
	public void testHandleGetEdgesRequest() throws Exception {
		final var response = OnRequest.handleGetEdgesRequest(this.user,
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
		final var response = OnRequest.handleGetEdgeRequest(this.user,
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
		assertEquals(this.user.getGlobalRole(), edge.role());
		assertNull(edge.firstSetupProtocol());
	}

}
