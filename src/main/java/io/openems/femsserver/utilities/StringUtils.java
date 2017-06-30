package io.openems.femsserver.utilities;

import com.google.gson.JsonObject;

public class StringUtils {

	public static String toShortString(JsonObject j, int length) {
		String s = j.toString();
		if (s.length() > length - 3) {
			return s.substring(0, length - 3) + "...";
		} else {
			return s;
		}
	}
}
