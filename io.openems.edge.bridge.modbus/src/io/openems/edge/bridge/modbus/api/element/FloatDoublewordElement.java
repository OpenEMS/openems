package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;

/**
 * A FloatDoublewordElement represents a Float value according to IEEE-754 in an
 * {@link AbstractDoubleWordElement}.
 */
public class FloatDoublewordElement extends AbstractDoubleWordElement<FloatDoublewordElement, Float> {

	public FloatDoublewordElement(int address) {
		super(OpenemsType.FLOAT, address);
	}

	@Override
	protected FloatDoublewordElement self() {
		return this;
	}

	@Override
	protected Float byteBufferToValue(ByteBuffer buff) {
		return buff.getFloat(0);
	}

	@Override
	protected void valueToByteBuffer(ByteBuffer buff, Float value) {
		buff.putFloat(value.floatValue());
	}
}
