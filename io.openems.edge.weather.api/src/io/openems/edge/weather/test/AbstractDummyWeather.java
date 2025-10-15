package io.openems.edge.weather.test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.weather.api.DailyWeatherSnapshot;
import io.openems.edge.weather.api.HourlyWeatherSnapshot;
import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;
import io.openems.edge.weather.api.Weather;

public abstract class AbstractDummyWeather<SELF extends AbstractDummyWeather<?>>
		extends AbstractDummyOpenemsComponent<SELF> implements Weather {

	private QuarterlyWeatherSnapshot currentWeather;
	private List<QuarterlyWeatherSnapshot> historicalQuarterlyWeatherData;
	private List<QuarterlyWeatherSnapshot> quarterlyWeatherForecast;
	private List<HourlyWeatherSnapshot> hourlyWeatherForecast;
	private List<DailyWeatherSnapshot> dailyWeatherForecast;

	protected AbstractDummyWeather(String id, io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(id, firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	public QuarterlyWeatherSnapshot getCurrentWeather() {
		return this.currentWeather;
	}

	@Override
	public CompletableFuture<List<QuarterlyWeatherSnapshot>> getHistoricalWeather(LocalDate dateFrom, LocalDate dateTo, ZoneId zone) {
		return CompletableFuture.completedFuture(this.historicalQuarterlyWeatherData);
	}

	@Override
	public List<QuarterlyWeatherSnapshot> getQuarterlyWeatherForecast(int forecastQuarters) {
		return this.quarterlyWeatherForecast;
	}
	
	@Override
	public List<HourlyWeatherSnapshot> getHourlyWeatherForecast(int forecastHours) {
		return this.hourlyWeatherForecast;
	}

	@Override
	public List<DailyWeatherSnapshot> getDailyWeatherForecast() {
		return this.dailyWeatherForecast;
	}

	/**
	 * Set the current weather.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF setCurrentWeather(QuarterlyWeatherSnapshot value) {
		this.currentWeather = value;
		return this.self();
	}

	/**
	 * Set the historical weather.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withHistoricalWeather(List<QuarterlyWeatherSnapshot> value) {
		this.historicalQuarterlyWeatherData = value;
		return this.self();
	}

	/**
	 * Set the quarterly weather forecast.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withQuarterlyWeatherForecast(List<QuarterlyWeatherSnapshot> value) {
		this.quarterlyWeatherForecast = value;
		return this.self();
	}
	
	/**
	 * Set the hourly weather forecast.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withHourlyWeatherForecast(List<HourlyWeatherSnapshot> value) {
		this.hourlyWeatherForecast = value;
		return this.self();
	}

	/**
	 * Set the daily weather forecast.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withDailyWeatherForecast(List<DailyWeatherSnapshot> value) {
		this.dailyWeatherForecast = value;
		return this.self();
	}
}
