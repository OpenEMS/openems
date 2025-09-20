package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;

/**
 * A SignedDoublewordElement represents a Long value in an
 * {@link AbstractDoubleWordElement}.
 */
public class SignedDoublewordElement extends AbstractDoubleWordElement<SignedDoublewordElement, Long> {

	public SignedDoublewordElement(int address) {
		super(OpenemsType.LONG, address);
	}

	@Override
	protected SignedDoublewordElement self() {
		return this;
	}

	@Override
	protected Long byteBufferToValue(ByteBuffer buff) {
		return Long.valueOf(buff.getInt());
	}

	@Override
	protected void valueToByteBuffer(ByteBuffer buff, Long value) {
		buff.putInt(value.intValue());
	}

}
