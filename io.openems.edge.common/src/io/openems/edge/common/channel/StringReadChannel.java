package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public class StringReadChannel extends AbstractReadChannel<String> {

	public StringReadChannel(OpenemsComponent component, ChannelId channelId) {
		super(OpenemsType.STRING, component, channelId);
	}

	public StringReadChannel(OpenemsComponent component, ChannelId channelId, String initialValue) {
		super(OpenemsType.STRING, component, channelId, initialValue);
	}

}
