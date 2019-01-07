package io.openems.edge.bridge.modbus.api.element;

import io.openems.common.types.OpenemsType;

import java.nio.ByteBuffer;

/**
 * Represents Float value according to IEE754.
 */
public class FloatDoublewordElement extends AbstractDoubleWordElement<FloatDoublewordElement, Float> {

	public FloatDoublewordElement(int address) {
		super(OpenemsType.FLOAT, address);
	}

	@Override
	protected FloatDoublewordElement self() {
		return this;
	}

	protected Float fromByteBuffer(ByteBuffer buff) {
		return buff.order(this.getByteOrder()).getFloat(0);
	}

	protected ByteBuffer toByteBuffer(ByteBuffer buff, Float value) {
		return buff.putFloat(value.floatValue());
	}
}
