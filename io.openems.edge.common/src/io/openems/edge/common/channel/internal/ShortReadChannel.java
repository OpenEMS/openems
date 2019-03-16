package io.openems.edge.common.channel.internal;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public class ShortReadChannel extends AbstractReadChannel<OpenemsTypeDoc<Short>, Short> {

	protected ShortReadChannel(OpenemsComponent component, ChannelId channelId, ShortDoc channelDoc) {
		this(component, channelId, channelDoc, null);
	}

	protected ShortReadChannel(OpenemsComponent component, ChannelId channelId, ShortDoc channelDoc,
			Short initialValue) {
		super(OpenemsType.SHORT, component, channelId, channelDoc, initialValue);
	}

}
