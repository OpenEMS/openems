package io.openems.impl.protocol.modbus.internal;

import java.nio.ByteOrder;

import org.eclipse.jdt.annotation.NonNull;

import io.openems.api.channel.Channel;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusElement;

public class ElementBuilder {
	private Integer address = null;
	private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
	private Channel channel = null;
	private int dummy = 0;
	private boolean signed = false;

	public ElementBuilder address(Integer address) {
		this.address = address;
		return this;
	}

	@SuppressWarnings("null")
	public ModbusElement build() throws ConfigException {
		if (address == null) {
			throw new ConfigException("Error in protocol: [address] is missing");
		} else if (dummy > 0) {
			return new DummyElement(address, dummy);
		} else if (channel == null) {
			throw new ConfigException("Error in protocol: [channel] is missing");
		}
		if (signed) {
			return new SignedWordElement(address, channel, byteOrder);
		} else {
			return new UnsignedWordElement(address, channel, byteOrder);
		}
	}

	public ElementBuilder byteOrder(@NonNull ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
		return this;
	}

	public ElementBuilder channel(@NonNull Channel channel) {
		this.channel = channel;
		return this;
	}

	public ElementBuilder dummy() {
		this.dummy = 1;
		return this;
	}

	public ElementBuilder dummy(int length) {
		this.dummy = length;
		return this;
	}

	public ElementBuilder signed() {
		this.signed = true;
		return this;
	}
}
