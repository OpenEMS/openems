package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class ShortReadChannel extends AbstractReadChannel<OpenemsTypeDoc<Short>, Short> {

	protected ShortReadChannel(OpenemsComponent component, ChannelId channelId, ShortDoc channelDoc) {
		super(OpenemsType.SHORT, component, channelId, channelDoc);
	}

}
