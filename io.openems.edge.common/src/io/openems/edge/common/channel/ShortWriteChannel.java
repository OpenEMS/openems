package io.openems.edge.common.channel;

import java.util.List;
import java.util.Optional;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.edge.common.component.OpenemsComponent;

public class ShortWriteChannel extends ShortReadChannel implements WriteChannel<Short> {

	protected ShortWriteChannel(OpenemsComponent component, ChannelId channelId, ShortDoc channelDoc) {
		super(component, channelId, channelDoc);
	}

	private Optional<Short> nextWriteValueOpt = Optional.empty();

	/**
	 * Internal method. Do not call directly.
	 *
	 * @param value the value as {@link Short}
	 */
	@Deprecated
	@Override
	public void _setNextWriteValue(Short value) {
		this.nextWriteValueOpt = Optional.ofNullable(value);
	}

	@Override
	public Optional<Short> getNextWriteValue() {
		return this.nextWriteValueOpt;
	}

	/*
	 * onSetNextWrite
	 */
	@Override
	public List<ThrowingConsumer<Short, OpenemsNamedException>> getOnSetNextWrites() {
		return super.getOnSetNextWrites();
	}

	@Override
	public void onSetNextWrite(ThrowingConsumer<Short, OpenemsNamedException> callback) {
		this.getOnSetNextWrites().add(callback);
	}
}
