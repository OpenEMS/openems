package io.openems.common.utils;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.types.OpenemsType;

/**
 * Handles implicit conversions between {@link OpenemsType}s
 */
public class TypeUtils {

	@SuppressWarnings("unchecked")
	public static <T> T getAsType(OpenemsType type, Object value) {
		// Extract Optionals
		if (value instanceof Optional<?>) {
			value = ((Optional<?>) value).orElse(null);
		}
		switch (type) {
		case BOOLEAN:
			return (T) (Boolean) value;

		case SHORT:
			if (value == null) {
				return (T) (Short) value;

			} else if (value instanceof Boolean) {
				boolean boolValue = (Boolean) value;
				return (T) Short.valueOf((boolValue ? (short) 1 : (short) 0));

			} else if (value instanceof Short) {
				return (T) (Short) value;

			} else if (value instanceof Integer) {
				int intValue = (Integer) value;
				if (intValue >= Short.MIN_VALUE && intValue <= Short.MAX_VALUE) {
					return (T) Short.valueOf((short) intValue);
				} else {
					throw new IllegalArgumentException("Cannot convert Integer [" + value + "] to Short");
				}

			} else if (value instanceof Long) {
				long longValue = (Long) value;
				if (longValue >= Short.MIN_VALUE && longValue <= Short.MAX_VALUE) {
					return (T) Short.valueOf((short) longValue);
				} else {
					throw new IllegalArgumentException("Cannot convert Long [" + value + "] to Short");
				}

			} else if (value instanceof Float) {
				int intValue = ((Float) value).intValue();
				if (intValue >= Short.MIN_VALUE && intValue <= Short.MAX_VALUE) {
					return (T) Short.valueOf((short) intValue);
				} else {
					throw new IllegalArgumentException("Cannot convert Float [" + value + "] to Short");
				}
			}

		case INTEGER:
			if (value == null) {
				return (T) ((Integer) value);

			} else if (value instanceof Boolean) {
				boolean boolValue = (Boolean) value;
				return (T) Integer.valueOf((boolValue ? 1 : 0));

			} else if (value instanceof Short) {
				return (T) Integer.valueOf((Short) value);

			} else if (value instanceof Integer) {
				return (T) (Integer) value;

			} else if (value instanceof Long) {
				long longValue = (Long) value;
				if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
					return (T) Integer.valueOf((int) longValue);
				} else {
					throw new IllegalArgumentException("Cannot convert Long [" + value + "] to Integer");
				}

			} else if (value instanceof Float) {
				return (T) (Integer) ((Float) value).intValue();
			}

		case LONG:
			if (value == null) {
				return (T) (Long) value;

			} else if (value instanceof Boolean) {
				boolean boolValue = (Boolean) value;
				return (T) Long.valueOf((boolValue ? 1l : 0l));

			} else if (value instanceof Short) {
				return (T) (Long) ((Short) value).longValue();

			} else if (value instanceof Integer) {
				return (T) (Long) ((Integer) value).longValue();

			} else if (value instanceof Long) {
				return (T) (Long) value;

			} else if (value instanceof Float) {
				return (T) (Long) ((Float) value).longValue();
			}

		case FLOAT:
			if (value == null) {
				return (T) (Float) value;

			} else if (value instanceof Boolean) {
				boolean boolValue = (Boolean) value;
				return (T) Float.valueOf((boolValue ? 1f : 0f));

			} else if (value instanceof Short) {
				return (T) (Float) ((Short) value).floatValue();

			} else if (value instanceof Integer) {
				return (T) (Float) ((Integer) value).floatValue();

			} else if (value instanceof Long) {
				long longValue = (Long) value;
				if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
					return (T) (Float) Long.valueOf(longValue).floatValue();
				} else {
					throw new IllegalArgumentException("Cannot convert Long [" + value + "] to Integer");
				}

			} else if (value instanceof Float) {
				return (T) (Float) value;
			}
		}
		throw new IllegalArgumentException(
				"Converter for value [" + value + "] to type [" + type + "] is not implemented.");
	}

	public static JsonElement getAsJson(OpenemsType type, Object originalValue) {
		if (originalValue == null) {
			return JsonNull.INSTANCE;
		}
		Object value = TypeUtils.getAsType(type, originalValue);
		switch (type) {
		case BOOLEAN:
			return new JsonPrimitive((Boolean) value);
		case FLOAT:
			return new JsonPrimitive((Float) value);
		case INTEGER:
			return new JsonPrimitive((Integer) value);
		case LONG:
			return new JsonPrimitive((Long) value);
		case SHORT:
			return new JsonPrimitive((Short) value);
		}
		throw new IllegalArgumentException("Converter for value [" + value + "] to JSON is not implemented.");
	}
}
