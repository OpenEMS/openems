package io.openems.backend.core.jsonrpcrequesthandler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.EdgeHandler;
import io.openems.backend.common.metadata.User;
import io.openems.backend.common.test.DummyEdge;
import io.openems.backend.common.test.DummyEdgeManager;
import io.openems.backend.common.test.DummyMetadata;
import io.openems.backend.common.test.DummyUser;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.response.GetEdgeConfigResponse;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;

public class EdgeRpcRequestHandlerTest {

	private static final String EDGE_ID = "edge0";
	private static final DummyUser USER = DummyUser.DUMMY_GUEST;

	/**
	 * Cached config with factories → served directly, no Edge contact.
	 */
	@Test
	public void testGetEdgeConfig_returnsCache_whenFactoriesPresent()
			throws OpenemsNamedException, ExecutionException, InterruptedException {
		final var cachedConfig = buildConfigWithFactory();
		final var handler = buildHandler(cachedConfig, false, EdgeConfig.empty());

		final var result = invokeGetEdgeConfig(handler);

		assertFalse("cached config must have factories", result.getFactories().isEmpty());
	}

	/**
	 * Empty cached config + Edge online → live config fetched from Edge.
	 */
	@Test
	public void testGetEdgeConfig_fetchesLive_whenFactoriesEmptyAndOnline()
			throws OpenemsNamedException, ExecutionException, InterruptedException {
		final var liveConfig = buildConfigWithFactory();
		final var handler = buildHandler(EdgeConfig.empty(), true, liveConfig);

		final var result = invokeGetEdgeConfig(handler);

		assertFalse("live config must have factories", result.getFactories().isEmpty());
	}

	/**
	 * Empty cached config + Edge offline → empty cache returned, no live fetch.
	 */
	@Test
	public void testGetEdgeConfig_returnsEmptyCache_whenFactoriesEmptyAndOffline()
			throws OpenemsNamedException, ExecutionException, InterruptedException {
		final var handler = buildHandler(EdgeConfig.empty(), false, buildConfigWithFactory());

		final var result = invokeGetEdgeConfig(handler);

		assertTrue("offline: empty cache must be returned", result.getFactories().isEmpty());
	}

	// --- helpers ---

	private static EdgeConfig invokeGetEdgeConfig(EdgeRpcRequestHandler handler)
			throws OpenemsNamedException, ExecutionException, InterruptedException {
		final var request = new GetEdgeConfigRequest();
		final var edgeRpcRequest = new EdgeRpcRequest(EDGE_ID, request);
		final var edgeRpcResponse = handler.handleRequest(USER, UUID.randomUUID(), edgeRpcRequest).get();
		// EdgeRpcResponse.getResult() = { "payload": { "jsonrpc", "id", "result": <EdgeConfig json> } }
		final var configJson = edgeRpcResponse.getResult()
				.get("payload").getAsJsonObject()
				.get("result").getAsJsonObject();
		return EdgeConfig.fromJson(configJson);
	}

	private static EdgeRpcRequestHandler buildHandler(EdgeConfig cachedConfig, boolean edgeOnline,
			EdgeConfig liveConfig) {
		final var parent = new CoreJsonRpcRequestHandlerImpl();

		parent.metadata = new DummyMetadata(e -> { /* no-op: swallow edge online/offline events */ }) {
			@Override
			public Optional<Edge> getEdge(String edgeId) {
				final var edge = new DummyEdge(this, EDGE_ID, "", "", "", ZonedDateTime.now());
				edge.setOnline(edgeOnline);
				return Optional.of(edge);
			}

			@Override
			public EdgeHandler edge() {
				return edgeId -> cachedConfig;
			}

			@Override
			public Role getUserRole(User user, String edgeId) {
				return Role.GUEST;
			}
		};

		parent.edgeManager = new DummyEdgeManager(Collections.emptyMap()) {
			@Override
			public CompletableFuture<JsonrpcResponseSuccess> send(String edgeId, User user, Role role,
					JsonrpcRequest request) {
				return CompletableFuture.completedFuture(new GetEdgeConfigResponse(request.getId(), liveConfig));
			}
		};

		return new EdgeRpcRequestHandler(parent);
	}

	private static EdgeConfig buildConfigWithFactory() {
		return EdgeConfig.fromJson(JsonUtils.buildJsonObject()
				.add("components", JsonUtils.buildJsonObject().build())
				.add("factories", JsonUtils.buildJsonObject()
						.add("io.openems.impl.controller.api.websocket.WebsocketApiController",
								JsonUtils.buildJsonObject()
										.addProperty("id",
												"io.openems.impl.controller.api.websocket.WebsocketApiController")
										.add("properties", JsonUtils.buildJsonArray().build())
										.add("natureIds", JsonUtils.buildJsonArray().build())
										.build())
						.build())
				.build());
	}
}
