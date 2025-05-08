package io.openems.edge.evcc.api.solartariff;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
	private final TreeMap<ZonedDateTime, Integer> hourlySolarData = new TreeMap<>();

	private String apiUrl;
	private Clock clock;

	private Prediction prediction;

	private Integer currentPrediction;

	private static class SolarTariffProvider implements DelayTimeProvider {
		@Override
		public Delay onFirstRunDelay() {
			return Delay.of(java.time.Duration.ofSeconds(5));
		}

		@Override
		public Delay onErrorRunDelay(HttpError error) {
			return Delay.of(java.time.Duration.ofMinutes(1));
		}

		@Override
		public Delay onSuccessRunDelay(HttpResponse<String> result) {
			return Delay.of(java.time.Duration.ofMinutes(15));
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

	private void handleResponse(HttpResponse<String> response)
			throws IOException {
		this.prediction = Prediction.EMPTY_PREDICTION;
		if (response.status().isSuccessful()) {
			try {
				JsonObject jsonResponse = JsonUtils
						.parseToJsonObject(response.data());
				if (jsonResponse.has("result")) {
					JsonArray js = jsonResponse.getAsJsonObject("result")
							.getAsJsonArray("rates");

					log.info("Solar Forecast API responded successfully.");

					LocalDateTime localCurrentHour = LocalDateTime
							.now(this.clock).withNano(0).withMinute(0)
							.withSecond(0);
					ZoneId zoneId = ZoneId.of("UTC");
					ZonedDateTime currentHour = localCurrentHour.atZone(zoneId);

					long duration = -1;
					if (js != null) {
						this.hourlySolarData.clear();

						for (int i = 0; i < js.size(); i++) {
							JsonObject data = js.get(i).getAsJsonObject();
							JsonElement startsAt = data.get("start");

							ZonedDateTime zonedDateTime = ZonedDateTime
									.parse(startsAt.getAsString());

							if (duration < 0) {
								JsonElement endsAt = data.get("end");

								ZonedDateTime zonedDateTimeEnd = ZonedDateTime
										.parse(endsAt.getAsString());

								duration = Duration.between(zonedDateTime,
										zonedDateTimeEnd).toMinutes();
							}

							ZonedDateTime utcDateTime = zonedDateTime
									.withZoneSameInstant(ZoneId.of("UTC"));

							JsonObject jsonObject = js.get(i).getAsJsonObject();

							Integer power = jsonObject.has("value")
									? jsonObject.get("value").getAsInt()
									: (jsonObject.has("price")
											? jsonObject.get("price").getAsInt()
											: null);

							this.hourlySolarData.put(utcDateTime, power);
						}

						if (this.hourlySolarData != null
								&& !this.hourlySolarData.isEmpty()) {

							// Create an array to store the forecast values for
							// the 48 hours in 15-minute steps
							var values = new Integer[192];

							int i = 0;
							for (Entry<ZonedDateTime, Integer> entry : this.hourlySolarData
									.entrySet()) {
								log.debug("loop processing[" + i + "]: "
										+ entry.getKey());
								if (!entry.getKey().isBefore(currentHour)
										&& i < values.length) {
									switch ((int) duration) {
										case 60 :
											// convert hourly values in 15min
											// steps
											values[i++] = entry.getValue();
											values[i++] = entry.getValue();
											values[i++] = entry.getValue();
											values[i++] = entry.getValue();
											break;
										case 15 :
											// no conversion
											values[i++] = entry.getValue();
											break;
										default :
											throw new IllegalArgumentException(
													"Unexpected duration for power: "
															+ duration
															+ " minutes");
									}
								}
							}
							this.prediction = Prediction.from(currentHour,
									values);
							this.currentPrediction = values[0];
						} else {
							log.warn(
									"Invalid or empty response from Solar Forecast API.");
						}
					}
				} else {
					log.warn(
							"Invalid or empty response from Solar Forecast API.");
				}
			} catch (OpenemsNamedException e) {
				log.warn("Invalid or empty response from Solar Forecast API.");
			}
		} else {
			log.warn("Failed to fetch solar forecast. HTTP status code: {}",
					response.status().code());
		}
	}

	private void handleError(HttpError error) {
		log.error("HTTP Error: {}", error.getMessage());
	}

	/**
	 * Returns the current prediction value.
	 *
	 * @return the predicted value as an Integer.
	 */
	public Prediction getPrediction() {
		return this.prediction;
	}

	/**
	 * Retrieves the current prediction status.
	 *
	 * @return the current prediction as an Integer.
	 */
	public Integer getCurrentPrediction() {
		return this.currentPrediction;
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

	/**
	 * Sets the clock for handling time-related functionality.
	 *
	 * @param clock
	 *            the Clock instance to be used
	 */
	public void setClock(Clock clock) {
		this.clock = clock;
	}

}
