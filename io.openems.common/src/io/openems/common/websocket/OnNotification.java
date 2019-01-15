package io.openems.common.websocket;

import org.java_websocket.WebSocket;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;

@FunctionalInterface
public interface OnNotification {

	/**
	 * Handles a JSON-RPC notification.
	 * 
	 * @param ws
	 * @param notification
	 * @throws OpenemsException
	 */
	public void run(WebSocket ws, JsonrpcNotification notification) throws OpenemsNamedException;

}
