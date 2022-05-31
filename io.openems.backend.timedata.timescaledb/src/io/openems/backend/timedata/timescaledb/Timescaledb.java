package io.openems.backend.timedata.timescaledb;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;

public interface Timescaledb {

	// TODO replace with Timedata
	public void write(String edgeId, TreeBasedTable<Long, ChannelAddress, JsonElement> data) throws OpenemsException;

}
