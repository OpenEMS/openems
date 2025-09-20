package io.openems.common.websocket;

import org.java_websocket.WebSocket;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingBiConsumer;
import io.openems.common.jsonrpc.base.JsonrpcNotification;

@FunctionalInterface
public interface OnNotification extends ThrowingBiConsumer<WebSocket, JsonrpcNotification, OpenemsNamedException> {

	public static final OnNotification NO_OP = (ws, notification) -> {
	};

	/**
	 * Handles a JSON-RPC notification.
	 *
	 * @param websocket    the WebSocket
	 * @param notification the JSON-RPC Notification
	 * @throws OpenemsNamedException on error
	 */
	public void accept(WebSocket websocket, JsonrpcNotification notification) throws OpenemsNamedException;

}
