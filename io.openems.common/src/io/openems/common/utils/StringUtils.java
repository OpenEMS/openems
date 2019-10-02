package io.openems.common.utils;

import com.google.gson.JsonElement;

/**
 * Provides static helper functions for string manipulation.
 */
public class StringUtils {

	public static String toShortString(String s, int length) {
		if (s.length() > length - 3) {
			return s.substring(0, length - 3) + "...";
		} else {
			return s;
		}
	}

	public static String toShortString(JsonElement j, int length) {
		String s = j.toString();
		return toShortString(s, length);
	}

	public static String capitalizeFirstLetter(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}
}
