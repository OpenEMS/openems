package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class BooleanReadChannel extends AbstractReadChannel<OpenemsTypeDoc<Boolean>, Boolean> {

	protected BooleanReadChannel(OpenemsComponent component, ChannelId channelId, BooleanDoc channelDoc) {
		super(OpenemsType.BOOLEAN, component, channelId, channelDoc);
	}

}
