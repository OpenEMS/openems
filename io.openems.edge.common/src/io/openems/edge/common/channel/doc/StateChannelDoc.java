package io.openems.edge.common.channel.doc;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class StateChannelDoc extends Doc {

	public StateChannelDoc(Level level) {
		this.level(level);
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
		// TODO set level of StateChannel via Constructor
		return new StateChannel(component, channelId);
	}

}
