package io.openems.common.websocket;

@FunctionalInterface
public interface OnInternalError {

	public void run(Exception ex);

}
