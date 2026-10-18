package io.openems.common.websocket;

import java.util.concurrent.CompletableFuture;

import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

public interface WsConnection {
	/**
	 * Sends a JSON-RPC request to a Websocket and registers a callback.
	 *
	 * @param request the JSON-RPC Request
	 * @return a promise for a successful JSON-RPC Response
	 */
	public CompletableFuture<JsonrpcResponseSuccess> send(JsonrpcRequest request);

	/**
	 * Sends a JSON-RPC Notification to a WebSocket.
	 *
	 * @param notification the JSON-RPC Notification
	 * @return true if sending was successful; false otherwise
	 */
	boolean send(JsonrpcNotification notification);
}
