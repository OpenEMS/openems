package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;

/**
 * A FloatQuadruplewordElement represents a Float value in an
 * {@link AbstractQuadrupleWordElement}.
 */
public class FloatQuadruplewordElement extends AbstractQuadrupleWordElement<FloatQuadruplewordElement, Double> {

	public FloatQuadruplewordElement(int address) {
		super(OpenemsType.DOUBLE, address);
	}

	@Override
	protected FloatQuadruplewordElement self() {
		return this;
	}

	@Override
	protected Double byteBufferToValue(ByteBuffer buff) {
		return buff.getDouble();
	}

	@Override
	protected void valueToByteBuffer(ByteBuffer buff, Double value) {
		buff.putDouble(value.doubleValue());
	}
}
