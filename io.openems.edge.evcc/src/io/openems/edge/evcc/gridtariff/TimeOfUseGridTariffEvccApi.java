package io.openems.edge.evcc.gridtariff;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonObject;

import io.openems.common.timedata.DurationUnit;
import io.openems.common.utils.JsonUtils;
import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.api.UrlBuilder;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProviderChain;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

public class TimeOfUseGridTariffEvccApi {

	@Reference
	private ComponentManager componentManager;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	private static final Logger log = LoggerFactory.getLogger(TimeOfUseGridTariffEvccApi.class);
	private BridgeHttp httpBridge;
	private HttpBridgeTimeService timeService;
	private Clock clock;
	private String apiUrl;
	private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(TimeOfUsePrices.EMPTY_PRICES);

	private static class GridTariffProvider implements DelayTimeProvider {
		@Override
		public Delay onFirstRunDelay() {
			return Delay.of(Duration.ZERO);
		}

		@Override
		public Delay onErrorRunDelay(HttpError error) {
			return DelayTimeProviderChain.fixedDelay(Duration.ofMinutes(1)).getDelay();
		}

		@Override
		public Delay onSuccessRunDelay(HttpResponse<String> result) {
			return DelayTimeProviderChain.fixedAtEveryFull(java.time.Clock.systemUTC(), DurationUnit.ofMinutes(15))
					.getDelay();
		}
	}

	public TimeOfUseGridTariffEvccApi() {
		this.apiUrl = "";
		if (this.componentManager != null) {
			this.clock = this.componentManager.getClock();
		}
		if (this.httpBridgeFactory != null) {
			this.httpBridge = this.httpBridgeFactory.get();
			this.timeService = this.httpBridge.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
			this.timeService.subscribeTime(new GridTariffProvider(), this::createEndpoint, this::handleResponse,
					this::handleError);
		}
	}

	public TimeOfUseGridTariffEvccApi(Clock clock) {
		this.apiUrl = "";
		this.clock = clock;
		if (this.httpBridgeFactory != null) {
			this.httpBridge = this.httpBridgeFactory.get();
			this.timeService = this.httpBridge.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
			this.timeService.subscribeTime(new GridTariffProvider(), this::createEndpoint, this::handleResponse,
					this::handleError);
		}
	}

	public TimeOfUseGridTariffEvccApi(String apiUrl, HttpBridgeTimeService timeService, Clock clock) {
		this.apiUrl = apiUrl;
		this.clock = clock;
		this.timeService = timeService;
		this.timeService.subscribeTime(new GridTariffProvider(), this::createEndpoint, this::handleResponse,
				this::handleError);
	}

	private Endpoint createEndpoint() {
		var url = UrlBuilder.parse(this.apiUrl);
		return new Endpoint(url.toEncodedString(), HttpMethod.GET, 5000, 5000, null, this.buildRequestHeaders());
	}

	private Map<String, String> buildRequestHeaders() {
		return Map.of("Accept", "application/json");
	}

	/**
	 * Handles the HTTP response received from an API that provides time-of-use
	 * prices.
	 *
	 * <p>
	 * This method processes the response by parsing the price data, ensuring it
	 * contains future timestamps, and updating the stored price information
	 * accordingly. If the API request fails or no valid prices are retrieved, it
	 * retains the last known valid prices.
	 * </p>
	 *
	 * @param response The HTTP response containing price data.
	 * @throws IOException If an error occurs while processing the response.
	 */
	public void handleResponse(HttpResponse<String> response) throws IOException {
		if (response.status().isSuccessful()) {
			log.debug("prices retrieved, parsing");
			TimeOfUsePrices newPrices = this.parsePrices(response.data());

			// prices parsed and not empty
			if (!newPrices.isEmpty()) {
				ZonedDateTime now = ZonedDateTime.now(this.clock);
				ZonedDateTime lastFullHour = now.withMinute(0).withSecond(0).withNano(0);
				ZonedDateTime utcTime = lastFullHour.withZoneSameInstant(ZoneId.of("UTC"));
				newPrices = TimeOfUsePrices.from(utcTime, newPrices);

				// replace already known prices if they contain future timestamps
				if (!newPrices.isEmpty()) {
					this.prices.set(newPrices);
					log.debug("prices retrieved successfully");
					return;
				}
			}
			log.warn("retrieved no future prices");
		}
		log.warn("API request failed. Retaining last known valid prices.");
	}

