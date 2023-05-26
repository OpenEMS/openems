package io.openems.edge.common.channel;

import java.util.List;
import java.util.Optional;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.edge.common.component.OpenemsComponent;

public class BooleanWriteChannel extends BooleanReadChannel implements WriteChannel<Boolean> {

	private Optional<Boolean> nextWriteValueOpt = Optional.empty();

	protected BooleanWriteChannel(OpenemsComponent component, ChannelId channelId, BooleanDoc channelDoc) {
		super(component, channelId, channelDoc);
	}

	/**
	 * Internal method. Do not call directly.
	 *
	 * @param value the value
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
	public List<ThrowingConsumer<Boolean, OpenemsNamedException>> getOnSetNextWrites() {
		return super.getOnSetNextWrites();
	}

	@Override
	public void onSetNextWrite(ThrowingConsumer<Boolean, OpenemsNamedException> callback) {
		this.getOnSetNextWrites().add(callback);
	}
}
