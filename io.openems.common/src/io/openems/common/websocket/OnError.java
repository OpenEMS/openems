package io.openems.common.websocket;

import org.java_websocket.WebSocket;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingBiConsumer;

@FunctionalInterface
public interface OnError extends ThrowingBiConsumer<WebSocket, Exception, OpenemsException> {

	public static final OnError NO_OP = (ws, ex) -> {
	};

	/**
	 * Handles a websocket error.
	 *
	 * @param ws the {@link WebSocket}
	 * @param ex the {@link Exception}
	 * @throws OpenemsException on error
	 */
	public void accept(WebSocket ws, Exception ex) throws OpenemsException;

}
