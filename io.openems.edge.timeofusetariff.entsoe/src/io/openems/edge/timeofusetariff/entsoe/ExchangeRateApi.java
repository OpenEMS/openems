package io.openems.edge.timeofusetariff.entsoe;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * A utility class for fetching exchange rates from a web API.
 * 
 * <p>
 * Day ahead prices retrieved from ENTSO-E are usually in EUR and might have to
 * be converted to the user's currency using the exchange rates provided by
 * Exchange Rate API. For more information on the ExchangeRate API, visit:
 * <a href=
 * "https://exchangerate.host/#/docs">https://exchangerate.host/#/docs</a>
 */
public class ExchangeRateApi {

	private static final String BASE_URL = "http://api.exchangerate.host/live?access_key=%s&source=EUR";
	private static final OkHttpClient client = new OkHttpClient();

	/**
	 * Fetches the exchange rates from base currency EUR.
	 * 
	 * @return the Response string.
	 * @throws IOException on error.
	 */
	protected static String getExchangeRates(String accessKey) throws IOException {
		var URL = String.format(BASE_URL, accessKey);
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
