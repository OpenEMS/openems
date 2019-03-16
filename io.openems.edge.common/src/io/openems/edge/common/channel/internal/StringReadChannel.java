package io.openems.edge.common.channel.internal;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public class StringReadChannel extends AbstractReadChannel<OpenemsTypeDoc<String>, String> {

	protected StringReadChannel(OpenemsComponent component, ChannelId channelId, StringDoc channelDoc) {
		this(component, channelId, channelDoc, null);
	}

	protected StringReadChannel(OpenemsComponent component, ChannelId channelId, StringDoc channelDoc,
			String initialValue) {
		super(OpenemsType.STRING, component, channelId, channelDoc, initialValue);
	}

}
