package io.openems.edge.common.channel.internal;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Level;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Represents a single state. Changes to the value are reported to the
 * {@link StateCollectorChannel} "State" of the OpenEMS Component.
 */
public class StateChannel extends AbstractReadChannel<AbstractDoc<Boolean>, Boolean> {

	private final Level level;

	protected StateChannel(OpenemsComponent component, ChannelId channelId, AbstractDoc<Boolean> channelDoc,
			Level level) {
		super(OpenemsType.BOOLEAN, component, channelId, channelDoc, false);
		this.level = level;
	}

	/**
	 * Gets the Level of this {@link StateChannel}.
	 * 
	 * @return the level
	 */
	public Level getLevel() {
		return level;
	}

}
