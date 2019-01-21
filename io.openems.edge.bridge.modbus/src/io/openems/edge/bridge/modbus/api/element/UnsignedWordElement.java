package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;

public class UnsignedWordElement extends AbstractWordElement<UnsignedWordElement, Integer> {

	public UnsignedWordElement(int address) {
		super(OpenemsType.INTEGER, address);
	}

	@Override
	protected UnsignedWordElement self() {
		return this;
	}

	protected Integer fromByteBuffer(ByteBuffer buff) {
		return Short.toUnsignedInt(buff.getShort(0));
	}

	protected ByteBuffer toByteBuffer(ByteBuffer buff, Integer value) {
		return buff.putShort(value.shortValue());
	}

}
