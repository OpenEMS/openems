package io.openems.edge.common.channel.internal;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.Level;
import io.openems.edge.common.component.OpenemsComponent;

public class StateChannelDoc extends AbstractDoc<Boolean> {

	private final Level level;

	public StateChannelDoc(Level level) {
		super(OpenemsType.BOOLEAN);
		this.level = level;
	}

	@Override
	protected StateChannelDoc self() {
		return this;
	}

	/**
	 * Creates an instance of {@link Channel} for the given Channel-ID using its
	 * Channel-{@link Doc}.
	 * 
	 * @param channelId the Channel-ID
	 * @return the Channel
	 */
	@Override
	public StateChannel createChannelInstance(OpenemsComponent component,
			io.openems.edge.common.channel.ChannelId channelId) {
		return new StateChannel(component, channelId, this, this.level);
	}
}
