package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class IntegerReadChannel extends AbstractReadChannel<OpenemsTypeDoc<Integer>, Integer> {

	protected IntegerReadChannel(OpenemsComponent component, ChannelId channelId, IntegerDoc channelDoc) {
		this(component, channelId, channelDoc, null);
	}

	protected IntegerReadChannel(OpenemsComponent component, ChannelId channelId, IntegerDoc channelDoc,
			Integer initialValue) {
		super(OpenemsType.INTEGER, component, channelId, channelDoc, initialValue);
	}

}
