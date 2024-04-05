package io.openems.edge.common.channel;

import io.openems.common.channel.ChannelCategory;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.edge.common.component.OpenemsComponent;

public class StateChannelDoc extends BooleanDoc {

	private final Level level;

	public StateChannelDoc(Level level) {
		super();
		this.level = level;
		this.initialValue(false);
		this.persistencePriority(PersistencePriority.HIGH);
	}

	@Override
	public ChannelCategory getChannelCategory() {
		return ChannelCategory.STATE;
	}

	@Override
	protected StateChannelDoc self() {
		return this;
	}

	public Level getLevel() {
		return this.level;
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
	public StateChannel createChannelInstance(OpenemsComponent component,
			io.openems.edge.common.channel.ChannelId channelId) {
		return new StateChannel(component, channelId, this, this.level, this.debounce, this.debounceMode);
	}
}
