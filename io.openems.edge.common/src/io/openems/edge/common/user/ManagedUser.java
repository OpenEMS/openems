package io.openems.edge.common.user;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import io.openems.common.session.Language;
import io.openems.common.session.Role;

/**
 * A {@link User} that can be used for Logging in. Managed by the
 * {@link UserService}.
 */
public class ManagedUser extends User {

	public static final int KEY_LENGTH = 256;
	public static final int ITERATIONS = 10;

	private final byte[] password;
	private final byte[] salt;

	public ManagedUser(String id, String name, Language language, Role role, String passwordAsBase64,
			String saltAsBase64) {
		this(id, name, language, role, Base64.getDecoder().decode(passwordAsBase64),
				Base64.getDecoder().decode(saltAsBase64));
	}

	public ManagedUser(String id, String name, Language language, Role role, final byte[] password, final byte[] salt) {
		super(id, name, language, role);
		this.password = password;
		this.salt = salt;
	}

	/**
	 * Validates a given password against the Users password+salt.
	 *
	 * @param password the given password
	 * @return true if passwords match
	 */
	public boolean validatePassword(String password) {
		if (this.password == null || this.salt == null) {
			// no password existing -> allow access
			return true;
		}
		var hashedPassword = ManagedUser.hashPassword(password, this.salt, ITERATIONS, KEY_LENGTH);
		return Arrays.equals(hashedPassword, this.password);
	}

	/**
	 * Validates if password+salt match the given password.
	 *
	 * @param passwordAsBase64 the hashed password
	 * @param saltAsBase64     the salt
	 * @param password         the given password
	 * @return true if they match.
	 */
	public static boolean validatePassword(String passwordAsBase64, String saltAsBase64, String password) {
		return ManagedUser.validatePassword(Base64.getDecoder().decode(passwordAsBase64),
				Base64.getDecoder().decode(saltAsBase64), password);
	}

	/**
	 * Validates if password+salt match the given password.
	 *
	 * @param password1 the hashed password
	 * @param salt      the salt
	 * @param password2 the given password
	 * @return true if they match.
	 */
	public static boolean validatePassword(final byte[] password1, final byte[] salt, String password2) {
		var hashedPassword = ManagedUser.hashPassword(password2, salt, ITERATIONS, KEY_LENGTH);
		return Arrays.equals(hashedPassword, password1);
	}

	/**
	 * Hashes a password. Source: https://www.owasp.org/index.php/Hashing_Java.
	 *
	 * @param password   the password
	 * @param salt       the salt
	 * @param iterations the number of iterations
	 * @param keyLength  the length of the key
	 * @return the hashed password
	 */
	public static byte[] hashPassword(final String password, final byte[] salt, final int iterations,
			final int keyLength) {
		return ManagedUser.hashPassword(password.toCharArray(), salt, iterations, keyLength);
	}

	/**
	 * Hashes a password. Source: https://www.owasp.org/index.php/Hashing_Java.
	 *
	 * @param password   the password
	 * @param salt       the salt
	 * @param iterations the number of iterations
	 * @param keyLength  the length of the key
	 * @return the hashed password
	 */
	public static byte[] hashPassword(final char[] password, final byte[] salt, final int iterations,
			final int keyLength) {
		try {
			var skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
			var spec = new PBEKeySpec(password, salt, iterations, keyLength);
			var key = skf.generateSecret(spec);
			return key.getEncoded();

		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}

}
