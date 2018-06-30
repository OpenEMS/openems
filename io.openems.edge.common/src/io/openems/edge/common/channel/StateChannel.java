package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Represents a single state. Changes to the value are reported to the
 * {@link StateCollectorChannel} "State" of the OpenEMS Component.
 */
public class StateChannel extends AbstractReadChannel<Boolean> {

	public StateChannel(OpenemsComponent component, ChannelId channelId) {
		super(OpenemsType.BOOLEAN, component, channelId);
	}

}
