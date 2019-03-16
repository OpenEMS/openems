package io.openems.edge.common.channel.internal;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public class LongReadChannel extends AbstractReadChannel<OpenemsTypeDoc<Long>, Long> {

	protected LongReadChannel(OpenemsComponent component, ChannelId channelId, LongDoc channelDoc) {
		this(component, channelId, channelDoc, null);
	}

	protected LongReadChannel(OpenemsComponent component, ChannelId channelId, LongDoc channelDoc, Long initialValue) {
		super(OpenemsType.LONG, component, channelId, channelDoc, initialValue);
	}

}
