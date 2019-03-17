package io.openems.edge.common.channel;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import io.openems.edge.common.component.OpenemsComponent;

public class BooleanWriteChannel extends BooleanReadChannel implements WriteChannel<Boolean> {

	private Optional<Boolean> nextWriteValueOpt = Optional.empty();

	protected BooleanWriteChannel(OpenemsComponent component, ChannelId channelId, BooleanDoc channelDoc) {
		super(component, channelId, channelDoc);
	}

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

	@Override
	public Optional<Boolean> getNextWriteValue() {
		return this.nextWriteValueOpt;
	}

	/*
	 * onSetNextWrite
	 *
	 * @return
	 */
	@Override
	public List<Consumer<Boolean>> getOnSetNextWrites() {
		return super.getOnSetNextWrites();
	}

	@Override
	public void onSetNextWrite(Consumer<Boolean> callback) {
		this.getOnSetNextWrites().add(callback);
	}

}
