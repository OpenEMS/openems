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

	/**
	 * Match two Strings, considering wildcards.
	 * 
	 * <ul>
	 * <li>if {@link #equals(Object)} is true -> return 0
	 * <li>if 'pattern' matches 'source' -> return value > 1; bigger values
	 * represent a better match
	 * <li>if both Strings do not match -> return -1
	 * </ul>
	 * 
	 * <p>
	 * Implementation note: only one wildcard is considered. Either the entire
	 * string is "*" or the wildcard is at the beginning or at the end of the
	 * pattern String. The the JUnit test for details.
	 * 
	 * @param source  the String to be evaluated
	 * @param pattern the pattern String, i.e. "meter*"
	 * @return an integer value representing the degree of matching
	 */
	public static int matchWildcard(String source, String pattern) {
		if (source.equals(pattern)) {
			return 0;
		} else if (pattern.equals("*")) {
			return 1;
		} else if (pattern.startsWith("*") && source.endsWith(pattern.substring(1))) {
			return pattern.length();
		} else if (pattern.endsWith("*") && source.startsWith(pattern.substring(0, pattern.length() - 1))) {
			return pattern.length();
		} else {
			return -1;
		}
	}
}
