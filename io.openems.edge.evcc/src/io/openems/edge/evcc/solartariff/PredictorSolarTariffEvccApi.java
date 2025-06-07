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

	private static final Logger log = LoggerFactory.getLogger(PredictorSolarTariffEvccApi.class);
	private BridgeHttp httpBridge;
	private String apiUrl;
	private Prediction prediction;
	private Integer currentPrediction;
	private ImmutableSortedMap<ZonedDateTime, Integer> solarData = ImmutableSortedMap.of();

	private static class SolarTariffProvider implements DelayTimeProvider {
		@Override
		public Delay onFirstRunDelay() {
			return Delay.of(Duration.ZERO);
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
	}

	public PredictorSolarTariffEvccApi(String apiUrl, BridgeHttp httpBridge, Clock clock) {
		this.apiUrl = apiUrl;
		this.httpBridge = httpBridge;
		this.httpBridge.subscribeTime(new SolarTariffProvider(), this::createEndpoint, this::handleResponse,
				this::handleError);
	}

	private Endpoint createEndpoint() {
		var url = UrlBuilder.parse(this.apiUrl);
		return new Endpoint(url.toEncodedString(), HttpMethod.GET, 5000, 5000, null,
				Map.of("Accept", "application/json"));
	}

	private void handleError(HttpError error) {
		log.error("HTTP Error: {}", error.getMessage());
	}

	/**
	 * Handles the HTTP response received from the Solar Forecast API.
	 * <p>
	 * This method processes the response, extracts the prediction data if
	 * available, and updates the current prediction accordingly. If the response is
	 * invalid or the request fails, it sets the prediction to a default empty
	 * value.
	 * </p>
	 *
	 * @param response The HTTP response received from the API.
	 * @throws IOException If an error occurs while processing the response.
	 */
	public void handleResponse(HttpResponse<String> response) throws IOException {
		if (response.status().isSuccessful()) {
			log.info("Received response from Solar Forecast API.");
			try {
				log.debug("Raw API Response: {}", response.data());
				this.prediction = this.parsePrediction(response.data());
				this.currentPrediction = this.prediction.getFirst().intValue();
			} catch (OpenemsNamedException e) {
				log.warn("Invalid or empty response from Solar Forecast API. Exception: {}", e.getMessage());
				this.currentPrediction = 0;
				this.prediction = Prediction.EMPTY_PREDICTION;
			}
		} else {
			log.warn("Failed to fetch solar forecast. HTTP status code: {}", response.status().code());
			this.currentPrediction = 0;
			this.prediction = Prediction.EMPTY_PREDICTION;
		}
	}

	private ImmutableSortedMap<ZonedDateTime, Integer> parseJson(String jsonData) throws OpenemsNamedException {
		var result = ImmutableSortedMap.<ZonedDateTime, Integer>naturalOrder();
		var jsonObject = JsonUtils.parseToJsonObject(jsonData);
		var resultObject = JsonUtils.getAsJsonObject(jsonObject, "result");
		var dataArray = JsonUtils.getAsJsonArray(resultObject, "rates");

		log.debug("Parsing JSON response: {}", jsonObject);

		long duration = -1;

		for (var element : dataArray) {
			log.debug("Processing JSON element: {}", element);

			Integer power = JsonUtils.getAsOptionalInt(element, "value")
					.orElseGet(() -> JsonUtils.getAsOptionalInt(element, "price").orElse(null));

			if (power == null) {
				log.error("Missing 'value' or 'price' field in JSON data: {}", element);
				return ImmutableSortedMap.of();
			}

			String startString = JsonUtils.getAsString(element, "start");
			ZonedDateTime startTime = ZonedDateTime.parse(startString);
			log.debug("Parsed start time: {}", startTime);

			if (duration < 0) {
				String endString = JsonUtils.getAsString(element, "end");
				ZonedDateTime endTime = ZonedDateTime.parse(endString);
				duration = Duration.between(startTime, endTime).toMinutes();

				log.debug("Parsed end time: {} - Duration: {} minutes", endTime, duration);
			}

			switch ((int) duration) {
			case 60:
				result.put(startTime, power);
				result.put(startTime.plusMinutes(15), power);
				result.put(startTime.plusMinutes(30), power);
				result.put(startTime.plusMinutes(45), power);
				break;
			case 30:
				result.put(startTime, power);
				result.put(startTime.plusMinutes(15), power);
				break;
			case 15:
				result.put(startTime, power);
				break;
			default:
				log.error("Unexpected duration for power: {} minutes", duration);
				return ImmutableSortedMap.of();
			}
		}

		log.debug("Final parsed solar data map: {}", result.build());
		return result.build();
	}

	/**
	 * Parses the JSON response from the solar tariff API and generates a
	 * prediction.
	 *
	 * <p>
	 * The method extracts solar production data from the provided JSON string,
	 * aligns the values with the current time, and converts the data into a
	 * prediction format. The prediction consists of values mapped to 15-minute
	 * intervals for accurate forecasting.
	 * </p>
	 *
	 * @param jsonData the JSON string containing solar tariff rates.
	 * @return a {@link Prediction} object representing the processed solar tariff
	 *         forecast.
	 * @throws OpenemsNamedException if the JSON parsing fails or contains invalid
	 *                               data.
	 */
	public Prediction parsePrediction(String jsonData) throws OpenemsNamedException {
		this.solarData = this.parseJson(jsonData);
		log.debug("Parsed solar data: {}", this.solarData);

		LocalDateTime localCurrentHour = LocalDateTime.now().withSecond(0).withNano(0).withMinute(0);
		ZoneId localZone = ZoneId.systemDefault();
		ZonedDateTime localZoned = localCurrentHour.atZone(localZone);
		ZonedDateTime currentHour = localZoned.withZoneSameInstant(ZoneId.of("UTC"));

		var values = new Integer[192];
		int i = 0;

		for (Entry<ZonedDateTime, Integer> entry : this.solarData.entrySet()) {
			if (!entry.getKey().isBefore(currentHour) && i < values.length) {
				values[i++] = entry.getValue();
			}
		}

		Prediction prediction = Prediction.from(currentHour, values);
		if (prediction.asArray().length == 0) {
			log.warn("no future values retrieved");
		}

		log.debug("parsed prediction: {}", prediction);

		return prediction;
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
	 * @param httpBridge the HTTP bridge to be used.
	 */
	public void setHttpBridge(BridgeHttp httpBridge) {
		this.httpBridge = httpBridge;
	}

	/**
	 * Sets the API URL for retrieving solar forecast data.
	 *
	 * @param apiUrl the API URL to be used.
	 */
	public void setApiUrl(String apiUrl) {
		this.apiUrl = apiUrl;
	}

}
