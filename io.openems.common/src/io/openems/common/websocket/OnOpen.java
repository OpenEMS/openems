package io.openems.common.websocket;

import java.util.function.BiFunction;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.Handshakedata;

import io.openems.common.exceptions.OpenemsError;

@FunctionalInterface
public interface OnOpen extends BiFunction<WebSocket, Handshakedata, OpenemsError> {

	public static final OnOpen NO_OP = (ws, handshakedata) -> {
		return null;
	};

	/**
	 * Handles OnOpen event of WebSocket.
	 *
	 * @param ws            the {@link WebSocket}
	 * @param handshakedata the {@link Handshakedata} with HTTP headers
	 * @return {@link OpenemsError} or null
	 */
	public OpenemsError apply(WebSocket ws, Handshakedata handshakedata);
}