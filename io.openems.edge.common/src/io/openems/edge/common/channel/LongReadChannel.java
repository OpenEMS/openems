package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class LongReadChannel extends AbstractReadChannel<OpenemsTypeDoc<Long>, Long> {

	protected LongReadChannel(OpenemsComponent component, ChannelId channelId, LongDoc channelDoc) {
		super(OpenemsType.LONG, component, channelId, channelDoc);
	}

}
