package io.openems.edge.weather.openmeteo.forecast;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import com.google.gson.JsonElement;

import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProviderChain;
import io.openems.common.timedata.DurationUnit;

public class WeatherForecastDelayTimeProvider implements DelayTimeProvider<HttpResponse<JsonElement>> {

	private final Clock clock;

	public WeatherForecastDelayTimeProvider(Clock clock) {
		super();
		this.clock = clock;
	}

	@Override
	public Delay onFirstRunDelay() {
		return Delay.immediate();
	}

	@Override
	public Delay onErrorRunDelay(HttpError error) {
		return DelayTimeProviderChain.fixedDelay(Duration.ofSeconds(10))//
				.plusRandomDelay(60, ChronoUnit.SECONDS) //
				.getDelay();
	}

	@Override
	public Delay onSuccessRunDelay(HttpResponse<JsonElement> result) {
		return DelayTimeProviderChain.fixedAtEveryFull(this.clock, DurationUnit.ofHours(3))//
				.plusRandomDelay(300, ChronoUnit.SECONDS)//
				.getDelay();
	}
}
