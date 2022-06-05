package io.openems.backend.timedata.timescaledb;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;

public interface Timescaledb {

	// TODO replace with Timedata

	/**
	 * Sends the data points to the Timedata service. Will be replaced with Timedata
	 * eventually.
	 *
	 * @param edgeId The unique Edge-ID
	 * @param data   Table of timestamp (epoch in milliseconds), Channel-Address and
	 *               the Channel value as JsonElement. Sorted by timestamp.
	 * @throws OpenemsException on error
	 */
	public void write(String edgeId, TreeBasedTable<Long, ChannelAddress, JsonElement> data) throws OpenemsException;

}
