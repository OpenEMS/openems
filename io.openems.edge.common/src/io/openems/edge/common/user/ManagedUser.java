package io.openems.edge.common.user;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import io.openems.common.session.Role;

/**
 * A {@link User} that can be used for Logging in. Managed by the
 * {@link UserService}.
 */
public class ManagedUser extends User {

	private final static int KEY_LENGTH = 256;
	private final static int ITERATIONS = 10;

	private final byte[] password;
	private final byte[] salt;

	public ManagedUser(String id, String name, Role role, String passwordAsBase64, String saltAsBase64) {
		this(id, name, role, Base64.getDecoder().decode(passwordAsBase64), Base64.getDecoder().decode(saltAsBase64));
	}

	public ManagedUser(String id, String name, Role role, final byte[] password, final byte[] salt) {
		super(id, name, role);
		this.password = password;
		this.salt = salt;
	}

	public boolean validatePassword(String password) {
		if (this.password == null || this.salt == null) {
			// no password existing -> allow access
			return true;
		}
		byte[] hashedPassword = ManagedUser.hashPassword(password, this.salt);
		return Arrays.equals(hashedPassword, this.password);
	}

	public byte[] getPassword() {
		return password;
	}

	public byte[] getSalt() {
		return salt;
	}

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
}
