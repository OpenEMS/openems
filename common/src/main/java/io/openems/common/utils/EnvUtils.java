package io.openems.common.utils;

import java.util.Optional;

import io.openems.common.exceptions.OpenemsException;

public class EnvUtils {
	public static boolean isSet(String name) {
		return System.getenv(name) != null;
	};
	
	public static String getAsString(String name) throws OpenemsException {
		String value = System.getenv(name);
		if (value == null || value.isEmpty()) {
			throw new OpenemsException("ENV [" + name + "] is not set");
		}
		return value;
	};
	
	public static Optional<String> getAsOptionalString(String name) {
		String value = System.getenv(name);
		return Optional.ofNullable(value);
	};

	public static int getAsInt(String name) throws OpenemsException {
		String valueString = EnvUtils.getAsString(name);
		try {
			return Integer.valueOf(valueString);
		} catch (NumberFormatException e) {
			throw new OpenemsException("ENV [" + name + "] value [" + valueString + "] is not an integer");
		}
	};
}
