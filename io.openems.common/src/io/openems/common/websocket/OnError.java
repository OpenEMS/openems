package io.openems.common.websocket;

import org.java_websocket.WebSocket;

import io.openems.common.exceptions.OpenemsException;

@FunctionalInterface
public interface OnError {

	/**
	 * Handles a websocket error.
	 *
	 * @param ws the {@link WebSocket}
	 * @param ex the {@link Exception}
	 * @throws OpenemsException on error
	 */
	public void run(WebSocket ws, Exception ex) throws OpenemsException;

}
