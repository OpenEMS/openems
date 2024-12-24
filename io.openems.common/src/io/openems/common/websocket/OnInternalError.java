package io.openems.common.websocket;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface OnInternalError extends BiConsumer<Throwable, String> {

	/**
	 * Handles an internal error.
	 *
	 * @param t            the error {@link Throwable}
	 * @param wsDataString the content from WsData.toString()
	 */
	public void accept(Throwable t, String wsDataString);

}
