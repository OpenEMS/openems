package io.openems.edge.bridge.modbus.api.element;

import io.openems.common.types.OpenemsType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Represents Float value according to IEE754.
 */
public class FloatDoublewordElement extends AbstractDoubleWordElement<Float> {

	public FloatDoublewordElement(int address) {
		super(OpenemsType.FLOAT, address);
	}

	public FloatDoublewordElement wordOrder(WordOrder wordOrder) {
		this.wordOrder = wordOrder;
		return this;
	}

	public FloatDoublewordElement byteOrder(ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
		return this;
	}

	protected Float fromByteBuffer(ByteBuffer buff) {
		return buff.order(this.byteOrder).getFloat(0);
	}

	protected ByteBuffer toByteBuffer(ByteBuffer buff, Float value) {
		return buff.putFloat(value.floatValue());
	}
}
