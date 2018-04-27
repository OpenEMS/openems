package io.openems.common.utils;

import java.util.Optional;

import io.openems.common.types.OpenemsType;

public class TypeUtils {

	@SuppressWarnings("unchecked")
	public static <T> T getAsType(OpenemsType type, Object value) {
		// Extract Optionals
		if (value instanceof Optional<?>) {
			value = ((Optional<?>) value).orElse(null);
		}
		switch (type) {
		case LONG:
			return (T) ((Long) value);
		case INTEGER:
			return (T) ((Integer) value);
		case BOOLEAN:
			return (T) ((Boolean) value);
		}
		throw new IllegalArgumentException(
				"Converter for value [" + value + "] to type [" + type + "] is not implemented.");
	}
}
