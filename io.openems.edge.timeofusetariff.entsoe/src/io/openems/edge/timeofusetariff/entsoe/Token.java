package io.openems.edge.timeofusetariff.entsoe;

public final class Token {

	// DO NOT PUBLISH THIS TOKEN
	public static final String TOKEN = "29ea7484-f60c-421a-b312-9db19dfd930a";
	public static final String EXCHANGERATE_ACCESSKEY = "fa28268048deba2e437f4c6e42b676a8";
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
