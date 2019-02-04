package io.openems.edge.bridge.modbus.api.element;

import io.openems.common.types.OpenemsType;

import java.nio.ByteBuffer;

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

	protected Long fromByteBuffer(ByteBuffer buff) {
		return Long.valueOf(buff.getInt());
	}

	protected ByteBuffer toByteBuffer(ByteBuffer buff, Long value) {
		return buff.putInt(value.intValue());
	}
	
}
