package io.openems.edge.simulator.datasource.api;

import java.util.Set;

import io.openems.common.types.OpenemsType;

public interface SimulatorDatasource {

	/**
	 * Gets the available keys.
	 * 
	 * @return the Channel-Id
	 */
	Set<String> getKeys();

	/**
	 * Returns the delta between two values in seconds.
	 * 
	 * @return the delta in seconds
	 */
	int getTimeDelta();

	/**
	 * Gets the value for the given key (channelId) in the given type.
	 * 
	 * @param <T>       the type
	 * @param type      the expected type
	 * @param channelId the Channel-Address
	 * @return the value
	 */
	<T> T getValue(OpenemsType type, String channelAddress);

}
