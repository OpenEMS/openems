package io.openems.edge.weather.api;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.exceptions.OpenemsException;
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
	 * Retrieves the current quarterly weather snapshot based on the current time.
	 *
	 * @return The current quarterly weather snapshot, or null if no data is
	 *         available.
	 */
	public QuarterlyWeatherSnapshot getCurrentWeather() throws OpenemsException;

	/**
	 * Retrieves historical quarterly weather snapshots for the given date range and
	 * zone.
	 *
	 * @param dateFrom the start date of the historical period (inclusive)
	 * @param dateTo   the end date of the historical period (inclusive)
	 * @param zone     the target {@link ZoneId} for the returned timestamps
	 * @return a {@link CompletableFuture} that will complete with a list of
	 *         {@link QuarterlyWeatherSnapshot} for the requested period, or
	 *         complete exceptionally if the service is unavailable
	 */
	public CompletableFuture<List<QuarterlyWeatherSnapshot>> getHistoricalWeather(//
			LocalDate dateFrom, //
			LocalDate dateTo, //
			ZoneId zone);

	/**
	 * Retrieves the quarterly weather forecast starting from the current time.
	 *
	 * @param forecastQuarters the number of quarters for which the forecast should
	 *                         be retrieved
	 * @return a list of quarterly weather snapshots from now onwards
	 */
	public List<QuarterlyWeatherSnapshot> getQuarterlyWeatherForecast(int forecastQuarters) throws OpenemsException;

	/**
	 * Retrieves the hourly weather forecast starting from the current time.
	 *
	 * @param forecastHours the number of hours for which the forecast should be
	 *                      retrieved
	 * @return a list of hourly weather snapshots from now onwards
	 */
	public List<HourlyWeatherSnapshot> getHourlyWeatherForecast(int forecastHours) throws OpenemsException;

	/**
	 * Retrieves the daily weather forecast starting from today.
	 *
	 * @return a sorted list of daily weather snapshots from today onwards
	 */
	public List<DailyWeatherSnapshot> getDailyWeatherForecast() throws OpenemsException;
}
