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

	/**
	 * Causes this character sequence to be replaced by the reverse of the sequence.
	 * 
	 * @param string to be reversed.
	 * @return reversed String.
	 */
	public static String reverse(String string) {
		return new StringBuilder(string).reverse().toString();
	}

	/**
	 * If the given string is null return false, otherwise result of
	 * {@link String#contains(CharSequence)} is returned.
	 * 
	 * @param string the string to check
	 * @param value  the sequence to search for
	 * @return true if string is not null and string contains value, otherwise false
	 */
	public static boolean containsWithNullCheck(String string, String value) {
		return string != null && string.contains(value);
	}

	/**
	 * Returns the 'alternative' if 'original' is null or blank.
	 *
	 * @param original    the original value, can be null, empty or filled with
	 *                    white-space only
	 * @param alternative the alternative value
	 * @return either the 'defined' value (not null, not empty, not only
	 *         white-space), alternatively the 'orElse' value
	 */
	public static String definedOrElse(String original, String alternative) {
		if (original != null && !original.isBlank()) {
			return original;
		}
		return alternative;
	}
}
