package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

/**
 * An SignedWordElement represents a Short value in an
 * {@link AbstractSingleWordElement}.
 */
public class SignedWordElement extends AbstractSingleWordElement<SignedWordElement, Short> {

	public SignedWordElement(int address) {
		super(OpenemsType.SHORT, address);
	}

	@Override
	protected SignedWordElement self() {
		return this;
	}

	@Override
	protected Short convert(ByteBuffer buff) {
		return buff.order(this.getByteOrder()).getShort(0);
	}

	@Override
	protected ByteBuffer toByteBuffer(ByteBuffer buff, Object object) {
		Short value = TypeUtils.getAsType(OpenemsType.SHORT, object);
		return buff.putShort(value.shortValue());
	}

}
