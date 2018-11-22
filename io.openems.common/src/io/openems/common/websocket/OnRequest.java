package io.openems.common.websocket;

import java.util.function.Consumer;

import org.java_websocket.WebSocket;

import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;

@FunctionalInterface
public interface OnRequest {

	/**
	 * Handle a JSON-RPC request, receive a JSON-RPC response via callback.
	 * 
	 * @param ws
	 * @param request
	 * @param responseCallback
	 */
	public void run(WebSocket ws, JsonrpcRequest request, Consumer<JsonrpcResponse> responseCallback);

}
