package io.openems.edge.io.phoenixcontact.gds;

import io.openems.edge.common.channel.ChannelId;

public class PlcNextGdsDataMappedValue {

	private final ChannelId channelId;
	private final Object value;

	public PlcNextGdsDataMappedValue(ChannelId channelId, Object value) {
		this.channelId = channelId;
		this.value = value;
	}

	public ChannelId getChannelId() {
		return this.channelId;
	}

	public Object getValue() {
		return this.value;
	}

}
