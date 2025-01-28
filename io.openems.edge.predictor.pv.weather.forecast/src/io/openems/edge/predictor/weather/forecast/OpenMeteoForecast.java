package io.openems.edge.predictor.weather.forecast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenMeteoForecast {

	private static final Logger logger = LoggerFactory.getLogger(OpenMeteoForecast.class);
	private String apiUrl;
	private JsonObject json;
	private boolean debugMode;

	public OpenMeteoForecast(boolean debugMode) {
		this.debugMode = debugMode;
	}

	/**
	 * Log a debug message if debugMode is enabled.
	 *
	 * @param message The message to log.
	 */
	public void debugLog(String message) {
		if (debugMode) {
			logger.debug(message);
		}
	}

	/**
	 * Fetch weather forecast data for the given coordinates.
	 *
	 * @param latitude  Latitude of the location.
	 * @param longitude Longitude of the location.
	 * @throws Exception If fetching the data fails.
	 */
	public void fetchData(String latitude, String longitude) throws Exception {
		this.apiUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" + longitude
				+ "&minutely_15=shortwave_radiation&forecast_days=3&models=best_match";
		// Look 3 days to the past if debugMode is enabled
		if (this.debugMode) {
			this.apiUrl += "&past_days=1";
		}
		HttpURLConnection conn = null;

		try {
			// Convert the API URL String into a URI and then to a URL
			URL url = new URI(this.apiUrl).toURL();
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");

			if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
				throw new RuntimeException("Failed: HTTP error code: " + conn.getResponseCode());
			}

			// Read the response using the connection's input stream
			StringBuilder response = new StringBuilder();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
				String line;
				while ((line = br.readLine()) != null) {
					response.append(line);
				}
			}

			this.json = JsonParser.parseString(response.toString()).getAsJsonObject();
			debugLog("Weather data successfully fetched: " + this.apiUrl);
		} catch (Exception e) {
			logger.error("Error in fetching weather data: ", e);
			throw e;
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
		this.apiUrl = String
				.format("https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&minutely_15=" + forecastModel
				// +
				// "&hourly=shortwave_radiation,global_tilted_irradiance,shortwave_radiation_instant"
						+ "&forecast_days=3&models=best_match&tilt=%s&azimuth=%s", latitude, longitude, tilt, azimuth);

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

			debugLog("Weather data successfully fetched: " + this.apiUrl);
		} catch (Exception e) {
			logger.error("Error in fetching weather data from OpenMeteo API: ", e);
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
}
