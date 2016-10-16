package io.openems.impl.protocol.modbus.internal;

import java.math.BigInteger;

import io.openems.api.channel.Channel;

public abstract class Element {
	protected final int address;
	protected final Channel channel;
	protected final BigInteger delta;
	protected final BigInteger multiplier;
	protected ModbusRange range = null;

	public Element(int address, Channel channel, int multiplier, int delta) {
		this.address = address;
		this.channel = channel;
		this.delta = BigInteger.valueOf(delta);
		this.multiplier = BigInteger.valueOf(multiplier);
	}

	public int getAddress() {
		return address;
	}

	public Channel getChannel() {
		return channel;
	}

	public abstract int getLength();

	public abstract BigInteger getMaxValue();

	public abstract BigInteger getMinValue();

	public ModbusRange getModbusRange() {
		return range;
	}

	/**
	 * Set the {@link ModbusRange}, where this Element belongs to. This is called during {@link ModbusRange}.add()
	 *
	 * @param range
	 */
	public void setModbusRange(ModbusRange range) {
		this.range = range;
	}

	public void setValue(BigInteger value) {
		channel.setValue(value);
	}
}
