package io.openems.edge.common.channel;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
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
	private final List<Consumer<Boolean>> onSetNextWriteCallbacks = new CopyOnWriteArrayList<>();

	@Override
	public List<Consumer<Boolean>> getOnSetNextWrites() {
		return this.onSetNextWriteCallbacks;
	}

	@Override
	public void onSetNextWrite(Consumer<Boolean> callback) {
		this.onSetNextWriteCallbacks.add(callback);
	}

}
