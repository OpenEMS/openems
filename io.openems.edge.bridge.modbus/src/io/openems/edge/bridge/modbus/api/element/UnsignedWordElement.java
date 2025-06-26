package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

/**
 * An UnsignedWordElement represents an Integer value in an
 * {@link AbstractSingleWordElement}.
 */
public class UnsignedWordElement extends AbstractSingleWordElement<UnsignedWordElement, Integer> {

	public UnsignedWordElement(int address) {
		super(OpenemsType.INTEGER, address);
	}

	@Override
	protected UnsignedWordElement self() {
		return this;
	}

	@Override
	protected Integer byteBufferToValue(ByteBuffer buff) {
		return Short.toUnsignedInt(buff.getShort(0));
	}

	@Override
	protected void valueToByteBuffer(ByteBuffer buff, Integer value) {
		Integer i = TypeUtils.getAsType(OpenemsType.INTEGER, value);
		buff.putShort(i.shortValue());
	}

}