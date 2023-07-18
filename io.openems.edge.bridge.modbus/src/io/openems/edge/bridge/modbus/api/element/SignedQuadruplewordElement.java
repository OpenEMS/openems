package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;

/**
 * A SignedQuadruplewordElement represents a Long value in an
 * {@link AbstractQuadrupleWordElement}.
 */
public class SignedQuadruplewordElement extends AbstractQuadrupleWordElement<SignedQuadruplewordElement, Long> {

	public SignedQuadruplewordElement(int address) {
		super(OpenemsType.LONG, address);
	}

	@Override
	protected SignedQuadruplewordElement self() {
		return this;
	}

	@Override
	protected Long convert(ByteBuffer buff) {
		return Long.valueOf(buff.getLong());
	}

	@Override
	protected ByteBuffer toByteBuffer(ByteBuffer buff, Long value) {
		return buff.putLong(value.longValue());
	}

}
