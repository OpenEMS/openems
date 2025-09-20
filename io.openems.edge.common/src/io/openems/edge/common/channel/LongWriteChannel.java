package io.openems.edge.common.channel;

import java.util.List;
import java.util.Optional;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.edge.common.component.OpenemsComponent;

public class LongWriteChannel extends LongReadChannel implements WriteChannel<Long> {

	protected LongWriteChannel(OpenemsComponent component, ChannelId channelId, LongDoc channelDoc) {
		super(component, channelId, channelDoc);
	}

	private Optional<Long> nextWriteValueOpt = Optional.empty();

	/**
	 * Internal method. Do not call directly.
	 *
	 * @param value the value as {@link Long}
	 */
	@Deprecated
	@Override
	public void _setNextWriteValue(Long value) {
		this.nextWriteValueOpt = Optional.ofNullable(value);
	}

	@Override
	public Optional<Long> getNextWriteValue() {
		return this.nextWriteValueOpt;
	}

	/*
	 * onSetNextWrite
	 */
	@Override
	public List<ThrowingConsumer<Long, OpenemsNamedException>> getOnSetNextWrites() {
		return super.getOnSetNextWrites();
	}

	@Override
	public void onSetNextWrite(ThrowingConsumer<Long, OpenemsNamedException> callback) {
		this.getOnSetNextWrites().add(callback);
	}
}
