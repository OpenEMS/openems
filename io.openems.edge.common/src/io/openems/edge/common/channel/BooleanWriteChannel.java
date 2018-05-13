package io.openems.edge.common.channel;

import java.util.Optional;
import java.util.function.Consumer;

import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public class BooleanWriteChannel extends BooleanReadChannel implements WriteChannel<Boolean> {

	public BooleanWriteChannel(OpenemsComponent component, ChannelId channelId) {
		super(component, channelId);
	}

	private Optional<Boolean> nextWriteValueOpt = Optional.empty();

	/**
	 * Internal method. Do not call directly.
	 * 
	 * @param value
	 */
	@Deprecated
	@Override
	public void _setNextWriteValue(Boolean value) {
		this.nextWriteValueOpt = Optional.ofNullable(value);
	}

	/**
	 * Internal method. Do not call directly.
	 * 
	 * @param value
	 */
	@Deprecated
	@Override
	public Optional<Boolean> _getNextWriteValue() {
		return this.nextWriteValueOpt;
	}

	/*
	 * onSetNextWrite
	 */
	private Consumer<Boolean> onSetNextWriteCallback = null;

	@Override
	public Consumer<Boolean> getOnSetNextWrite() {
		return this.onSetNextWriteCallback;
	}

	/**
	 * Internal method. Do not call directly.
	 * 
	 * @param value
	 */
	@Deprecated
	@Override
	public void _onSetNextWrite(Consumer<Boolean> callback) {
		this.onSetNextWriteCallback = callback;
	}

}
