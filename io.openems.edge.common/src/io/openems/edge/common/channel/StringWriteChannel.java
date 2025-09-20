package io.openems.edge.common.channel;

import java.util.List;
import java.util.Optional;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.edge.common.component.OpenemsComponent;

public class StringWriteChannel extends StringReadChannel implements WriteChannel<String> {

	public StringWriteChannel(OpenemsComponent component, ChannelId channelId, StringDoc channelDoc) {
		super(component, channelId, channelDoc);
	}

	private Optional<String> nextWriteValueOpt = Optional.empty();

	/**
	 * Internal method. Do not call directly.
	 *
	 * @param value the value as {@link String}
	 */
	@Deprecated
	@Override
	public void _setNextWriteValue(String value) {
		this.nextWriteValueOpt = Optional.ofNullable(value);
	}

	@Override
	public Optional<String> getNextWriteValue() {
		return this.nextWriteValueOpt;
	}

	/*
	 * onSetNextWrite
	 */
	@Override
	public List<ThrowingConsumer<String, OpenemsNamedException>> getOnSetNextWrites() {
		return super.getOnSetNextWrites();
	}

	@Override
	public void onSetNextWrite(ThrowingConsumer<String, OpenemsNamedException> callback) {
		this.getOnSetNextWrites().add(callback);
	}
}
