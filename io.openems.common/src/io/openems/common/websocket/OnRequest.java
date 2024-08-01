package io.openems.common.websocket;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletableFuture;

import org.java_websocket.WebSocket;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

@FunctionalInterface
public interface OnRequest extends
		ThrowingBiFunction<WebSocket, JsonrpcRequest, CompletableFuture<? extends JsonrpcResponseSuccess>, OpenemsNamedException> {

	public static final OnRequest NO_OP = (ws, request) -> {
		return completedFuture(new GenericJsonrpcResponseSuccess(request.id));
	};

	/**
	 * Handle a JSON-RPC Request, receive a JSON-RPC Response via callback.
	 *
	 * @param ws      the {@link WebSocket}
	 * @param request the JSON-RPC Request
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<? extends JsonrpcResponseSuccess> apply(WebSocket ws, JsonrpcRequest request)
			throws OpenemsNamedException;

}
