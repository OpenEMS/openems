package io.openems.backend.edge.manager;

import java.util.concurrent.CompletableFuture;

import io.openems.backend.common.edge.EdgeCache;
import io.openems.backend.common.edge.EdgeClient;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;

public class BackendEdgeClient implements EdgeClient {
	private final String edgeId;
	private final WsData edgeServerConnection;
	private final EdgeCache cache = new EdgeCache();

	BackendEdgeClient(String edgeId, WsData edgeServerConnection) {
		this.edgeId = edgeId;
		this.edgeServerConnection = edgeServerConnection;
	}

	@Override
	public String getEdgeId() {
		return this.edgeId;
	}

	public EdgeCache getCache() {
		return this.cache;
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> send(JsonrpcRequest request) {
		return this.edgeServerConnection.send(new EdgeRpcRequest(this.edgeId, request));
	}

	@Override
	public boolean send(JsonrpcNotification notification) {
		return this.edgeServerConnection.send(new EdgeRpcNotification(this.edgeId, notification));
	}
}
