package io.openems.edge.timeofusetariff.entsoe;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * A utility class for fetching exchange rates from a web API.
 * 
 * <p>
 * Day ahead prices retrieved from ENTSO-E are always in Euros and required to
 * be converted to the user's currency using the exchange rates provided by
 * Exchange Rate API. For more information on the ExchangeRate API, visit:
 * <a href=
 * "https://exchangerate.host/#/docs">https://exchangerate.host/#/docs</a>
 */
public class ExchangeRateApi {

	private static final String BASE_URL = "https://api.exchangerate.host/latest?base=%s";
	private static final OkHttpClient client = new OkHttpClient();
	private static final String URL = String.format(BASE_URL, "EUR");

	/**
	 * Fetches the exchange rates from base currency EUR.
	 * 
	 * @return the Response string.
	 * @throws IOException on error.
	 */
	protected static String getExchangeRates() throws IOException {
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
