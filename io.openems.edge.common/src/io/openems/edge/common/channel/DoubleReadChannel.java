package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public class DoubleReadChannel extends AbstractReadChannel<Double> {

	public DoubleReadChannel(OpenemsComponent component, ChannelId channelId) {
		super(OpenemsType.DOUBLE, component, channelId);
	}

	public DoubleReadChannel(OpenemsComponent component, ChannelId channelId, Double initialValue) {
		super(OpenemsType.DOUBLE, component, channelId, initialValue);
	}

}