	private void handleError(HttpError error) {
		log.error("HTTP Error: {}", error.getMessage());
	}

	/**
	 * Parses the JSON response from the grid tariff API and converts it into a
	 * structured {@link TimeOfUsePrices} object.
	 *
	 * <p>
	 * The method extracts pricing data from the JSON string, processes time
	 * intervals, and normalizes prices based on predefined duration segments (15,
	 * 30, or 60 minutes).
	 * </p>
	 *
	 * <p>
	 * If the provided JSON data lacks both "price" and "value" fields, the method
	 * returns {@link TimeOfUsePrices#EMPTY_PRICES}. In case of a parsing error, an
	 * empty result is also returned.
	 * </p>
	 *
	 * @param jsonData the JSON string containing time-of-use tariff rates.
	 * @return a {@link TimeOfUsePrices} object representing the parsed pricing
	 *         data.
	 */
	public TimeOfUsePrices parsePrices(String jsonData) {
		try {
			var result = ImmutableSortedMap.<ZonedDateTime, Double>naturalOrder();
			
			JsonObject jsonObject = JsonUtils.parseToJsonObject(jsonData);
	        JsonObject resultObject = jsonObject.has("result") 
	                ? jsonObject.getAsJsonObject("result") // Fallback, older API 
	                : jsonObject;

			var dataArray = JsonUtils.getAsJsonArray(resultObject, "rates");
			long duration = -1;

			for (var element : dataArray) {
				var optionalPrice = JsonUtils.getAsOptionalDouble(element, "price");
				var optionalValue = JsonUtils.getAsOptionalDouble(element, "value");

				if (optionalPrice.isEmpty() && optionalValue.isEmpty()) {
					log.error("Missing 'price' or 'value' field in JSON data: {}", element);
					return TimeOfUsePrices.EMPTY_PRICES;
				}

				double value = new BigDecimal((optionalPrice.orElseGet(() -> optionalValue.get()) * 1000))
						.setScale(3, RoundingMode.HALF_UP).doubleValue();

				String startString = JsonUtils.getAsString(element, "start");
				ZonedDateTime startTime = ZonedDateTime.parse(startString);
				ZonedDateTime utcTime = startTime.withZoneSameInstant(ZoneId.of("UTC"));

				if (duration < 0) {
					String endString = JsonUtils.getAsString(element, "end");
					ZonedDateTime endTime = ZonedDateTime.parse(endString);

					duration = Duration.between(startTime, endTime).toMinutes();
				}

				switch ((int) duration) {
				case 60:
					result.put(utcTime, value);
					result.put(utcTime.plusMinutes(15), value);
					result.put(utcTime.plusMinutes(30), value);
					result.put(utcTime.plusMinutes(45), value);
					break;
				case 30:
					result.put(utcTime, value);
					result.put(utcTime.plusMinutes(15), value);
					break;
				case 15:
					result.put(utcTime, value);
					break;
				default:
					log.error("Unexpected duration for rate: {} minutes", duration);
					return TimeOfUsePrices.EMPTY_PRICES;
				}
			}

			TimeOfUsePrices prices = TimeOfUsePrices.from(result.build());
			log.debug("parsedPrices: ", prices);

			return prices;
		} catch (Exception e) {
			log.error("Failed to parse API data: {}", e.getMessage());
			log.debug("Exception:  {}", e);
			return TimeOfUsePrices.EMPTY_PRICES;
		}
	}

	/**
	 * Returns the current grid tariff prices.
	 *
	 * @return the TimeOfUsePrices object containing tariff information.
	 */
	public TimeOfUsePrices getPrices() {
		ZonedDateTime utcTime = ZonedDateTime.now(this.clock).withZoneSameInstant(ZoneId.of("UTC"));
		TimeOfUsePrices prices = TimeOfUsePrices.from(utcTime, this.prices.get());
		log.debug("Prices: ", prices);
		return prices;
	}

	/**
	 * Sets the HTTP bridge time service for handling network communication.
	 *
	 * @param timeService the HTTP bridge time service to be used.
	 */
	public void setTimeService(HttpBridgeTimeService timeService) {
		this.timeService = timeService;
	}

	/**
	 * Sets the API URL for retrieving grid tariff data.
	 *
	 * @param apiUrl the API endpoint URL.
	 */
	public void setApiUrl(String apiUrl) {
		this.apiUrl = apiUrl;
	}

}
