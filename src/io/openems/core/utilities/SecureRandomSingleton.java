package io.openems.core.utilities;

import java.security.SecureRandom;

public class SecureRandomSingleton {
	private static SecureRandom instance;

	public static synchronized SecureRandom getInstance() {
		if (SecureRandomSingleton.instance == null) {
			SecureRandomSingleton.instance = new SecureRandom();
		}
		return SecureRandomSingleton.instance;
	}

	private SecureRandomSingleton() {}
}
