package io.openems.edge.predictor.weather.forecast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

import io.openems.common.types.ChannelAddress;

import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;

import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.api.prediction.AbstractPredictor;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.prediction.Predictor;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.PV.WeatherForecastModel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class PredictorWeatherForecastModelImpl extends AbstractPredictor
		implements Predictor, OpenemsComponent, PredictorWeatherForecastModel {

	private final Logger log = LoggerFactory.getLogger(PredictorWeatherForecastModelImpl.class);
	private double factorPv1; // Factor to multiply with short wave solar radiation to forecast PV production
	private double factorPv2;
	private double factorPv3;
	private String apiUrl;
	private JsonObject json;
	private int targetArraySize = 192; // 48hours

	private String forecastModel = "global_tilted_irradiance";
	private String weatherModel;

	private ZoneId timezone;
	private String encodedTimeZone;

	@Reference
	private ComponentManager componentManager;

	private Config config;
	// private OpenMeteoForecast openMeteoForecast; // Service to fetch weather data

	public PredictorWeatherForecastModelImpl() throws OpenemsNamedException {
		super(OpenemsComponent.ChannelId.values(), Controller.ChannelId.values(),
				PredictorWeatherForecastModel.ChannelId.values());
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws Exception {
		this.config = config;

		super.activate(context, this.config.id(), this.config.alias(), this.config.enabled(),
				this.config.channelAddresses(), this.config.logVerbosity());

		this.timezone = this.componentManager.getClock().getZone();
		this.encodedTimeZone = URLEncoder.encode(this.timezone.toString(), StandardCharsets.UTF_8);

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ClockProvider getClockProvider() {
		return this.componentManager;
	}

	protected Prediction createNewPrediction(ChannelAddress channelAddress) {
		try {
			// Initialize OpenMeteoForecast
			// this.openMeteoForecast = new OpenMeteoForecast(config.debugMode());
			// Get the current time
			ZonedDateTime now = ZonedDateTime.now(this.componentManager.getClock());

			ZonedDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);

			// Factors and configurations
			this.factorPv1 = Optional.ofNullable(config.factorPv1()).orElse(0.0);
			this.factorPv2 = Optional.ofNullable(config.factorPv2()).orElse(0.0);
			this.factorPv3 = Optional.ofNullable(config.factorPv3()).orElse(0.0);

			List<Double> pv1Radiation = new ArrayList<>();
			List<Double> pv2Radiation = new ArrayList<>();
			List<Double> pv3Radiation = new ArrayList<>();

			// Fetch data for PV1 if factor > 0
			if (factorPv1 > 0) {
				this.fetchDataWithTiltAndAzimuth(this.config.latitude(), this.config.longitude(),
						this.config.azimuthPv1(), this.config.tiltPv1(), this.forecastModel);
				Optional<List<Double>> pv1Data = this.getRadiation(this.forecastModel);
				pv1Data.ifPresent(pv1Radiation::addAll);
			}

			// Fetch data for PV2 if factor > 0
			if (factorPv2 > 0) {
				this.fetchDataWithTiltAndAzimuth(this.config.latitude(), this.config.longitude(),
						this.config.azimuthPv2(), this.config.tiltPv2(), this.forecastModel);
				Optional<List<Double>> pv2Data = this.getRadiation(this.forecastModel);
				pv2Data.ifPresent(pv2Radiation::addAll);
			}

			// Fetch data for PV3 if factor > 0
			if (factorPv3 > 0) {
				this.fetchDataWithTiltAndAzimuth(this.config.latitude(), this.config.longitude(),
						this.config.azimuthPv3(), this.config.tiltPv3(), this.forecastModel);
				Optional<List<Double>> pv3Data = this.getRadiation(this.forecastModel);
				pv3Data.ifPresent(pv3Radiation::addAll);
			}

			// Calculate the combined radiation data
			List<Double> combinedRadiation = new ArrayList<>();
			for (int i = 0; i < targetArraySize; i++) {
				double pv1Value = i < pv1Radiation.size() ? pv1Radiation.get(i) * factorPv1 : 0;
				double pv2Value = i < pv2Radiation.size() ? pv2Radiation.get(i) * factorPv2 : 0;
				double pv3Value = i < pv3Radiation.size() ? pv3Radiation.get(i) * factorPv3 : 0;
				combinedRadiation.add(pv1Value + pv2Value + pv3Value);
			}

			// If no data was fetched, return empty prediction
			if (combinedRadiation.isEmpty()) {
				return Prediction.EMPTY_PREDICTION;
			}

			// Calculate the index corresponding to the current 15-minute interval
			int currentIntervalIndex = (int) ChronoUnit.MINUTES.between(startOfDay, now) / 15;

			// Ensure the currentIntervalIndex is within bounds
			if (currentIntervalIndex >= combinedRadiation.size()) {
				return Prediction.EMPTY_PREDICTION;
			}

			// Create an array to store the forecast values for the next 192 intervals (48
			// hours in 15-minute steps)
			var values = new Integer[targetArraySize];

			// Extract and calculate data starting from the calculated currentIntervalIndex
			for (int i = 0; i < targetArraySize; i++) {
				int dataIndex = currentIntervalIndex + i;
				double pv1Value = dataIndex < pv1Radiation.size() ? pv1Radiation.get(dataIndex) * factorPv1 : 0;
				double pv2Value = dataIndex < pv2Radiation.size() ? pv2Radiation.get(dataIndex) * factorPv2 : 0;
				double pv3Value = dataIndex < pv3Radiation.size() ? pv3Radiation.get(dataIndex) * factorPv3 : 0;
				double sumValue = pv1Value + pv2Value + pv3Value;

				values[i] = (int) Math.round(sumValue);

				if (i == 0) { // Save nearest prediction in channels
					this._setProductionActivePower(values[0]);
					this._setProductionActivePowerPv1((int) Math.round(pv1Value));
					this._setProductionActivePowerPv2((int) Math.round(pv2Value));
					this._setProductionActivePowerPv3((int) Math.round(pv3Value));

				}

				// Debug output for each step
				if (this.config.debugMode()) {
					ZonedDateTime timestamp = startOfDay.plusMinutes((currentIntervalIndex + i) * 15);
					logDebug(this.log, "Index: " + i + " Time: " + timestamp.toString() + " PV1 radiation: "
							+ (dataIndex < pv1Radiation.size() ? pv1Radiation.get(dataIndex) : 0) + "W/m² / factor: "
							+ factorPv1 + " Result " + pv1Value + "W" + " PV2 radiation: "
							+ (dataIndex < pv2Radiation.size() ? pv2Radiation.get(dataIndex) : 0) + "W/m² / factor: "
							+ factorPv2 + " Result " + pv2Value + "W" + " PV3 radiation: "
							+ (dataIndex < pv3Radiation.size() ? pv3Radiation.get(dataIndex) : 0) + "W/m² / factor: "
							+ factorPv3 + " Result " + pv3Value + "W" + " Sum: " + sumValue + "W");
				}
			}

			// Return the prediction starting from the calculated time
			return Prediction.from(startOfDay.plusMinutes(currentIntervalIndex * 15), values);

		} catch (Exception e) {
			log.error("Error creating prediction: ", e);
			return Prediction.EMPTY_PREDICTION;
		}
	}

	/**
	 * Fetch weather forecast data for the given coordinates.
	 *
	 * @param latitude  Latitude of the location.
	 * @param longitude Longitude of the location.
	 * @param tilt      Tilt angle of the PV system.
	 * @param azimuth   Azimuth angle of the PV system.
	 * @throws Exception If fetching the data fails.
	 */
	public void fetchDataWithTiltAndAzimuth(String latitude, String longitude, int azimuth, int tilt,
			String forecastModel) throws Exception {

		this.weatherModel = this.config.WeatherModels().getWeatherModel();

		this.apiUrl = String
				.format("https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&minutely_15=" + forecastModel
				// +
				// "&hourly=shortwave_radiation,global_tilted_irradiance,shortwave_radiation_instant"
						+ "&forecast_days=3" + this.weatherModel + "&tilt=%s&azimuth=%s", latitude, longitude, tilt,
						azimuth + "&timezone=" + this.encodedTimeZone);

		HttpURLConnection conn = null;
		try {
			URL url = new URI(this.apiUrl).toURL(); // URL aus URI erstellen
			conn = (HttpURLConnection) url.openConnection(); // Verbindung initialisieren
			conn.setRequestMethod("GET");

			if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
				throw new RuntimeException("Failed: HTTP error code: " + conn.getResponseCode());
			}

			try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
				StringBuilder response = new StringBuilder();
				String line;
				while ((line = br.readLine()) != null) {
					response.append(line);
				}
				this.json = JsonParser.parseString(response.toString()).getAsJsonObject();
			}

			this.logDebug(this.log, "Weather data successfully fetched: " + this.apiUrl);
		} catch (Exception e) {
			log.error("Error in fetching weather data from OpenMeteo API: ", e);
			throw e;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	/**
	 * Get the shortwave radiation data from the fetched JSON.
	 *
	 * @return Optional list of shortwave radiation values.
	 * 
	 *         in the future one can add other factors such as temparature, cloud
	 *         cover , snow cover etc , each parameters can be fetched individually
	 *         and used for production power calculation
	 */
	public Optional<List<Double>> getRadiation(String forecastModel) {
		return Optional.ofNullable(json).map(j -> j.getAsJsonObject("minutely_15"))
				// .map(m -> m.getAsJsonArray("shortwave_radiation"))
				.map(m -> m.getAsJsonArray(forecastModel)).map(arr -> IntStream.range(0, arr.size()).mapToObj(arr::get)
						.map(element -> element.getAsDouble()).collect(Collectors.toList()));
	}

	/**
	 * Uses Info Log for further debug features.
	 */
	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}
}
