package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public class ShortReadChannel extends AbstractReadChannel<Short> {

	public ShortReadChannel(OpenemsComponent component, ChannelId channelId) {
		super(OpenemsType.SHORT, component, channelId);
	}

	public ShortReadChannel(OpenemsComponent component, ChannelId channelId, Short initialValue) {
		super(OpenemsType.SHORT, component, channelId, initialValue);
	}


}
