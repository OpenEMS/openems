package io.openems.edge.common.channel.internal;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public class BooleanReadChannel extends AbstractReadChannel<OpenemsTypeDoc<Boolean>, Boolean> {

	protected BooleanReadChannel(OpenemsComponent component, ChannelId channelId, BooleanDoc channelDoc) {
		this(component, channelId, channelDoc, null);
	}

	protected BooleanReadChannel(OpenemsComponent component, ChannelId channelId, BooleanDoc channelDoc,
			Boolean initialValue) {
		super(OpenemsType.BOOLEAN, component, channelId, channelDoc, initialValue);
	}

}
