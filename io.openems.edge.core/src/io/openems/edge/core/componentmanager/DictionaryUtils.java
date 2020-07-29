package io.openems.edge.core.componentmanager;

import java.util.Dictionary;
import java.util.Optional;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

/**
 * Provides helper utilities to handle {@link Dictionary}s.
 */
public class DictionaryUtils {

	public static String getAsString(Dictionary<String, Object> dict, String key) {
		return TypeUtils.getAsType(OpenemsType.STRING, dict.get(key));
	}

	public static Optional<String> getAsOptionalString(Dictionary<String, Object> dict, String key) {
		return Optional.ofNullable(getAsString(dict, key));
	}

	public static Integer getAsInteger(Dictionary<String, Object> dict, String key) {
		return TypeUtils.getAsType(OpenemsType.INTEGER, dict.get(key));
	}

	public static Optional<Integer> getAsOptionalInteger(Dictionary<String, Object> dict, String key) {
		return Optional.ofNullable(getAsInteger(dict, key));
	}

	public static Boolean getAsBoolean(Dictionary<String, Object> dict, String key) {
		return TypeUtils.getAsType(OpenemsType.BOOLEAN, dict.get(key));
	}

	public static Optional<Boolean> getAsOptionalBoolean(Dictionary<String, Object> dict, String key) {
		return Optional.ofNullable(getAsBoolean(dict, key));
	}

}
