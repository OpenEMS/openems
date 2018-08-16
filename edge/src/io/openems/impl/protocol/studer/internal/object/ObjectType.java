package io.openems.impl.protocol.studer.internal.object;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public enum ObjectType {
	USERINFO((short) 0x0001), //
	PARAMETER((short) 0x0002), //
	MESSAGE((short) 0x0003), //
	CUSTOMDATALOG((short) 0x0005), //
	FILETRANSFER((short) 0x0101);

	private short code;

	public byte[] getByte() {
		return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(code).array();
	}

	private ObjectType(short b) {
		this.code = b;
	}

	public static ObjectType getByCode(short code) {
		for (ObjectType o : ObjectType.values()) {
			if (o.code == code) {
				return o;
			}
		}
		return null;
	}
}
