package io.openems.edge.weather.test;

import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.weather.api.Weather;
import io.openems.edge.weather.api.WeatherData;
import io.openems.edge.weather.api.WeatherSnapshot;

public abstract class AbstractDummyWeather<SELF extends AbstractDummyWeather<?>>
		extends AbstractDummyOpenemsComponent<SELF> implements Weather {

	private WeatherSnapshot currentWeatherData = null;
	private WeatherData historicalWeatherData = WeatherData.EMPTY_WEATHER_DATA;
	private WeatherData forecastedWeatherData = WeatherData.EMPTY_WEATHER_DATA;

	protected AbstractDummyWeather(String id, io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(id, firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	public WeatherSnapshot getCurrentWeather() {
		return this.currentWeatherData;
	}

	@Override
	public CompletableFuture<WeatherData> getHistoricalWeather(ZonedDateTime dateFrom, ZonedDateTime dateTo) {
		return CompletableFuture.completedFuture(this.historicalWeatherData);
	}

	@Override
	public WeatherData getWeatherForecast() {
		return this.forecastedWeatherData;
	}

	/**
	 * Set the Current Weather.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF setCurrentWeather(WeatherSnapshot value) {
		this.currentWeatherData = value;
		return this.self();
	}

	/**
	 * Set the Historical Weather.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withHistoricalWeather(WeatherData value) {
		this.historicalWeatherData = value;
		return this.self();
	}

	/**
	 * Set the Weather Forecast.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withWeatherForecast(WeatherData value) {
		this.forecastedWeatherData = value;
		return this.self();
	}
}
