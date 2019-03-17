package io.openems.edge.common.channel;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import io.openems.edge.common.channel.internal.FloatDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class FloatWriteChannel extends FloatReadChannel implements WriteChannel<Float> {

	protected FloatWriteChannel(OpenemsComponent component, ChannelId channelId, FloatDoc channelDoc) {
		super(component, channelId, channelDoc);
	}

	private Optional<Float> nextWriteValueOpt = Optional.empty();

	/**
	 * Internal method. Do not call directly.
	 * 
	 * @param value
	 */
	@Deprecated
	@Override
	public void _setNextWriteValue(Float value) {
		this.nextWriteValueOpt = Optional.ofNullable(value);
	}

	@Override
	public Optional<Float> getNextWriteValue() {
		return this.nextWriteValueOpt;
	}

	/*
	 * onSetNextWrite
	 */
	@Override
	public List<Consumer<Float>> getOnSetNextWrites() {
		return super.getOnSetNextWrites();
	}

	@Override
	public void onSetNextWrite(Consumer<Float> callback) {
		this.getOnSetNextWrites().add(callback);
	}

}
