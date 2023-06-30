package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class IntegerReadChannel extends AbstractReadChannel<OpenemsTypeDoc<Integer>, Integer> {

	protected IntegerReadChannel(OpenemsComponent component, ChannelId channelId, IntegerDoc channelDoc) {
		super(OpenemsType.INTEGER, component, channelId, channelDoc);
	}

}
