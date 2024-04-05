package io.openems.common.utils;

import java.security.SecureRandom;

/**
 * Generate secure random tokens.
 *
 * <p>
 * Source: http://stackoverflow.com/a/41156
 */
public class SecureRandomSingleton {

	private static SecureRandom instance;

	/**
	 * Gets the {@link SecureRandom} singleton instance.
	 *
	 * @return the {@link SecureRandom} instance
	 */
	public static synchronized SecureRandom getInstance() {
		if (SecureRandomSingleton.instance == null) {
			SecureRandomSingleton.instance = new SecureRandom();
		}
		return SecureRandomSingleton.instance;
	}

	private SecureRandomSingleton() {
	}
}
