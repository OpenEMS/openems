package io.openems.edge.common.channel;

import io.openems.common.channel.Debounce;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class BooleanDoc extends OpenemsTypeDoc<Boolean> {

	public BooleanDoc() {
		super(OpenemsType.BOOLEAN);
	}

	@Override
	protected BooleanDoc self() {
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public BooleanReadChannel createChannelInstance(OpenemsComponent component, ChannelId channelId) {
		switch (this.getAccessMode()) {
		case READ_ONLY:
			return new BooleanReadChannel(component, channelId, this, this.debounce, this.debounceMode);
		case READ_WRITE:
		case WRITE_ONLY:
			return new BooleanWriteChannel(component, channelId, this);
		}
		throw new IllegalArgumentException(
				"AccessMode [" + this.getAccessMode() + "] is unhandled. This should never happen.");
	}

	/**
	 * Debounce the State-Channel value: the StateChannel is only set to true after
	 * it had been set to true for at least "debounce" times.
	 */
	protected int debounce = 0;
	protected Debounce debounceMode = null;

	/**
	 * Add a debounce before actually setting the channel value.
	 * 
	 * @param debounce     the debounce counter
	 * @param debounceMode the {@link Debounce} mode
	 * @return myself
	 */
	public BooleanDoc debounce(int debounce, Debounce debounceMode) {
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
