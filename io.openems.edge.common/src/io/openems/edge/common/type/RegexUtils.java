package io.openems.edge.common.type;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtils {

	private RegexUtils() {
	}

	/**
	 * Apply a Regex {@link Pattern} to a string.
	 * 
	 * @param pattern the {@link Pattern}
	 * @param string  the string
	 * @return a {@link Matcher} if pattern matches the string
	 * @throws IllegalArgumentException if pattern does not match
	 */
	public static Matcher applyPatternOrError(Pattern pattern, String string) throws IllegalArgumentException {
		var matcher = pattern.matcher(string);
		if (!matcher.find()) {
			throw new IllegalArgumentException("Pattern [" + pattern + "] does not match [" + string + "]");
		}
		return matcher;
	}
}
