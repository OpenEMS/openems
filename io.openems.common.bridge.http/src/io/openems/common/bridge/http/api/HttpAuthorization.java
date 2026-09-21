package io.openems.common.bridge.http.api;

public final class HttpAuthorization {

	/**
	 * Creates the content of the "Authorization" header for a Bearer token.
	 * 
	 * @param token the Bearer token
	 * @return the created content of the "Authorization" header
	 */
	public static String bearer(String token) {
		return "Bearer " + token;
	}

	private HttpAuthorization() {
	}
}
