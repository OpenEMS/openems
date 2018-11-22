package io.openems.common.websocket;

@FunctionalInterface
public interface OnInternalError {

	/**
	 * Handles an internal error.
	 * 
	 * @param ex
	 */
	public void run(Exception ex);

}
