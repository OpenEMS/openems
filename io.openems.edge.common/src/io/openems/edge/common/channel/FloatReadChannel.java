package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public class FloatReadChannel extends AbstractReadChannel<Float> {

	public FloatReadChannel(OpenemsComponent component, ChannelId channelId) {
		super(OpenemsType.FLOAT, component, channelId);
	}

	public FloatReadChannel(OpenemsComponent component, ChannelId channelId, Float initialValue) {
		super(OpenemsType.FLOAT, component, channelId, initialValue);
	}

}
