package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;

/**
 * An UnsignedDoublewordElement represents a Long value in an
 * {@link AbstractDoubleWordElement}.
 */
public class UnsignedDoublewordElement extends AbstractDoubleWordElement<UnsignedDoublewordElement, Long> {

	public UnsignedDoublewordElement(int address) {
		super(OpenemsType.LONG, address);
	}

	@Override
	protected UnsignedDoublewordElement self() {
		return this;
	}

	@Override
	protected Long convert(ByteBuffer buff) {
		return Integer.toUnsignedLong(buff.getInt(0));
	}

	@Override
	protected ByteBuffer toByteBuffer(ByteBuffer buff, Long value) {
		return buff.putInt(value.intValue());
	}
}
