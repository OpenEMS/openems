package io.openems.edge.common.channel.internal;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class StateCollectorChannelDoc extends EnumDoc {

	public StateCollectorChannelDoc() {
		super(Level.values());
	}

	/**
	 * Creates an instance of {@link Channel} for the given Channel-ID using its
	 * Channel-{@link Doc}.
	 *
	 * @param channelId the Channel-ID
	 * @return the Channel
	 */
	@SuppressWarnings("unchecked")
	@Override
	public StateCollectorChannel createChannelInstance(OpenemsComponent component,
			io.openems.edge.common.channel.ChannelId channelId) {
		return new StateCollectorChannel(component, channelId, this);
	}
}
