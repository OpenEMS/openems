package io.openems.core.utilities;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.openems.api.exception.NotImplementedException;

public class BitUtils {

	private final static int BYTES_INT = 4;
	private final static int BYTES_LONG = 8;

	public final static ByteOrder BYTE_ODER = ByteOrder.BIG_ENDIAN;

	public static int getBitLength(Class<?> type) throws NotImplementedException {
		if (Integer.class.isAssignableFrom(type)) {
			return BYTES_INT;

		} else if (Long.class.isAssignableFrom(type)) {
			return BYTES_LONG * 8;
		}
		throw new NotImplementedException("BitLength for type [" + type + "] is not implemented.");
	}

	public static byte[] toBytes(Object value) throws NotImplementedException {
		Class<?> type = value.getClass();
		if (Integer.class.isAssignableFrom(type)) {
			return ByteBuffer.allocate(BYTES_INT).order(BYTE_ODER).putInt((Integer) value).array();

		} else if (Long.class.isAssignableFrom(type)) {
			return ByteBuffer.allocate(BYTES_LONG).order(BYTE_ODER).putLong((Long) value).array();

		}
		throw new NotImplementedException(
				"Converter to Byte for value [" + value + "] of type [" + type + "] is not implemented.");
	}
}
