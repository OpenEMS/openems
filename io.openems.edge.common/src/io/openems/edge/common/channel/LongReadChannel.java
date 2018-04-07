package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public class LongReadChannel extends AbstractReadChannel<Long> {

	public LongReadChannel(OpenemsComponent component, ChannelId channelId) {
		super(OpenemsType.LONG, component, channelId);
	}

}
