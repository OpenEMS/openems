package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;

public class SignedWordElement extends AbstractWordElement<Short> {

	public SignedWordElement(int address) {
		super(OpenemsType.SHORT, address);
	}

	protected Short fromByteBuffer(ByteBuffer buff) {
		return buff.order(getByteOrder()).getShort(0);
	}

	protected ByteBuffer toByteBuffer(ByteBuffer buff, Short value) {
		return buff.putShort(value.shortValue());
	}

}
