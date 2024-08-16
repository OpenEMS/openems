package io.openems.common.websocket;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseError;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

/**
 * Objects of this class are used to store additional data with websocket
 * connections of WebSocketClient and WebSocketServer.
 */
public abstract class WsData {

	/**
	 * Holds the WebSocket.
	 */
	private final WebSocket websocket;

	protected WsData(WebSocket ws) {
		this.websocket = ws;
	}

	/**
	 * Holds Futures for JSON-RPC Requests.
	 */
	// TODO add timeout to requestFutures
	private final ConcurrentHashMap<UUID, CompletableFuture<JsonrpcResponseSuccess>> requestFutures = new ConcurrentHashMap<>();

	/**
	 * This method is called on close of the parent websocket. Use it to release
	 * blocked resources.
	 */
	public void dispose() {
		final var e = new OpenemsException("Websocket connection closed");
		// Complete all pending requests
		this.requestFutures.values().forEach(r -> r.completeExceptionally(e));
		this.requestFutures.clear();
	}

	/**
	 * Gets the WebSocket. Possibly null!
	 *
	 * @return the WebSocket instance
	 */
	public WebSocket getWebsocket() {
		return this.websocket;
	}

	/**
	 * Sends a JSON-RPC request to a Websocket and registers a callback.
	 *
	 * @param request the JSON-RPC Request
	 * @return a promise for a successful JSON-RPC Response
	 */
	public CompletableFuture<JsonrpcResponseSuccess> send(JsonrpcRequest request) {
		var future = new CompletableFuture<JsonrpcResponseSuccess>();
		var existingFuture = this.requestFutures.putIfAbsent(request.getId(), future);
		if (existingFuture != null) {
			return CompletableFuture.failedFuture(OpenemsError.JSONRPC_ID_NOT_UNIQUE.exception(request.getId()));
		}
		if (!this.sendMessage(request)) {
			future.completeExceptionally(OpenemsError.JSONRPC_SEND_FAILED.exception());
		}
		return future;
	}

	/**
	 * Sends a JSON-RPC Notification to a WebSocket.
	 *
	 * @param notification the JSON-RPC Notification
	 * @return true if sending was successful; false otherwise
	 */
	public boolean send(JsonrpcNotification notification) {
		return this.sendMessage(notification);
	}

	/**
	 * Sends the JSON-RPC message.
	 *
	 * @param message the JSON-RPC Message
	 * @return true if sending was successful; false otherwise
	 */
	private boolean sendMessage(JsonrpcMessage message) {
		if (!this.websocket.isOpen()) {
			return false;
		}
		try {
			this.websocket.send(message.toString());
			return true;
		} catch (WebsocketNotConnectedException e) {
			// handles corner cases
			return false;
		}
	}

	/**
	 * Handles a JSON-RPC response by completing the previously registers request
	 * Future.
	 *
	 * @param response the JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	public void handleJsonrpcResponse(JsonrpcResponse response) throws OpenemsNamedException {
		var future = this.requestFutures.remove(response.getId());
		if (future == null) {
			// this was a response without a request
			throw OpenemsError.JSONRPC_RESPONSE_WITHOUT_REQUEST.exception(response.toJsonObject());
		}
		// this was a response on a request
		if (response instanceof JsonrpcResponseSuccess) {
			// Success Response -> complete future
			future.complete((JsonrpcResponseSuccess) response);

		} else if (response instanceof JsonrpcResponseError) {
			// Named OpenEMS-Error Response -> cancel future
			var error = (JsonrpcResponseError) response;
			var exception = new OpenemsNamedException(error.getOpenemsError(), error.getParamsAsObjectArray());
			future.completeExceptionally(exception);

		} else {
			// Undefined Error Response -> cancel future
			var exception = new OpenemsNamedException(OpenemsError.GENERIC,
					"Response is neither JsonrpcResponseSuccess nor JsonrpcResponseError: " + response.toString());
			future.completeExceptionally(exception);
		}
	}

	/**
	 * Provides a specific toString method.
	 *
	 * @return a specific string for this instance
	 */
	@Override
	public abstract String toString();
}
