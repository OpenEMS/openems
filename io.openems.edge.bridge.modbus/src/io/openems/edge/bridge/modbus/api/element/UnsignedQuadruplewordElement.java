package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;

public class UnsignedQuadruplewordElement extends AbstractQuadrupleWordElement<UnsignedQuadruplewordElement, Long> {

	public UnsignedQuadruplewordElement(int address) {
		super(OpenemsType.LONG, address);
	}

	@Override
	protected UnsignedQuadruplewordElement self() {
		return this;
	}

	protected Long fromByteBuffer(ByteBuffer buff) {

		return buff.getLong(0);
	}

	protected ByteBuffer toByteBuffer(ByteBuffer buff, Long value) {

		return buff.putLong(value);
	}

}
