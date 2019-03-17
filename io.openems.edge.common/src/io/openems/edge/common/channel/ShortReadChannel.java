package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.channel.internal.ShortDoc;
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
