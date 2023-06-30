package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

/**
 * An UnsignedWordElement represents an Integer value in an
 * {@link AbstractWordElement}.
 */
public class UnsignedWordElement extends AbstractWordElement<UnsignedWordElement, Integer> {

	public UnsignedWordElement(int address) {
		super(OpenemsType.INTEGER, address);
	}

	@Override
	protected UnsignedWordElement self() {
		return this;
	}

	@Override
	protected Integer fromByteBuffer(ByteBuffer buff) {
		return Short.toUnsignedInt(buff.getShort(0));
	}

	@Override
	protected ByteBuffer toByteBuffer(ByteBuffer buff, Object object) {
		Integer value = TypeUtils.getAsType(OpenemsType.INTEGER, object);
		return buff.putShort(value.shortValue());
	}

}
