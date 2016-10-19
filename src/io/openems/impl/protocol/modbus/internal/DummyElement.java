package io.openems.impl.protocol.modbus.internal;

import io.openems.api.channel.Channel;
import io.openems.impl.protocol.modbus.ModbusElement;

public class DummyElement extends ModbusElement {

	private final int length;

	public DummyElement(int address, int length) {
		super(address, new Channel("", null, null, null, null));
		this.length = length;
	}

	@Override
	public int getLength() {
		return length;
	}

}
