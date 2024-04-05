package io.openems.common.websocket;

import org.java_websocket.WebSocket;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

@FunctionalInterface
public interface OnOpen {

	/**
	 * Handles OnOpen event of WebSocket.
	 *
	 * @param ws        the WebSocket
	 * @param handshake the HTTP handshake/headers
	 * @throws OpenemsNamedException on error
	 */
	public void run(WebSocket ws, JsonObject handshake) throws OpenemsNamedException;

}
