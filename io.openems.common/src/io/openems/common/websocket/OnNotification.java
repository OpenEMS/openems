package io.openems.common.websocket;

import org.java_websocket.WebSocket;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;

@FunctionalInterface
public interface OnNotification {

	/**
	 * Handles a JSON-RPC notification.
	 *
	 * @param websocket    the WebSocket
	 * @param notification the JSON-RPC Notification
	 * @throws OpenemsNamedException on error
	 */
	public void run(WebSocket websocket, JsonrpcNotification notification) throws OpenemsNamedException;

}
