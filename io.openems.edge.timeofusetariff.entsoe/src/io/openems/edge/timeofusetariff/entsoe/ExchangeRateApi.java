package io.openems.edge.timeofusetariff.entsoe;

import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;

import java.io.IOException;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.currency.Currency;
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
// TODO this should be extracted to a Exchange-Rate API + Provider
public class ExchangeRateApi {

	private static final String BASE_URL = "http://api.exchangerate.host/live?access_key=%s&source=%s&currencies=%s";

	private static final OkHttpClient client = new OkHttpClient();

	/**
	 * Fetches the exchange rate from exchangerate.host.
	 * 
	 * @param accessKey personal API access key.
	 * @param source    the source currency (e.g. EUR)
	 * @param target    the target currency (e.g. SEK)
	 * @return the exchange rate.
	 * @throws IOException           on error.
	 * @throws OpenemsNamedException on error
	 */
	protected static double getExchangeRate(String accessKey, String source, Currency target)
			throws IOException, OpenemsNamedException {
		var request = new Request.Builder() //
				.url(String.format(BASE_URL, accessKey, source, target.name())) //
				.build();

		try (var response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Failed to fetch exchange rate. HTTP status code: " + response.code());
			}

			return parseResponse(response.body().string(), source, target);
		}
	}

	/**
	 * Parses the response string from exchangerate.host.
	 * 
	 * @param response the response string
	 * @param source   the source currency (e.g. EUR)
	 * @param target   the target currency (e.g. SEK)
	 * @return the exchange rate.
	 * @throws OpenemsNamedException on error.
	 */
	protected static double parseResponse(String response, String source, Currency target)
			throws OpenemsNamedException {
		var json = parseToJsonObject(response);
		var quotes = getAsJsonObject(json, "quotes");
		var result = getAsDouble(quotes, source + target.name());
		return result;
	}

}
