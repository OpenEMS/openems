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
	 * Holds the Websocket. Possibly null!
	 */
	private WebSocket websocket = null;

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
		// Complete all pending requests
		this.requestFutures.values()
				.forEach(r -> r.completeExceptionally(new OpenemsException("Websocket connection closed.")));
		this.requestFutures.clear();
	}

	/**
	 * Sets the WebSocket.
	 *
	 * @param ws the WebSocket instance
	 */
	public synchronized void setWebsocket(WebSocket ws) {
		this.websocket = ws;
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
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<JsonrpcResponseSuccess> send(JsonrpcRequest request) throws OpenemsNamedException {
		var future = new CompletableFuture<JsonrpcResponseSuccess>();
		var existingFuture = this.requestFutures.putIfAbsent(request.getId(), future);
		if (existingFuture != null) {
			throw OpenemsError.JSONRPC_ID_NOT_UNIQUE.exception(request.getId());
		}
		this.sendMessage(request);
		return future;
	}

	/**
	 * Sends a JSON-RPC Notification to a WebSocket.
	 *
	 * @param notification the JSON-RPC Notification
	 * @throws OpenemsException on error
	 */
	public void send(JsonrpcNotification notification) throws OpenemsException {
		this.sendMessage(notification);
	}

	/**
	 * Sends the JSON-RPC message.
	 *
	 * @param message the JSON-RPC Message
	 * @throws OpenemsException on error
	 */
	private void sendMessage(JsonrpcMessage message) throws OpenemsException {
		if (this.websocket == null) {
			throw new OpenemsException("There is no Websocket defined for this WsData.");
		}
		try {
			this.websocket.send(message.toString());
		} catch (WebsocketNotConnectedException e) {
			throw new OpenemsException("Websocket is not connected: " + e.getMessage());
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
