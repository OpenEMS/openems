package io.openems.core.databus;

import io.openems.api.channel.Channel;

public class DataChannelMapping {
	private final Integer address;
	private final Channel channel;
	private final String channelId;

	public DataChannelMapping(Channel channel, String channelId, int address) {
		this.address = address;
		this.channel = channel;
		this.channelId = channelId;
	}

	public Integer getAddress() {
		return address;
	}

	public Channel getChannel() {
		return channel;
	}

	public String getChannelId() {
		return channelId;
	}

	@Override
	public String toString() {
		return "DataChannel [address=" + address + ", channel=" + channel + ", channelId=" + channelId + "]";
	}
}
