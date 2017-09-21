package io.openems.core.utilities;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.openems.api.exception.NotImplementedException;

public class BitUtils {

	private final static int BITS = 8;
	private final static int BYTES_INT = 4;
	private final static int BYTES_LONG = 8;
	private final static int BITS_BOOLEAN = 1;

	public final static ByteOrder BYTE_ODER = ByteOrder.BIG_ENDIAN;

	public static int getBitLength(Class<?> type) throws NotImplementedException {
		switch (OpenemsTypes.get(type)) {
		case INTEGER:
			return BYTES_INT * BITS;

		case LONG:
			return BYTES_LONG * BITS;

		case BOOLEAN:
			return BITS_BOOLEAN;

		case DOUBLE: // TODO
		case INET_4_ADDRESS: // TODO
		case STRING:
		case LONG_ARRAY:
		case JSON_ARRAY:
		case JSON_OBJECT:
		case DEVICE_NATURE:
		case THING_MAP:
			// igore - no error
			return 0;
		}
		throw new NotImplementedException("BitLength for type [" + type + "] is not implemented.");
	}

	public static byte[] toBytes(Object value) throws NotImplementedException {
		Class<?> type = value.getClass();
		switch (OpenemsTypes.get(type)) {
		case INTEGER:
			return ByteBuffer.allocate(BYTES_INT).order(BYTE_ODER).putInt((Integer) value).array();

		case LONG:
			return ByteBuffer.allocate(BYTES_LONG).order(BYTE_ODER).putLong((Long) value).array();

		case BOOLEAN: // TODO put boolean value in a byte
		case DOUBLE: // TODO
		case INET_4_ADDRESS: // TODO
		case STRING:
		case LONG_ARRAY:
		case JSON_ARRAY:
		case JSON_OBJECT:
		case DEVICE_NATURE:
		case THING_MAP:
			// igore - no error
			return new byte[0];
		}
		throw new NotImplementedException(
				"Converter to Byte for value [" + value + "] of type [" + type + "] is not implemented.");
	}
}
