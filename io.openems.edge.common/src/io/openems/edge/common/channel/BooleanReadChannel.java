package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public class BooleanReadChannel extends AbstractReadChannel<Integer> {

	public BooleanReadChannel(OpenemsComponent component, ChannelId channelId) {
		super(OpenemsType.BOOLEAN, component, channelId);
	}

}
