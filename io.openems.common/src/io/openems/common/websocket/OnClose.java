package io.openems.common.websocket;

import org.java_websocket.WebSocket;

import io.openems.common.exceptions.OpenemsException;

@FunctionalInterface
public interface OnClose {

	/**
	 * Handles a websocket OnClose event.
	 * 
	 * @param ws
	 * @param code
	 * @param reason
	 * @param remote
	 * @throws OpenemsException
	 */
	public void run(WebSocket ws, int code, String reason, boolean remote) throws OpenemsException;

}
