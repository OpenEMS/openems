package io.openems.impl.protocol.studer.internal.object;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public enum PropertyId {
	VALUE("Value", (short) 0x0001), //
	VALUE_QSP("Value", (short) 0x0005), //
	MIN_QSP("Min", (short) 0x0006), //
	MAX_QSP("Max", (short) 0x0007), //
	LEVEL_QSP("Level", (short) 0x0008), //
	UNSAVED_VALUE_QSP("UnsavedValue", (short) 0x000D);

	private final String name;
	private final short code;

	public byte[] getByte() {
		return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(code).array();
	}

	private PropertyId(String name, short code) {
		this.code = code;
		this.name = name;
	}

	public static PropertyId getByCode(short code) {
		for (PropertyId p : PropertyId.values()) {
			if (p.code == code) {
				return p;
			}
		}
		return null;
	}

	public String getName() {
		return name;
	}
}
