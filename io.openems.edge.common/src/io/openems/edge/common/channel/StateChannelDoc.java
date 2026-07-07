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

	protected StateChannel createChannelInstance(OpenemsComponent component,
			io.openems.edge.common.channel.ChannelId channelId, BooleanDoc channelDoc) {
		return new StateChannel(component, channelId, channelDoc, this.level, this.debounce, this.debounceMode);
	}

	@SuppressWarnings("unchecked")
	@Override
	public StateChannel createChannelInstance(OpenemsComponent component,
			io.openems.edge.common.channel.ChannelId channelId) {
		return this.createChannelInstance(component, channelId, this);
	}
}
