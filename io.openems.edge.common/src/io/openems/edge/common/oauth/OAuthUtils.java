package io.openems.edge.common.oauth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class OAuthUtils {

	/**
	 * Generates a code verifier for PKCE (Proof Key for Code Exchange).
	 * 
	 * @return the code verifier
	 */
	public static String generateCodeVerifier() {
		final var secureRandom = new SecureRandom();
		byte[] codeVerifier = new byte[32];
		secureRandom.nextBytes(codeVerifier);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
	}

	/**
	 * Generates a code challenge from the given code verifier using SHA-256 hashing
	 * and Base64 URL encoding without padding.
	 * 
	 * @param codeVerifier the code verifier
	 * @return the code challenge
	 */
	public static String generateCodeChallenge(String codeVerifier) {
		try {
			var bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
			var messageDigest = MessageDigest.getInstance("SHA-256");
			messageDigest.update(bytes, 0, bytes.length);
			byte[] digest = messageDigest.digest();
			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private OAuthUtils() {
	}
}
