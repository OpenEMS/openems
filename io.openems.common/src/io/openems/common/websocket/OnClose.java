package io.openems.common.websocket;

import org.java_websocket.WebSocket;

@FunctionalInterface
public interface OnClose {

	public static final OnClose NO_OP = (ws, code, reason, remote) -> {
	};

	/**
	 * Called after the websocket connection has been closed.
	 *
	 * @param ws     the {@link WebSocket}
	 * @param code   the close code
	 * @param reason the close reason
	 * @param remote Returns whether or not the closing of the connection was
	 *               initiated by the remote host
	 */
	public void accept(WebSocket ws, int code, String reason, boolean remote);

}
