package io.openems.impl.protocol.modbus.internal;

import java.nio.ByteOrder;

import io.openems.api.exception.OpenemsModbusException;
import io.openems.impl.protocol.modbus.ModbusChannel;
import io.openems.impl.protocol.modbus.ModbusElement;

public class ElementBuilder {
	private Integer address = null;
	private ModbusChannel channel = null;
	private int delta = 0;
	private int multiplier = 1;
	ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

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
		;
		return new UnsignedWordElement(address, channel, multiplier, delta, byteOrder);
	}

	public ElementBuilder channel(ModbusChannel channel) {
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
}
