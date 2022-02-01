package io.openems.common.websocket;

import org.java_websocket.WebSocket;

import io.openems.common.exceptions.OpenemsException;

@FunctionalInterface
public interface OnClose {

	/**
	 * Called after the websocket connection has been closed.
	 *
	 * @param ws     the {@link WebSocket}
	 * @param code   the close code
	 * @param reason the close reason
	 * @param remote Returns whether or not the closing of the connection was
	 *               initiated by the remote host
	 * @throws OpenemsException on error
	 */
	public void run(WebSocket ws, int code, String reason, boolean remote) throws OpenemsException;

}
