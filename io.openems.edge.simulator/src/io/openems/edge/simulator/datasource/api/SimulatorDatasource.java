package io.openems.edge.simulator.datasource.api;

import java.util.List;
import java.util.Set;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;

public interface SimulatorDatasource {

	/**
	 * Gets the available keys.
	 *
	 * @return the Channel-Id
	 */
	public Set<String> getKeys();

	/**
	 * Returns the delta between two values in seconds.
	 *
	 * @return the delta in seconds
	 */
	public int getTimeDelta();

	/**
	 * Gets all values for the given key (channelId) in the given type.
	 *
	 * @param <T>            the type
	 * @param type           the expected type
	 * @param channelAddress the Channel-Address
	 * @return the values, possibly empty
	 */
	public <T> List<T> getValues(OpenemsType type, ChannelAddress channelAddress);

	/**
	 * Gets the value for the given key (channelId) in the given type.
	 *
	 * @param <T>            the type
	 * @param type           the expected type
	 * @param channelAddress the Channel-Address
	 * @return the value, possibly null
	 */
	public <T> T getValue(OpenemsType type, ChannelAddress channelAddress);

}
