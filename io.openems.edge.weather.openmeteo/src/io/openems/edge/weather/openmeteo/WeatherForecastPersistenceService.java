package io.openems.edge.weather.openmeteo;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import io.openems.common.utils.ThreadPoolUtils;

public class WeatherForecastPersistenceService {

	private static final long INITIAL_DELAY_10_SECONDS = 10L;
	private static final long PERIOD_60_MINUTES = 60L;

	private final Logger log = LoggerFactory.getLogger(WeatherForecastPersistenceService.class);

	private final WeatherOpenMeteo parent;
	private final Supplier<Clock> clockSupplier;

	private ScheduledExecutorService scheduler;

	public WeatherForecastPersistenceService(//
			WeatherOpenMeteo parent, //
			Supplier<Clock> clockSupplier) {
		this.parent = parent;
		this.clockSupplier = clockSupplier;
	}

	/**
	 * Starts the hourly persistence job that updates weather forecast channels. The
	 * job runs immediately and then repeats at every full hour.
	 */
	public void startHourlyPersistenceJob() {
		if (this.scheduler != null) {
			this.deactivateHourlyPersistenceJob();
		}
		this.scheduler = Executors.newSingleThreadScheduledExecutor();

		// Short delay before the first update to ensure that the forecast data from the
		// API is available
		this.scheduler.schedule(//
				this::updateWeatherForecastChannels, //
				INITIAL_DELAY_10_SECONDS, //
				TimeUnit.SECONDS);

		long initialDelay = this.computeInitialDelayToNextFullHour();
		this.scheduler.scheduleAtFixedRate(//
				this::updateWeatherForecastChannels, //
				initialDelay, //
				PERIOD_60_MINUTES, //
				TimeUnit.MINUTES);
	}

	/**
	 * Deactivates the currently running scheduled persistence job, if any, and
	 * shuts down the associated executor service.
	 */
	public void deactivateHourlyPersistenceJob() {
		if (this.scheduler != null) {
			ThreadPoolUtils.shutdownAndAwaitTermination(this.scheduler, 5);
			this.scheduler = null;
		}
	}

	private void updateWeatherForecastChannels() {
		try {
			this.updateHourlyForecastChannels();
			this.updateDailyForecastChannels();
		} catch (Exception e) {
			this.log.error("Failed to update weather forecast channels", e);
			this.resetAllForecastChannels();
		}
	}

	@VisibleForTesting
	void updateHourlyForecastChannels() throws Exception {
		var hourlyForecast = this.parent.getHourlyWeatherForecast(24);

		if (hourlyForecast == null || hourlyForecast.size() < 25) {
			throw new IllegalStateException("Hourly weather forecast is null or too short");
		}

		int[] hours = { 0, 4, 8, 12, 16, 20, 24 };
		for (int h : hours) {
			this.setTemperatureIn(h, round(hourlyForecast.get(h).temperature()));
			this.setWeatherCodeIn(h, round(hourlyForecast.get(h).weatherCode()));
		}
	}

	@VisibleForTesting
	void updateDailyForecastChannels() throws Exception {
		var dailyForecast = this.parent.getDailyWeatherForecast();

		if (dailyForecast == null || dailyForecast.isEmpty()) {
			throw new IllegalStateException("Daily weather forecast is null or empty");
		}

		var todayForecast = dailyForecast.get(0);
		this.parent._setTodaysSunshineDuration(round(todayForecast.sunshineDuration()));
		this.parent._setTodaysMinTemperature(round(todayForecast.minTemperature()));
		this.parent._setTodaysMaxTemperature(round(todayForecast.maxTemperature()));
	}

	@VisibleForTesting
	void resetAllForecastChannels() {
		int[] hours = { 0, 4, 8, 12, 16, 20, 24 };
		for (int h : hours) {
			this.setTemperatureIn(h, null);
			this.setWeatherCodeIn(h, null);
		}

		this.parent._setTodaysSunshineDuration(null);
		this.parent._setTodaysMinTemperature(null);
		this.parent._setTodaysMaxTemperature(null);
	}

	private void setTemperatureIn(int hour, Integer value) {
		switch (hour) {
		case 0 -> this.parent._setCurrentTemperature(value);
		case 4 -> this.parent._setTemperatureIn4h(value);
		case 8 -> this.parent._setTemperatureIn8h(value);
		case 12 -> this.parent._setTemperatureIn12h(value);
		case 16 -> this.parent._setTemperatureIn16h(value);
		case 20 -> this.parent._setTemperatureIn20h(value);
		case 24 -> this.parent._setTemperatureIn24h(value);
		default -> throw new IllegalArgumentException("Unsupported hour: " + hour);
		}
	}

	private void setWeatherCodeIn(int hour, Integer value) {
		switch (hour) {
		case 0 -> this.parent._setCurrentWeatherCode(value);
		case 4 -> this.parent._setWeatherCodeIn4h(value);
		case 8 -> this.parent._setWeatherCodeIn8h(value);
		case 12 -> this.parent._setWeatherCodeIn12h(value);
		case 16 -> this.parent._setWeatherCodeIn16h(value);
		case 20 -> this.parent._setWeatherCodeIn20h(value);
		case 24 -> this.parent._setWeatherCodeIn24h(value);
		default -> throw new IllegalArgumentException("Unsupported hour: " + hour);
		}
	}

	@VisibleForTesting
	long computeInitialDelayToNextFullHour() {
		var now = ZonedDateTime.now(this.clockSupplier.get());
		var nextHour = now.truncatedTo(ChronoUnit.HOURS).plusHours(1);
		return Duration.between(now, nextHour).toMinutes();
	}

	private static int round(double value) {
		return (int) Math.round(value);
	}
}
