package io.openems.impl.protocol.modbus.internal;

import java.nio.ByteOrder;

import io.openems.api.channel.Channel;
import io.openems.api.exception.OpenemsModbusException;
import io.openems.impl.protocol.modbus.ModbusElement;

public class ElementBuilder {
	private Integer address = null;
	private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
	private Channel channel = null;
	private int delta = 0;
	private int multiplier = 1;
	private boolean signed = false;

	public ElementBuilder address(Integer address) {
		this.address = address;
		return this;
	}

	public ModbusElement build() throws OpenemsModbusException {
		if (address == null) {
			throw new OpenemsModbusException("Error in protocol: [address] is missing");
		} else if (channel == null) {
			throw new OpenemsModbusException("Error in protocol: [channel] is missing");
		}
		if (signed) {
			return new SignedWordElement(address, channel, multiplier, delta, byteOrder);
		} else {
			return new UnsignedWordElement(address, channel, multiplier, delta, byteOrder);
		}
	}

	public ElementBuilder channel(Channel channel) {
		this.channel = channel;
		return this;
	}

	public ElementBuilder delta(int delta) {
		this.delta = delta;
		return this;
	}

	public ElementBuilder multiplier(int multiplier) {
		this.multiplier = multiplier;
		return this;
	}

	public ElementBuilder signed() {
		this.signed = true;
		return this;
	}
}
