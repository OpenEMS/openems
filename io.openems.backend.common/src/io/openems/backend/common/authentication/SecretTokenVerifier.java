package io.openems.backend.common.authentication;

import static io.openems.common.utils.StringUtils.isNullOrBlank;

import java.nio.charset.StandardCharsets;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.JWTVerifier;

public class SecretTokenVerifier {

	/** Minimum secret length in bytes: 256 bit, as required for HMAC-SHA256. */
	private static final int MIN_SECRET_BYTES = 32;

	private final JWTVerifier verifier;

	/**
	 * Creates a {@link SecretTokenVerifier}.
	 *
	 * @param secret the secret to verify tokens with
	 * @throws IllegalArgumentException if the secret is null or shorter than 32
	 *                                  bytes (256 bit)
	 */
	public SecretTokenVerifier(String secret) {
		requireValidSecret(secret);

		this.verifier = JWT.require(Algorithm.HMAC256(secret)) //
				.build();
	}

	/**
	 * Verifies the given token.
	 *
	 * @param token the token to verify
	 * @return the subject if the token is valid, otherwise null
	 */
	public String verify(String token) {
		if (isNullOrBlank(token)) {
			return null;
		}
		try {
			final var jwt = this.verifier.verify(token);
			return jwt.getSubject();
		} catch (JWTVerificationException _) {
			return null;
		}
	}

	/**
	 * Checks that the given secret is not null and long enough for HMAC-SHA256.
	 *
	 * @param secret the secret to check
	 */
	static void requireValidSecret(String secret) {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			throw new IllegalArgumentException(
					"secret must not be null and must be at least " + MIN_SECRET_BYTES + " bytes (256 bit) long");
		}
	}

}
