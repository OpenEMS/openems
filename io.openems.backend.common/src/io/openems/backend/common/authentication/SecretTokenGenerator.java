package io.openems.backend.common.authentication;

import java.time.Clock;
import java.time.Duration;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

public class SecretTokenGenerator {

	/** Time-to-live of generated tokens. */
	private static final Duration TOKEN_TTL = Duration.ofMinutes(5);

	/**
	 * Generates a JWT token with the given id and secret. The token will expire in
	 * 5 minutes.
	 *
	 * @param id     the id to be included in the token
	 * @param secret the secret to sign the token
	 * @return the generated token
	 * @throws IllegalArgumentException if the secret is null or shorter than 32
	 *                                  bytes (256 bit), or if the token cannot be
	 *                                  created or signed
	 */
	public static String generateAuthToken(String id, String secret) {
		SecretTokenVerifier.requireValidSecret(secret);
		try {
			return JWT.create() //
					.withSubject(id) //
					.withExpiresAt(Clock.systemUTC().instant().plus(TOKEN_TTL)) //
					.sign(Algorithm.HMAC256(secret));
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("Failed to generate auth token", e);
		}
	}

	private SecretTokenGenerator() {
	}
}
