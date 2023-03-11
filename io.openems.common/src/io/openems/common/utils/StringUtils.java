package io.openems.common.utils;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;

/**
 * Provides static helper functions for string manipulation.
 */
public class StringUtils {

	/**
	 * Shortens a string to a given length.
	 *
	 * <p>
	 * Example: converts a string "hello world" to "hello w..."
	 *
	 * @param s      the string
	 * @param length the target string length
	 * @return the shortened string
	 */
	public static String toShortString(String s, int length) {
		if (s.length() > length - 3) {
			return s.substring(0, length - 3) + "...";
		}
		return s;
	}

	/**
	 * Shortens a {@link JsonElement} string representation to a given length.
	 *
	 * <p>
	 * Example: converts a "{ 'foo': 'bar' }" to "{ 'foo': '..."
	 *
	 * @param j      the {@link JsonElement}
	 * @param length the target string length
	 * @return the shortened string
	 */
	public static String toShortString(JsonElement j, int length) {
		var s = j.toString();
		return StringUtils.toShortString(s, length);
	}

	/**
	 * Convert the first letter of a string to Upper-Case.
	 *
	 * <p>
	 * Example: converts "hello world" to "Hello world"
	 *
	 * @param s the string
	 * @return the converted string
	 */
	public static String capitalizeFirstLetter(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	/**
	 * Match two Strings, considering wildcards.
	 *
	 * <ul>
	 * <li>if {@link #equals(Object)} is true -&gt; return 0
	 * <li>if 'pattern' matches 'source' -&gt; return value &gt; 1; bigger values
	 * represent a better match
	 * <li>if both Strings do not match -&gt; return -1
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
		}
		if (pattern.equals("*")) {
			return 1;
		} else if (pattern.startsWith("*") && source.endsWith(pattern.substring(1))) {
			return pattern.length();
		} else if (pattern.endsWith("*") && source.startsWith(pattern.substring(0, pattern.length() - 1))) {
			return pattern.length();
		} else {
			return -1;
		}
	}

	private static final Predicate<String> DETECT_INTEGER_PATTERN = //
			Pattern.compile("^[-+]?[0-9]+$").asPredicate();
	private static final Predicate<String> DETECT_FLOAT_PATTERN = //
			Pattern.compile("^[-+]?[0-9]*\\.[0-9]+$").asPredicate();

	/**
	 * Checks if the given string matches an Integer pattern, i.e. if could be
	 * parsed to Integer/Long.
	 * 
	 * @param string a string
	 * @return true if it matches Integer
	 */
	public static boolean matchesIntegerPattern(String string) {
		return DETECT_INTEGER_PATTERN.test(string);
	}

	/**
	 * Checks if the given string matches an Float pattern, i.e. if could be parsed
	 * to Float/Double.
	 * 
	 * @param string a string
	 * @return true if it matches Float
	 */
	public static boolean matchesFloatPattern(String string) {
		return DETECT_FLOAT_PATTERN.test(string);
	}
}
