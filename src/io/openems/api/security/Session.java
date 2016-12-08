package io.openems.api.security;

import java.math.BigInteger;
import java.security.SecureRandom;

import io.openems.core.utilities.SecureRandomSingleton;

public class Session {

	private final static int SESSION_ID_LENGTH = 130;

	private final String token;
	private final User user;
	// TODO: timeout, IP address,...

	public Session(String token, User user) {
		this.token = token;
		this.user = user;
	}

	public Session(User user) {
		this(generateSessionId(), user);
	}

	public String getToken() {
		return token;
	}

	public User getUser() {
		return user;
	}

	public boolean isValid() {
		if (this.token != null && this.user != null) {
			return true;
		}
		return false;
	}

	private static String generateSessionId() {
		// Source: http://stackoverflow.com/a/41156
		SecureRandom sr = SecureRandomSingleton.getInstance();
		return new BigInteger(SESSION_ID_LENGTH, sr).toString(32);
	}
}
