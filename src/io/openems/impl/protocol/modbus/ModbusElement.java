package io.openems.impl.protocol.modbus;

import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.impl.protocol.modbus.internal.ModbusRange;

public abstract class ModbusElement {
	protected final int address;
	protected final Channel channel;
	protected final BigInteger delta;
	protected final Logger log;
	protected final BigInteger multiplier;
	protected ModbusRange range = null;

	public ModbusElement(int address, Channel channel, int multiplier, int delta) {
		this.address = address;
		this.channel = channel;
		this.delta = BigInteger.valueOf(delta);
		this.multiplier = BigInteger.valueOf(multiplier);
		log = LoggerFactory.getLogger(this.getClass());
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
		if (channel instanceof ModbusChannel) {
			((ModbusChannel) channel).updateValue(value);
		} else if (channel instanceof WriteableModbusChannel) {
			((WriteableModbusChannel) channel).updateValue(value);
		} else {
			log.error("Unable to set value. Channel is no ModbusChannel or WritableModbusChannel.");
		}
	}
}
