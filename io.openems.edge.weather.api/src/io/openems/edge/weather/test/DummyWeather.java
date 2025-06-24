package io.openems.edge.weather.test;

import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.weather.api.Weather;
import io.openems.edge.weather.api.WeatherData;
import io.openems.edge.weather.api.WeatherSnapshot;

public class DummyWeather extends AbstractDummyOpenemsComponent<DummyWeather> implements Weather {

	private WeatherSnapshot currentWeatherData = null;
	private WeatherData historicalWeatherData = WeatherData.EMPTY_WEATHER_DATA;
	private WeatherData forecastedWeatherData = WeatherData.EMPTY_WEATHER_DATA;

	public DummyWeather(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				Weather.ChannelId.values());
	}

	@Override
	protected DummyWeather self() {
		return this;
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

	public void setCurrentWeather(WeatherSnapshot currentWeatherData) {
		this.currentWeatherData = currentWeatherData;
	}

	public void setHistoricalWeather(WeatherData historicalWeatherData) {
		this.historicalWeatherData = historicalWeatherData;
	}

	public void setWeatherForecast(WeatherData forecastedWeatherData) {
		this.forecastedWeatherData = forecastedWeatherData;
	}
}
