package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class StringReadChannel extends AbstractReadChannel<OpenemsTypeDoc<String>, String> {

	protected StringReadChannel(OpenemsComponent component, ChannelId channelId, StringDoc channelDoc) {
		super(OpenemsType.STRING, component, channelId, channelDoc);
	}

}
