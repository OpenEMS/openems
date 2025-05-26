package io.openems.edge.evcc.solartariff;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.api.UrlBuilder;
import io.openems.edge.bridge.http.time.DelayTimeProvider;
import io.openems.edge.predictor.api.prediction.Prediction;

@Component(name = "PredictorSolarTariffEvccApi", immediate = true)
public class PredictorSolarTariffEvccApi {

	private static final Logger log = LoggerFactory
			.getLogger(PredictorSolarTariffEvccApi.class);
	private BridgeHttp httpBridge;
	private String apiUrl;
	private Clock clock;
	private Prediction prediction;
	private Integer currentPrediction;
	private ImmutableSortedMap<ZonedDateTime, Integer> solarData = ImmutableSortedMap
			.of();

	private static class SolarTariffProvider implements DelayTimeProvider {
		@Override
		public Delay onFirstRunDelay() {
			return Delay.of(Duration.ofSeconds(5));
		}

		@Override
		public Delay onErrorRunDelay(HttpError error) {
			return Delay.of(Duration.ofMinutes(1));
		}

		@Override
		public Delay onSuccessRunDelay(HttpResponse<String> result) {
			return Delay.of(Duration.ofMinutes(15));
		}
	}

	public PredictorSolarTariffEvccApi() {
		this.httpBridge = null;
		this.apiUrl = "";
		this.clock = null;
	}

	public PredictorSolarTariffEvccApi(String apiUrl, BridgeHttp httpBridge,
			Clock clock) {
		this.apiUrl = apiUrl;
		this.httpBridge = httpBridge;
		this.clock = clock;
		this.httpBridge.subscribeTime(new SolarTariffProvider(),
				this::createEndpoint, this::handleResponse, this::handleError);
	}

	private Endpoint createEndpoint() {
		var url = UrlBuilder.parse(this.apiUrl);
		return new Endpoint(url.toEncodedString(), HttpMethod.GET, 5000, 5000,
				null, Map.of("Accept", "application/json"));
	}

	private void handleError(HttpError error) {
		log.error("HTTP Error: {}", error.getMessage());
	}

	private void handleResponse(HttpResponse<String> response)
			throws IOException {
		this.prediction = Prediction.EMPTY_PREDICTION;

		if (response.status().isSuccessful()) {
			try {
				log.info("Received response from Solar Forecast API.");
				log.debug("Raw API Response: {}", response.data());

				this.solarData = this.parsePrices(response.data());
				log.debug("Parsed solar data: {}", this.solarData);

				this.calculatePrediction();
				log.debug("Calculated prediction: {}", this.prediction);
			} catch (OpenemsNamedException e) {
				log.warn(
						"Invalid or empty response from Solar Forecast API. Exception: {}",
						e.getMessage());
				this.prediction = Prediction.EMPTY_PREDICTION;
			}
		} else {
			log.warn("Failed to fetch solar forecast. HTTP status code: {}",
					response.status().code());
		}
	}

	private ImmutableSortedMap<ZonedDateTime, Integer> parsePrices(
			String jsonData) throws OpenemsNamedException {
		var result = ImmutableSortedMap.<ZonedDateTime, Integer>naturalOrder();
		var jsonObject = JsonUtils.parseToJsonObject(jsonData);
		var resultObject = JsonUtils.getAsJsonObject(jsonObject, "result");
		var dataArray = JsonUtils.getAsJsonArray(resultObject, "rates");

		log.debug("Parsing JSON response: {}", jsonObject);

		long duration = -1;

		for (var element : dataArray) {
			log.debug("Processing JSON element: {}", element);

			Integer power = JsonUtils.getAsOptionalInt(element, "value")
					.orElseGet(() -> JsonUtils
							.getAsOptionalInt(element, "price").orElse(null));

			if (power == null) {
				log.error("Missing 'value' or 'price' field in JSON data: {}",
						element);
				return ImmutableSortedMap.of();
			}

			String startString = JsonUtils.getAsString(element, "start");
			ZonedDateTime startTime = ZonedDateTime.parse(startString);
			log.debug("Parsed start time: {}", startTime);

			if (duration < 0) {
				String endString = JsonUtils.getAsString(element, "end");
				ZonedDateTime endTime = ZonedDateTime.parse(endString);
				duration = Duration.between(startTime, endTime).toMinutes();

				log.debug("Parsed end time: {} - Duration: {} minutes", endTime,
						duration);
			}

			switch ((int) duration) {
				case 60 :
					result.put(startTime, power);
					result.put(startTime.plusMinutes(15), power);
					result.put(startTime.plusMinutes(30), power);
					result.put(startTime.plusMinutes(45), power);
					break;
				case 30 :
					result.put(startTime, power);
					result.put(startTime.plusMinutes(15), power);
					break;
				case 15 :
					result.put(startTime, power);
					break;
				default :
					log.error("Unexpected duration for power: {} minutes",
							duration);
					return ImmutableSortedMap.of();
			}
		}

		log.debug("Final parsed solar data map: {}", result.build());
		return result.build();
	}

	private void calculatePrediction() {
		LocalDateTime localCurrentHour = LocalDateTime.now(this.clock)
				.withSecond(0).withNano(0).withMinute(0);
		ZoneId zoneId = ZoneId.of("UTC");
		ZonedDateTime currentHour = localCurrentHour.atZone(zoneId);

		var values = new Integer[192];
		int i = 0;

		for (Entry<ZonedDateTime, Integer> entry : this.solarData.entrySet()) {
			if (!entry.getKey().isBefore(currentHour) && i < values.length) {
				values[i++] = entry.getValue();
			}
		}

		this.prediction = Prediction.from(currentHour, values);
		this.currentPrediction = values[0];
	}

	/**
	 * Returns the current solar tariff prediction.
	 *
	 * @return the predicted solar tariff value.
	 */
	public Prediction getPrediction() {
		return this.prediction;
	}

	/**
	 * Retrieves the most recent forecasted value.
	 *
	 * @return the current predicted solar value in Wh.
	 */
	public Integer getCurrentPrediction() {
		return this.currentPrediction;
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
	 * Sets the API URL for retrieving solar forecast data.
	 *
	 * @param apiUrl
	 *            the API URL to be used.
	 */
	public void setApiUrl(String apiUrl) {
		this.apiUrl = apiUrl;
	}

	/**
	 * Sets the clock for handling time-based calculations.
	 *
	 * @param clock
	 *            the Clock instance to be used.
	 */
	public void setClock(Clock clock) {
		this.clock = clock;
	}

}
