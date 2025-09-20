package io.openems.edge.common.channel;

import java.util.List;
import java.util.Optional;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.edge.common.component.OpenemsComponent;

public class IntegerWriteChannel extends IntegerReadChannel implements WriteChannel<Integer> {

	protected IntegerWriteChannel(OpenemsComponent component, ChannelId channelId, IntegerDoc channelDoc) {
		super(component, channelId, channelDoc);
	}

	private Optional<Integer> nextWriteValueOpt = Optional.empty();

	/**
	 * Internal method. Do not call directly.
	 *
	 * @param value the value as Integer
	 */
	@Deprecated
	@Override
	public void _setNextWriteValue(Integer value) {
		this.nextWriteValueOpt = Optional.ofNullable(value);
	}

	@Override
	public Optional<Integer> getNextWriteValue() {
		return this.nextWriteValueOpt;
	}

	/*
	 * onSetNextWrite
	 */
	@Override
	public List<ThrowingConsumer<Integer, OpenemsNamedException>> getOnSetNextWrites() {
		return super.getOnSetNextWrites();
	}

	@Override
	public void onSetNextWrite(ThrowingConsumer<Integer, OpenemsNamedException> callback) {
		this.getOnSetNextWrites().add(callback);
	}
}
