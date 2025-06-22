package io.openems.edge.weather.api;

import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.Doc;

@ProviderType
public interface Weather {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Retrieves the current weather snapshot based on the current time.
	 *
	 * @return The current weather snapshot, or null if no data is available for the
	 *         rounded time.
	 */
	public WeatherSnapshot getCurrentWeather();

	/**
	 * Fetches historical weather data for the specified date range.
	 * 
	 * @param dateFrom The start date and time of the desired historical weather
	 *                 data.
	 * @param dateTo   The end date and time of the desired historical weather data.
	 * @return A CompletableFuture containing the historical weather data, or empty
	 *         data if an error occurs.
	 */
	public CompletableFuture<WeatherData> getHistoricalWeather(ZonedDateTime dateFrom, ZonedDateTime dateTo);

	/**
	 * Retrieves the weather forecast, adjusted to the nearest quarter-hour based on
	 * the current time.
	 * 
	 * @return The weather forecast, or empty weather data if no relevant forecast
	 *         is available.
	 */
	public WeatherData getWeatherForecast();
}
