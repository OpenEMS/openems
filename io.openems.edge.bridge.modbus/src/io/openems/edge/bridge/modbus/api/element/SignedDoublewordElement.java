package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;

public class SignedDoublewordElement extends AbstractDoubleWordElement<Long> {

	public SignedDoublewordElement(int address) {
		super(OpenemsType.LONG, address);
	}

	public SignedDoublewordElement wordOrder(WordOrder wordOrder) {
		this.wordOrder = wordOrder;
		return this;
	}

	protected Long fromByteBuffer(ByteBuffer buff) {
		return Long.valueOf(buff.getInt());
	}

	protected ByteBuffer toByteBuffer(ByteBuffer buff, Long value) {
		return buff.putInt(value.intValue());
	}

}
