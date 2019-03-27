package io.openems.edge.common.channel;

import java.util.List;
import java.util.Optional;

import io.openems.common.exceptions.CheckedConsumer;
import io.openems.edge.common.component.OpenemsComponent;

public class DoubleWriteChannel extends DoubleReadChannel implements WriteChannel<Double> {

	protected DoubleWriteChannel(OpenemsComponent component, ChannelId channelId, DoubleDoc channelDoc) {
		super(component, channelId, channelDoc);
	}

	private Optional<Double> nextWriteValueOpt = Optional.empty();

	/**
	 * Internal method. Do not call directly.
	 * 
	 * @param value
	 */
	@Deprecated
	@Override
	public void _setNextWriteValue(Double value) {
		this.nextWriteValueOpt = Optional.ofNullable(value);
	}

	@Override
	public Optional<Double> getNextWriteValue() {
		return this.nextWriteValueOpt;
	}

	/*
	 * onSetNextWrite
	 */
	@Override
	public List<CheckedConsumer<Double>> getOnSetNextWrites() {
		return super.getOnSetNextWrites();
	}

	@Override
	public void onSetNextWrite(CheckedConsumer<Double> callback) {
		this.getOnSetNextWrites().add(callback);
	}

}
