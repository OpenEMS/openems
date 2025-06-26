package io.openems.common.utils;

import java.security.SecureRandom;

public class PasswordUtils {

	/**
	 * Generate a random alphabetic numeric password with given length.
	 *
	 * @param length of the Password
	 * @return Generated Password
	 */
	public static String generateRandomPassword(int length) {
		var random = new SecureRandom();

		return random.ints(48, 122) //
				.filter(i -> Character.isAlphabetic(i) || Character.isDigit(i)) //
				.limit(length) //
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append) //
				.toString();
	}
}
