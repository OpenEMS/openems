package io.openems.backend.timedata.timescaledb;

import com.google.gson.JsonElement;

import io.openems.common.types.ChannelAddress;

public class Point {

	public final long timestamp;
	public final String edgeId;
	public final ChannelAddress channelAddress;
	public final JsonElement value;

	public Point(long timestamp, String edgeId, ChannelAddress channelAddress, JsonElement value) {
		this.timestamp = timestamp;
		this.edgeId = edgeId;
		this.channelAddress = channelAddress;
		this.value = value;
	}
}