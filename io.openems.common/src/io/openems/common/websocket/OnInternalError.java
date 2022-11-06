package io.openems.common.websocket;

@FunctionalInterface
public interface OnInternalError {

	/**
	 * Handles an internal error.
	 *
	 * @param t            the error {@link Throwable}
	 * @param wsDataString the content from WsData.toString()
	 */
	public void run(Throwable t, String wsDataString);

}
