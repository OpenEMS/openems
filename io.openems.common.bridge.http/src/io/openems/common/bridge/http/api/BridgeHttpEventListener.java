package io.openems.common.bridge.http.api;

public interface BridgeHttpEventListener<T> {

	/**
	 * Called when the event occurs.
	 *
	 * @param eventData the event data
	 */
	void onEvent(T eventData);

}