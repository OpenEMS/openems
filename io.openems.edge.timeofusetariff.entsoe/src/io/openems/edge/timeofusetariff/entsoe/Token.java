package io.openems.edge.timeofusetariff.entsoe;

public final class Token {

	// DO NOT PUBLISH THIS TOKEN
	public static final String TOKEN = null;
	public static final String EXCHANGERATE_ACCESSKEY = null;
	// DO NOT PUBLISH THIS TOKEN

	private Token() {
	}

	protected static final String parseOrNull(String token) {
		if (token != null && !token.isBlank()) {
			return token;
		}
		return TOKEN;
	}

	protected static final String parseExchangeRateAccesskeyOrNull(String apikey) {
		if (apikey != null && !apikey.isBlank()) {
			return apikey;
		}
		return EXCHANGERATE_ACCESSKEY;
	}

}
