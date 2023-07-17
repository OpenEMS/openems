package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;
import java.util.Optional;

import io.openems.common.exceptions.OpenemsException;
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
	protected Double fromByteBuffer(ByteBuffer buff) {
		return Double.valueOf(buff.getDouble());
	}

	@Override
	protected ByteBuffer toByteBuffer(ByteBuffer buff, Double value) {
		return buff.putDouble(value.doubleValue());
	}
}
