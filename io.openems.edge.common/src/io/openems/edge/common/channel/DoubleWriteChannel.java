package io.openems.edge.common.channel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingConsumer;
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
	public List<ThrowingConsumer<Double, OpenemsNamedException>> getOnSetNextWrites() {
		return super.getOnSetNextWrites();
	}

	@Override
	public void onSetNextWrite(ThrowingConsumer<Double, OpenemsNamedException> callback) {
		this.getOnSetNextWrites().add(callback);
	}

	/**
	 * An object that holds information about the write target of this Channel, i.e.
	 * a Modbus Register or REST-Api endpoint address. Defaults to null.
	 */
	private Object writeTarget = null;

	@Override
	public <WRITE_TARGET> void setWriteTarget(WRITE_TARGET writeTarget) throws IllegalArgumentException {
		if (this.writeTarget != null && writeTarget != null && !Objects.equals(this.writeTarget, writeTarget)) {
			throw new IllegalArgumentException("Unable to set write target [" + writeTarget.toString()
					+ "]. Channel already has a write target [" + this.writeTarget.toString() + "]");
		}
		this.writeTarget = writeTarget;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <WRITE_TARGET> WRITE_TARGET getWriteTarget() {
		return (WRITE_TARGET) this.writeTarget;
	}
}
