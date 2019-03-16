package io.openems.edge.common.channel.internal;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public class FloatReadChannel extends AbstractReadChannel<OpenemsTypeDoc<Float>, Float> {

	protected FloatReadChannel(OpenemsComponent component, ChannelId channelId, FloatDoc channelDoc) {
		this(component, channelId, channelDoc, null);
	}

	protected FloatReadChannel(OpenemsComponent component, ChannelId channelId, FloatDoc channelDoc,
			Float initialValue) {
		super(OpenemsType.FLOAT, component, channelId, channelDoc, initialValue);
	}

}
