package io.openems.common.bridge.http.api;

import static io.openems.common.utils.StringUtils.startsWithIgnoreCase;

public final class HttpAuthorization {

	/**
	 * Prefix of the "Authorization" header value for Bearer tokens, including the
	 * trailing space.
	 */
	public static final String VALUE_BEARER_PREFIX = "Bearer ";

	/**
	 * Creates the content of the "Authorization" header for a Bearer token.
	 *
	 * @param token the Bearer token
	 * @return the created content of the "Authorization" header
	 */
	public static String bearer(String token) {
		return VALUE_BEARER_PREFIX + token;
	}

	/**
	 * Checks if the given "Authorization" header value carries a Bearer token. The
	 * auth scheme is case-insensitive, see RFC 6750.
	 *
	 * @param authorizationHeader the "Authorization" header value, or null
	 * @return true if the value starts with the Bearer prefix
	 */
	public static boolean isBearer(String authorizationHeader) {
		return authorizationHeader != null && startsWithIgnoreCase(authorizationHeader, VALUE_BEARER_PREFIX);
	}

	/**
	 * Extracts the token from the given "Authorization" header value.
	 *
	 * @param authorizationHeader the "Authorization" header value, or null
	 * @return the Bearer token; or null if the value is not a Bearer token
	 */
	public static String extractBearerToken(String authorizationHeader) {
		if (!isBearer(authorizationHeader)) {
			return null;
		}
		return authorizationHeader.substring(VALUE_BEARER_PREFIX.length());
	}

	private HttpAuthorization() {
	}
}
