package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;

public class UnsignedDoublewordElement extends AbstractDoubleWordElement<UnsignedDoublewordElement, Long> {

	public UnsignedDoublewordElement(int address) {
		super(OpenemsType.LONG, address);
	}

	@Override
	protected UnsignedDoublewordElement self() {
		return this;
	}

	protected Long fromByteBuffer(ByteBuffer buff) {
		return Integer.toUnsignedLong(buff.getInt(0));
	}

	protected ByteBuffer toByteBuffer(ByteBuffer buff, Long value) {
		return buff.putInt(value.intValue());
	}
}
