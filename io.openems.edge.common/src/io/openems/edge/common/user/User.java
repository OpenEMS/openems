package io.openems.edge.common.user;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import io.openems.common.session.Role;

public class User {

	private final String name;
	private final Role role;
	private byte[] password;
	private byte[] salt;

	public User(String name, Role role, String passwordAsBase64, String saltAsBase64) {
		this(name, role, Base64.getDecoder().decode(passwordAsBase64), Base64.getDecoder().decode(saltAsBase64));
	}

	public User(String name, Role role, final byte[] password, final byte[] salt) {
		this.name = name;
		this.role = role;
		this.password = password;
		this.salt = salt;
	}

	public boolean validatePassword(String password) {
		if (this.password == null || this.salt == null) {
			// no password existing -> allow access
			return true;
		}
		byte[] hashedPassword = User.hashPassword(password, this.salt);
		return Arrays.equals(hashedPassword, this.password);
	}

	public String getName() {
		return this.name;
	}

	public Role getRole() {
		return this.role;
	}

	public byte[] getPassword() {
		return password;
	}

	public byte[] getSalt() {
		return salt;
	}

	/*
	 * statics
	 */
	private final static int KEY_LENGTH = 256;
	// private final static int SALT_LENGTH = 32;
	private final static int ITERATIONS = 10;

	private static byte[] hashPassword(final String password, final byte[] salt) {
		return hashPassword(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
	}

	/**
	 * Source: https://www.owasp.org/index.php/Hashing_Java
	 *
	 * @param password
	 * @param salt
	 * @param iterations
	 * @param keyLength
	 * @return
	 */
	private static byte[] hashPassword(final char[] password, final byte[] salt, final int iterations,
			final int keyLength) {
		try {
			SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
			PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
			SecretKey key = skf.generateSecret(spec);
			byte[] res = key.getEncoded();
			return res;

		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return "User [name=" + name + ", role=" + role + "]";
	}
	
}
