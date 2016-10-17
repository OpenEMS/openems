package io.openems.api.channel;

public class WriteableChannelBuilder extends ChannelBuilder<WriteableChannelBuilder> {
	@Override
	public WriteableChannel build() {
		return new WriteableChannel(unit, minValue, maxValue);
	}
}
