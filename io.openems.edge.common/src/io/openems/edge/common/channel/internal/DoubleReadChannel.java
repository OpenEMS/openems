package io.openems.edge.common.channel.internal;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public class DoubleReadChannel extends AbstractReadChannel<OpenemsTypeDoc<Double>, Double> {

	protected DoubleReadChannel(OpenemsComponent component, ChannelId channelId, DoubleDoc channelDoc) {
		this(component, channelId, channelDoc, null);
	}

	protected DoubleReadChannel(OpenemsComponent component, ChannelId channelId, DoubleDoc channelDoc,
			Double initialValue) {
		super(OpenemsType.DOUBLE, component, channelId, channelDoc, initialValue);
	}

}
