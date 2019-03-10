package io.openems.edge.common.channel.doc;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class StateCollectorChannelDoc extends OptionsEnumDoc {

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
	@Override
	public Channel<?> createChannelInstance(OpenemsComponent component,
			io.openems.edge.common.channel.doc.ChannelId channelId) {
		return new StateCollectorChannel(component, channelId);
	}
}
