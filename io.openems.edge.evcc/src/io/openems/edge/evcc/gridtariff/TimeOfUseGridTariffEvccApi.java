package io.openems.edge.evcc.gridtariff;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.timedata.DurationUnit;
import io.openems.common.utils.JsonUtils;
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

	private TimeOfUsePrices parsePrices(String jsonData) {
		try {
			var result = ImmutableSortedMap
					.<ZonedDateTime, Double>naturalOrder();
			var resultObject = JsonUtils.parseToJsonObject(jsonData)
					.getAsJsonObject("result");
			var dataArray = JsonUtils.getAsJsonArray(resultObject, "rates");
			long duration = -1;
			
			for (var element : dataArray) {
				var optionalPrice = JsonUtils.getAsOptionalDouble(element,
						"price");
				var optionalValue = JsonUtils.getAsOptionalDouble(element,
						"value");

				if (optionalPrice.isEmpty() && optionalValue.isEmpty()) {
					log.error(
							"Missing 'price' or 'value' field in JSON data: {}",
							element);
					return TimeOfUsePrices.EMPTY_PRICES;
				}

				double value = optionalPrice
						.orElseGet(() -> optionalValue.get()) * 1000;

				String startString = JsonUtils.getAsString(element, "start");
				ZonedDateTime startTime = ZonedDateTime.parse(startString);

				if (duration < 0) {
					String endString = JsonUtils.getAsString(element, "end");
					ZonedDateTime endTime = ZonedDateTime.parse(endString);

					duration = Duration.between(startTime, endTime).toMinutes();
				}

				switch ((int) duration) {
					case 60 :
						result.put(startTime, value);
						result.put(startTime.plusMinutes(15), value);
						result.put(startTime.plusMinutes(30), value);
						result.put(startTime.plusMinutes(45), value);
						break;
					case 30 :
						result.put(startTime, value);
						result.put(startTime.plusMinutes(15), value);
						break;
					case 15 :
						result.put(startTime, value);
						break;
					default :
						log.error("Unexpected duration for rate: {} minutes",
								duration);
						return TimeOfUsePrices.EMPTY_PRICES;
				}
			}

			return TimeOfUsePrices.from(result.build());
		} catch (Exception e) {
			log.error("Failed to parse API data", e);
			return TimeOfUsePrices.EMPTY_PRICES;
		}
	}

	/**
	 * Returns the current grid tariff prices.
	 *
	 * @return the TimeOfUsePrices object containing tariff information.
	 */
	public TimeOfUsePrices getPrices() {
		return this.prices.get();
	}

	/**
	 * Sets the HTTP bridge for handling network communication.
	 *
	 * @param httpBridge
	 *            the HTTP bridge to be used.
	 */
	public void setHttpBridge(BridgeHttp httpBridge) {
		this.httpBridge = httpBridge;
	}

	/**
	 * Sets the API URL for retrieving grid tariff data.
	 *
	 * @param apiUrl
	 *            the API endpoint URL.
	 */
	public void setApiUrl(String apiUrl) {
		this.apiUrl = apiUrl;
	}

}
