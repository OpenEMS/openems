package io.openems.common.websocket;

@FunctionalInterface
public interface OnInternalError {

	/**
	 * Handles an internal error.
	 *
	 * @param ex           the thrown Exception
	 * @param wsDataString the content from WsData.toString()
	 */
	public void run(Exception ex, String wsDataString);

}
