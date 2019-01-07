package io.openems.edge.bridge.modbus.api.element;

import io.openems.common.types.OpenemsType;

import java.nio.ByteBuffer;

public class SignedDoublewordElement extends AbstractDoubleWordElement<SignedDoublewordElement, Long> {

	public SignedDoublewordElement(int address) {
		super(OpenemsType.LONG, address);
	}

	@Override
	protected SignedDoublewordElement self() {
		return this;
	}

	protected Long fromByteBuffer(ByteBuffer buff) {
		return Long.valueOf(buff.getInt());
	}

	protected ByteBuffer toByteBuffer(ByteBuffer buff, Long value) {
		return buff.putInt(value.intValue());
	}

}
