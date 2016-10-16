package io.openems.impl.protocol.modbus;

import java.math.BigInteger;

import io.openems.api.channel.Channel;
import io.openems.impl.protocol.modbus.internal.ModbusRange;

public abstract class ModbusElement {
	protected final int address;
	protected final ModbusChannel channel;
	protected final BigInteger delta;
	protected final BigInteger multiplier;
	protected ModbusRange range = null;

	public ModbusElement(int address, ModbusChannel channel, int multiplier, int delta) {
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

	protected void setValue(BigInteger value) {
		channel.updateValue(value);
	}
}
