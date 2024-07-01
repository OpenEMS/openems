package io.openems.edge.bridge.modbus.api.element;

import static java.nio.ByteOrder.BIG_ENDIAN;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

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
		var src = buff.array();
		var out = new byte[src.length];
		for (int i = 0; i < src.length; i += 2) {
			if (this.getByteOrder() == BIG_ENDIAN) {
				out[i] = src[i];
				out[i + 1] = src[i + 1];

			} else { // LITTLE_ENDIAN
				out[i] = src[i + 1];
				out[i + 1] = src[i];
			}
		}
		return new String(tidyUp(out)).trim();
	}

	@Override
	protected StringWordElement self() {
		return this;
	}

	@Override
	protected void valueToByteBuffer(ByteBuffer buff, String value) {
		buff.put(value.getBytes(StandardCharsets.US_ASCII));
	}

	private static byte[] tidyUp(byte[] array) {
		for (var i = 0; i < array.length; i++) {
			array[i] = tidyUp(array[i]);
		}
		return array;
	}

	private static byte tidyUp(byte b) {
		if (b == 0) {
			b = 32;// replace '0' with ASCII space
		}
		return b;
	}

}