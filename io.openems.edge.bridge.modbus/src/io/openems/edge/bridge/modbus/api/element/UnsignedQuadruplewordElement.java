package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;

/**
 * An UnsignedQuadruplewordElement represents a Long value in an
 * {@link AbstractQuadrupleWordElement}.
 */
public class UnsignedQuadruplewordElement extends AbstractQuadrupleWordElement<UnsignedQuadruplewordElement, Long> {

	public UnsignedQuadruplewordElement(int address) {
		super(OpenemsType.LONG, address);
	}

	@Override
	protected UnsignedQuadruplewordElement self() {
		return this;
	}

	@Override
	protected Long byteBufferToValue(ByteBuffer buff) {
		return buff.getLong(0);
	}

	@Override
	protected void valueToByteBuffer(ByteBuffer buff, Long value) {
		buff.putLong(value);
	}

}