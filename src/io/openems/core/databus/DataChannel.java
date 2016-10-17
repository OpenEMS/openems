package io.openems.core.databus;

import io.openems.api.channel.Channel;
import io.openems.api.thing.Thing;

public class DataChannel {
	public final Channel channel;
	public final String channelId;
	public final Thing thing;
	public final String thingId;

	public DataChannel(Thing thing, String thingId, Channel channel, String channelId) {
		this.thing = thing;
		this.thingId = thingId;
		this.channel = channel;
		this.channelId = channelId;
	}

	@Override
	public String toString() {
		return "DataChannelMapping [channel=" + channel + ", channelId=" + channelId + "]";
	}
}
