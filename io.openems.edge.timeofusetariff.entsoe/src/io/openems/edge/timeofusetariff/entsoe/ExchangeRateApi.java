package io.openems.edge.timeofusetariff.entsoe;

import java.io.IOException;

import io.openems.edge.common.currency.Currency;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class ExchangeRateApi {

	private static final String BASE_URL = "https://api.exchangerate.host/latest?base=%s";
	private static final OkHttpClient client = new OkHttpClient();

	/**
	 * Fetches the exchange rate from base currency EUR to the currency requested by
	 * user.
	 * 
	 * @param currency {@link Currency} requested by User.
	 * @return the Response string.
	 * @throws IOException on error.
	 */
	protected static String getExchangeRate() throws IOException {
		var URL = String.format(BASE_URL, "EUR");
		var request = new Request.Builder() //
				.url(URL) //
				.build();

		try (var response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Failed to fetch exchange rate. HTTP status code: " + response.code());
			}

			return response.body().string();
		}
	}

}
