package io.openems.common.bridge.http.api;

public interface BridgeHttpEventRaiser {

	/**
	 * Raises an event with the given event definition and event data.
	 *
	 * @param <T>             The type of the event data.
	 * @param eventDefinition The definition of the event to be raised.
	 * @param eventData       The data associated with the event.
	 */
	<T> void raiseEvent(BridgeHttpEventDefinition<T> eventDefinition, T eventData);

}
