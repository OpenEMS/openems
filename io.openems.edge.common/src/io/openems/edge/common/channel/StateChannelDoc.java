package io.openems.edge.common.channel;

import io.openems.common.channel.ChannelCategory;
import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.AbstractDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class StateChannelDoc extends AbstractDoc<Boolean> {

	private final Level level;

	public StateChannelDoc(Level level) {
		super(OpenemsType.BOOLEAN);
		this.level = level;
		this.initialValue(false);
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

	/**
	 * Debounce the State-Channel value: the StateChannel is only set to true after
	 * it had been set to true for at least "debounce" times.
	 */
	private int debounce = 0;
	private Debounce debounceMode = Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE;

	public StateChannelDoc debounce(int debounce, Debounce debounceMode) {
		this.debounce = debounce;
		this.debounceMode = debounceMode;
		return this;
	}

	public int getDebounce() {
		return this.debounce;
	}

	public Debounce getDebounceMode() {
		return this.debounceMode;
	}
}
