package io.openems.edge.simulator.datasource.api;

import java.util.Set;

import io.openems.common.types.OpenemsType;

public interface SimulatorDatasource {

	/**
	 * Gets the available keys
	 * 
	 * @return
	 */
	Set<String> getKeys();

	/**
	 * Returns the delta between two values in seconds
	 * 
	 * @return
	 */
	int getTimeDelta();

	/**
	 * Gets the value for the given key (channelId) in the given type
	 * 
	 * @param type
	 * @param channelId
	 * @return
	 */
	<T> T getValue(OpenemsType type, String channelId);

}
