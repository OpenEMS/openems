package io.openems.impl.protocol.modbus.internal;

import java.nio.ByteOrder;

import io.openems.api.channel.Channel;
import io.openems.api.exception.OpenemsModbusException;
import io.openems.impl.protocol.modbus.ModbusElement;

public class ElementBuilder {
	private Integer address = null;
	private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
	private Channel channel = null;
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
			return new SignedWordElement(address, channel, byteOrder);
		} else {
			return new UnsignedWordElement(address, channel, byteOrder);
		}
	}

	public ElementBuilder channel(Channel channel) {
		this.channel = channel;
		return this;
	}

	public ElementBuilder signed() {
		this.signed = true;
		return this;
	}
}
