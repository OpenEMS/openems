package io.openems.common.websocket;

import java.util.function.BiFunction;


import io.openems.common.exceptions.OpenemsError;

@FunctionalInterface
public interface OnOpen extends BiFunction<WebSocket, Handshake, OpenemsError> {

	public static final OnOpen NO_OP = (ws, handshake) -> {
		return null;
	};

	/**
	 * Handles OnOpen event of WebSocket.
	 *
	 * @param ws            the {@link WebSocket}
	 * @param handshake the {@link Handshake} with HTTP headers
	 * @return {@link OpenemsError} or null
	 */
	public OpenemsError apply(WebSocket ws, Handshake handshake);
}