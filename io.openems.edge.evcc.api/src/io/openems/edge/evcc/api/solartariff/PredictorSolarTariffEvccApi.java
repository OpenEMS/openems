package io.openems.edge.evcc.api.solartariff;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class PredictorSolarTariffEvccApi {

	private final String apiUrl;

	public PredictorSolarTariffEvccApi(String apiUrl) {
		this.apiUrl = apiUrl;
	}
	
	/**
	 * Fetches the solar forecast data from the API.
	 *
	 * <p>This method sends a GET request to the configured API URL and retrieves the response 
	 * as a JSON object. If the response contains a valid "result" field, the method extracts 
	 * and returns the "rates" array from the JSON structure. Otherwise, an exception is thrown 
	 * to indicate an error in fetching the forecast data.
	 *
	 * @return A {@link JsonArray} containing the forecasted solar tariff rates.
	 * @throws OpenemsNamedException If the API response is invalid or empty.
	 */
	public JsonArray getSolarForecast() throws OpenemsNamedException {
		JsonObject jsonResponse = this.sendGetRequest(this.apiUrl);
		if (jsonResponse != null && jsonResponse.has("result")) {
			return jsonResponse.getAsJsonObject("result")
					.getAsJsonArray("rates");
		} else {
			throw new OpenemsException(
					"Invalid or empty response from Solar Forecast API.");
		}
	}

	private JsonObject sendGetRequest(String url) throws OpenemsNamedException {
		HttpClient client = HttpClient.newBuilder()
				.connectTimeout(java.time.Duration.ofSeconds(5)).build();

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
				.GET().timeout(java.time.Duration.ofSeconds(5)).build();

		try {
			HttpResponse<String> response = client.send(request,
					HttpResponse.BodyHandlers.ofString());
			int status = response.statusCode();
			String body = response.body();

			if (status < 300) {
				return JsonUtils.parseToJsonObject(body);
			} else {
				throw new OpenemsException(
						"Error while reading from API. Response code: " + status
								+ ". " + body);
			}
		} catch (IOException | InterruptedException e) {
			throw new OpenemsException(
					"Unable to connect to the Solar Forecast API.");
		}
	}
}