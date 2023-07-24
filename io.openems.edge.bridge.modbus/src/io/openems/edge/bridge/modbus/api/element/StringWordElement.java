package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;

/**
 * An StringWordElement represents a String value. Each Register (= 2 bytes)
 * represents two characters.
 */
public class StringWordElement extends AbstractMultipleWordsElement<StringWordElement, String> {

	public StringWordElement(int startAddress, int length) {
		super(OpenemsType.STRING, startAddress, length);
	}

	@Override
	protected String byteBufferToValue(ByteBuffer buff) {
		for (var b : buff.array()) {
			if (b == 0) {
				b = 32;// replace '0' with ASCII space
			}
		}
		return new String(buff.array()).trim();
	}

	@Override
	protected StringWordElement self() {
		return this;
	}

	@Override
	protected void valueToByteBuffer(ByteBuffer buff, String value) {
		ByteBuffer.wrap(value.getBytes());
	}

}