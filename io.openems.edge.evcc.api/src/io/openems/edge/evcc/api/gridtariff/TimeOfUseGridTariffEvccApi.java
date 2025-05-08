package io.openems.edge.evcc.api.gridtariff;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.common.timedata.DurationUnit;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.api.UrlBuilder;
import io.openems.edge.bridge.http.time.DelayTimeProvider;
import io.openems.edge.bridge.http.time.DelayTimeProviderChain;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

public class TimeOfUseGridTariffEvccApi {

	private static final Logger log = LoggerFactory
			.getLogger(TimeOfUseGridTariffEvccApi.class);
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
	private BridgeHttp httpBridge;
	private String apiUrl;
	private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(
			TimeOfUsePrices.EMPTY_PRICES);

	private static class GridTariffProvider implements DelayTimeProvider {
		@Override
		public Delay onFirstRunDelay() {
			return Delay.of(Duration.ofSeconds(5));
		}

		@Override
		public Delay onErrorRunDelay(HttpError error) {
			return DelayTimeProviderChain.fixedDelay(Duration.ofMinutes(1))
					.getDelay();
		}

		@Override
		public Delay onSuccessRunDelay(HttpResponse<String> result) {
			return DelayTimeProviderChain
					.fixedAtEveryFull(java.time.Clock.systemDefaultZone(),
							DurationUnit.ofMinutes(15))
					.getDelay();
		}
	}

	public TimeOfUseGridTariffEvccApi() {
		this.httpBridge = null;
		this.apiUrl = "";
	}

	public TimeOfUseGridTariffEvccApi(String apiUrl, BridgeHttp httpBridge) {
		this.apiUrl = apiUrl;
		this.httpBridge = httpBridge;
		this.httpBridge.subscribeTime(new GridTariffProvider(),
				this::createEndpoint, this::handleResponse, this::handleError);
	}

	private Endpoint createEndpoint() {
		var url = UrlBuilder.parse(this.apiUrl);
		return new Endpoint(url.toEncodedString(), HttpMethod.GET, 5000, 5000,
				null, this.buildRequestHeaders());
	}

	private Map<String, String> buildRequestHeaders() {
		return Map.of("Accept", "application/json");
	}

	private void handleResponse(HttpResponse<String> response)
			throws IOException {
		if (response.status().isSuccessful()) {
			this.prices.set(this.parsePrices(response.data()));
		} else {
			log.warn("Failed to fetch prices. HTTP status code: {}",
					response.status().code());
		}
	}

	private void handleError(HttpError error) {
		log.error("HTTP Error: {}", error.getMessage());
	}

	/**
	 * Returns the current grid tariff prices.
	 *
	 * @return TimeOfUsePrices grid prices.
	 */
	public TimeOfUsePrices getPrices() {
		return this.prices.get();
	}

	private TimeOfUsePrices parsePrices(String jsonData) {
		try {
			var jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
			var resultObject = jsonObject.getAsJsonObject("result");
			var ratesArray = resultObject.getAsJsonArray("rates");
			var result = ImmutableSortedMap
					.<ZonedDateTime, Double>naturalOrder();

			for (JsonElement rateElement : ratesArray) {
				if (rateElement.isJsonObject()) {
					JsonObject rateObject = rateElement.getAsJsonObject();
					String startString = rateObject.get("start").getAsString();
					String endString = rateObject.get("end").getAsString();
					double value = rateObject.has("price")
							? rateObject.get("price").getAsDouble() * 1000
							: rateObject.get("value").getAsDouble() * 1000;

					ZonedDateTime startsAt = ZonedDateTime.parse(startString,
							DATE_FORMATTER);
					ZonedDateTime endsAt = ZonedDateTime.parse(endString,
							DATE_FORMATTER);
					long duration = Duration.between(startsAt, endsAt)
							.toMinutes();

					switch ((int) duration) {
						case 60 :
							for (int i = 0; i < 4; i++) {
								ZonedDateTime quarterStart = startsAt
										.plusMinutes(i * 15);
								result.put(quarterStart, value);
							}
							break;
						case 15 :
							result.put(startsAt, value);
							break;
						default :
							throw new IllegalArgumentException(
									"Unexpected duration for rate: " + duration
											+ " minutes");
					}
				} else {
					log.error("Rate element is not a JsonObject: {}",
							rateElement);
				}
			}

			return TimeOfUsePrices.from(result.build());
		} catch (Exception e) {
			log.error("Failed to parse API data", e);
			return TimeOfUsePrices.EMPTY_PRICES;
		}
	}

	/**
	 * Sets the HTTP bridge for handling network communication.
	 *
	 * @param httpBridge
	 *            the HTTP bridge to be used
	 */
	public void setHttpBridge(BridgeHttp httpBridge) {
		this.httpBridge = httpBridge;
	}

	/**
	 * Sets the API URL for network requests.
	 *
	 * @param apiUrl
	 *            the API URL to be used
	 */
	public void setApiUrl(String apiUrl) {
		this.apiUrl = apiUrl;
	}
}
