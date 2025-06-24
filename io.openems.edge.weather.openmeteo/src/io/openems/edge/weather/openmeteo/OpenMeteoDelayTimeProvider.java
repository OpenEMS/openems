package io.openems.edge.weather.openmeteo;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import io.openems.common.timedata.DurationUnit;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.time.DelayTimeProvider;
import io.openems.edge.bridge.http.time.DelayTimeProviderChain;

public class OpenMeteoDelayTimeProvider implements DelayTimeProvider {

	private final Clock clock;

	public OpenMeteoDelayTimeProvider(Clock clock) {
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
	public Delay onSuccessRunDelay(HttpResponse<String> result) {
		return DelayTimeProviderChain.fixedAtEveryFull(this.clock, DurationUnit.ofHours(6)).getDelay();
	}
}
