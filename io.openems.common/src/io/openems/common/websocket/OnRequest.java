package io.openems.common.websocket;

import java.util.concurrent.CompletableFuture;

import org.java_websocket.WebSocket;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

@FunctionalInterface
public interface OnRequest {

	/**
	 * Handle a JSON-RPC Request, receive a JSON-RPC Response via callback.
	 *
	 * @param ws      the {@link WebSocket}
	 * @param request the JSON-RPC Request
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<? extends JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
			throws OpenemsNamedException;

}
