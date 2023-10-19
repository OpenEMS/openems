package io.openems.edge.timeofusetariff.entsoe;

public final class Token {

	// DO NOT PUBLISH THIS TOKEN
	public static final String TOKEN = null;
	// DO NOT PUBLISH THIS TOKEN

	private Token() {
	}

	protected static final String parseOrNull(String token) {
		if (token != null && !token.isBlank()) {
			return token;
		}
		return TOKEN;
	}

}
