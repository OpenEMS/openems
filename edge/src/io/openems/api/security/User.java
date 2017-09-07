/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.api.security;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Optional;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.exception.OpenemsException;
import io.openems.common.utils.SecureRandomSingleton;
import io.openems.core.Config;

public enum User {
	/*
	 * "GUEST" generally has readonly access
	 *
	 * default: guest/guest
	 */
	GUEST( //
			new byte[] { 33, -62, 51, 37, 35, -81, 52, -51, 79, -67, 15, 47, -25, 42, 69, -68, -6, 19, 103, 33, -16,
					-36, -87, -24, 111, -20, -30, -19, -33, -106, -78, -107 }, //
			"user".getBytes(StandardCharsets.ISO_8859_1) //
	), //

	/*
	 * "OWNER" is the owner of the system.
	 *
	 * default: owner/owner
	 */
	OWNER( //
			new byte[] { 120, -104, 11, 5, -15, -45, -103, -24, 111, -31, 45, 112, -122, -57, -29, 120, 77, -22, -36, 2,
					102, 36, 32, 90, 109, 94, 125, 99, -82, 94, -95, -126 }, //
			"owner".getBytes(StandardCharsets.ISO_8859_1), //
			GUEST //
	), //
	/*
	 * "INSTALLER" is a qualified electrician with extended configuration access
	 *
	 * default: installer/installer
	 */
	INSTALLER( //
			new byte[] { -40, -19, 93, 50, 91, 5, 119, 6, -97, -53, -97, 30, -122, -76, -2, 95, -19, 2, 17, 102, -128,
					-104, 20, 90, 119, -110, 69, 109, 50, -15, -3, 106 }, //
			"installer".getBytes(StandardCharsets.ISO_8859_1), //
			GUEST, OWNER //
	), //
	/*
	 * "ADMIN" is allowed to do anything
	 *
	 * default: admin/admin
	 */
	ADMIN( //
			new byte[] { -73, 16, 18, -107, 69, 80, -112, 66, 61, 7, 22, -65, 33, -109, -119, 123, -55, 119, -7, 30, 37,
					51, 49, 83, 74, 28, -10, -18, -14, -72, -30, 10 }, //
			"admin".getBytes(StandardCharsets.ISO_8859_1), //
			GUEST, OWNER, INSTALLER //
	);

	/*
	 * all users; sorted in reverse order of importance
	 */
	private final static User[] USERS = new User[] { ADMIN, INSTALLER, OWNER, GUEST };
	private final static int KEY_LENGTH = 256;
	private final static int SALT_LENGTH = 32;
	private final static int ITERATIONS = 10;
	// was the user database initialized? Do not allow settings after initialization.
	private static boolean initialized = false;
	private final static Logger log = LoggerFactory.getLogger(User.class);

	public static User[] getUsers() {
		return USERS;
	}

	/**
	 * Get the User object for a given username.
	 *
	 * @param username
	 * @return User
	 * @throws OpenemsException
	 */
	public static User getUserByName(String username) throws OpenemsException {
		for (User user : USERS) {
			if (username.equals(user.getName())) {
				return user;
			}
		}
		throw new OpenemsException("Unable to find user [" + username + "].");
	}

	private static byte[] getRandomSalt(int length) {
		SecureRandom sr = SecureRandomSingleton.getInstance();
		byte[] salt = new byte[length];
		sr.nextBytes(salt);
		return salt;
	}

	/**
	 * Authenticates a user with his password
	 *
	 * @param password
	 * @return the authenticated User or null if authentication failed
	 */
	public static Optional<User> authenticate(String password) {
		// Search for any user with the given password
		for (User user : USERS) {
			if (user.checkPassword(password)) {
				log.info("Authentication successful with password only for user [" + user.getName() + "].");
				return Optional.ofNullable(user);
			}
		}
		log.info("Authentication failed with password only.");
		return Optional.empty();
	}

	public static Optional<User> authenticate(String username, String password) {
		// Search for user with given username
		for (User user : USERS) {
			if (username.equals(user.getName())) {
				if (user.checkPassword(password)) {
					log.info("Authentication successful for user[" + username + "].");
					return Optional.of(user);
				} else {
					log.info("Authentication failed for user[" + username + "]: wrong password");
					return Optional.empty();
				}
			}
		}
		// Search for any user with the given password
		return authenticate(password);
	}

	private static byte[] hashPassword(final String password, final byte[] salt) {
		return hashPassword(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
	}

	public void initialize(String passwordBase64, String saltBase64) {
		if (User.initialized) {
			log.warn("User database has already been initialized!");
		} else {
			Decoder decoder = Base64.getDecoder();
			this.password = decoder.decode(passwordBase64);
			this.salt = decoder.decode(saltBase64);
		}
	}

	public static void initializeFinished() {
		User.initialized = true;
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

	/*
	 * This object
	 */
	private byte[] password = null;
	private byte[] salt = null;
	// all roles this user has
	private User[] roles = null;

	private User() {}

	private User(final byte[] password, final byte[] salt, User... roles) {
		this.password = password;
		this.salt = salt;
		this.roles = Arrays.copyOf(roles, roles.length + 1);
		this.roles[this.roles.length - 1] = this;
	}

	public void changePassword(String oldPassword, String newPassword) throws OpenemsException {
		if (checkPassword(oldPassword)) {
			byte[] salt = getRandomSalt(SALT_LENGTH);
			byte[] password = hashPassword(newPassword, salt);
			this.password = password;
			this.salt = salt;
			Config.getInstance().writeConfigFile();
		} else {
			throw new OpenemsException("Access denied. Old password was wrong.");
		}
	}

	private boolean checkPassword(String password) {
		if (this.password == null || this.salt == null) {
			// no password existing -> allow access
			return true;
		}
		byte[] hashedPassword = hashPassword(password, this.salt);
		return Arrays.equals(hashedPassword, this.password);
	}

	public byte[] getHashedPassword() {
		return password;
	}

	public String getPasswordBase64() {
		return Base64.getEncoder().encodeToString(password);
	}

	public byte[] getSalt() {
		return salt;
	}

	public String getSaltBase64() {
		return Base64.getEncoder().encodeToString(salt);
	}

	public String getName() {
		return name().toLowerCase();
	}

	public User[] getRoles() {
		return this.roles;
	}

	public boolean hasRole(User role) {
		if (Arrays.asList(getRoles()).contains(role)) {
			return true;
		}
		return false;
	}
}
