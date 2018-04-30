package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.openems.common.types.OpenemsType;

public class UnsignedWordElement extends AbstractWordElement<Integer> {

	public UnsignedWordElement(int address) {
		super(OpenemsType.INTEGER, address);
	}

	public UnsignedWordElement byteOrder(ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
		return this;
	}

	protected Integer fromByteBuffer(ByteBuffer buff) {
		return Short.toUnsignedInt(buff.getShort(0));
	}

	protected ByteBuffer toByteBuffer(ByteBuffer buff, Integer value) {
		return buff.putShort(value.shortValue());
	}

}
