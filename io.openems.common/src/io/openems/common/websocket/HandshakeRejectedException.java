package io.openems.common.websocket;

/**
 * Thrown by {@link AbstractWebsocketServer#onHandshake(WebSocket, Handshake)}
 * to reject a WebSocket handshake. The client receives HTTP 403 Forbidden.
 */
public class HandshakeRejectedException extends Exception {

	private static final long serialVersionUID = 1L;

	public HandshakeRejectedException(String message) {
		super(message);
	}
}
