package io.openems.edge.common.channel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public class StateChannel extends AbstractReadChannel<Integer> {

	private final Map<io.openems.edge.common.channel.doc.ChannelId, Channel<?>> channels = Collections
			.synchronizedMap(new HashMap<>());

	public StateChannel(OpenemsComponent component, ChannelId channelId) {
		super(OpenemsType.INTEGER, component, channelId);
	}

	protected void addChannel(Channel<?> channel) {
		if (channel == null) {
			throw new NullPointerException(
					"Trying to add 'null' Channel. Hint: Check for missing handling of Enum value.");
		}
		this.channels.put(channel.channelId(), channel);
	}
}
