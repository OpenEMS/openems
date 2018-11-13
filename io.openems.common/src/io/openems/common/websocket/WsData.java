package io.openems.common.websocket;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.java_websocket.WebSocket;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;

/**
 * Objects of this class are used to store additional data with websocket
 * connections of WebSocketClient and WebSocketServer
 */
public class WsData {

	private final ConcurrentHashMap<UUID, Consumer<JsonrpcResponse>> callbacks = new ConcurrentHashMap<>();

	/**
	 * Sends a JsonrpcRequest to a Websocket and registers a callback.
	 * 
	 * @param ws
	 * @param request
	 * @param responseCallback
	 * @throws OpenemsException
	 */
	public void send(WebSocket ws, JsonrpcRequest request, Consumer<JsonrpcResponse> responseCallback)
			throws OpenemsException {
		Consumer<JsonrpcResponse> existingCallback = this.callbacks.putIfAbsent(request.getId(), responseCallback);
		if (existingCallback != null) {
			throw new OpenemsException("A Request with this ID [" + request.getId() + "] had already been existing");
		} else {
			ws.send(request.toString());
		}
	}

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
