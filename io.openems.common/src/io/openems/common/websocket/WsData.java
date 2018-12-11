package io.openems.common.websocket;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.Error;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;

/**
 * Objects of this class are used to store additional data with websocket
 * connections of WebSocketClient and WebSocketServer
 */
public class WsData {

	/**
	 * Holds the websocket. Possibly null!
	 */
	private WebSocket websocket = null;

	/**
	 * Holds callbacks for JSON-RPC Requests
	 */
	private final ConcurrentHashMap<UUID, Consumer<JsonrpcResponse>> callbacks = new ConcurrentHashMap<>();

	/**
	 * This method is called on close of the parent websocket. Use it to release
	 * blocked resources.
	 */
	public void dispose() {
		// nothing here
	}

	/**
	 * Sets the websocket.
	 * 
	 * @param ws
	 */
	public synchronized void setWebsocket(WebSocket ws) {
		this.websocket = ws;
	}

	/**
	 * Gets the websocket. Possibly null!
	 * 
	 * @return
	 */
	public WebSocket getWebsocket() {
		return websocket;
	}

	/**
	 * Sends a JSON-RPC request to a Websocket and registers a callback.
	 * 
	 * @param ws
	 * @param request
	 * @param responseCallback
	 */
	public void send(JsonrpcRequest request, Consumer<JsonrpcResponse> responseCallback) throws OpenemsException {
		Consumer<JsonrpcResponse> existingCallback = this.callbacks.putIfAbsent(request.getId(), responseCallback);
		if (existingCallback != null) {
			responseCallback.accept(Error.ID_NOT_UNIQUE.asJsonrpc(request.getId(), request.getId()));
		} else {
			this.sendMessage(request);
		}
	}

	/**
	 * Sends a JSON-RPC notification to a Websocket.
	 * 
	 * @param ws
	 * @param notification
	 * @throws OpenemsException
	 */
	public void send(JsonrpcNotification notification) throws OpenemsException {
		this.sendMessage(notification);
	}

	/**
	 * Sends the JSON-RPC message.
	 * 
	 * @param message
	 * @throws OpenemsException
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
	 * Handles a JSON-RPC response by calling the previously registers request
	 * callback.
	 * 
	 * @param response
	 * @throws OpenemsException
	 */
	public void handleJsonrpcResponse(JsonrpcResponse response) throws OpenemsException {
		Consumer<JsonrpcResponse> callback = this.callbacks.remove(response.getId());
		if (callback != null) {
			// this was a response on a request
			callback.accept(response);
		} else {
			// this was a response without a request
			throw new OpenemsException("Got Response without Request: " + response.toString());
		}
	}

}
