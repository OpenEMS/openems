package io.openems.edge.evcc.weather;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import io.openems.common.timedata.DurationUnit;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.time.DelayTimeProvider;
import io.openems.edge.bridge.http.time.DelayTimeProviderChain;

/**
 * Provides delay timings for EVCC weather API polling.
 *
 * <p>
 * Uses wall-clock aligned delays to prevent time drift and distributes load
 * with random jitter.
 */
public class EvccDelayTimeProvider implements DelayTimeProvider {

	private final Clock clock;

	public EvccDelayTimeProvider(Clock clock) {
		super();
		this.clock = clock;
	}

	@Override
	public Delay onFirstRunDelay() {
		return Delay.immediate();
	}

	@Override
	public Delay onErrorRunDelay(HttpError error) {
		// Retry after 1 minute with up to 60 seconds of random jitter
		// This prevents thundering herd if multiple instances fail simultaneously
		return DelayTimeProviderChain.fixedDelay(Duration.ofMinutes(1)) //
				.plusRandomDelay(60, ChronoUnit.SECONDS) //
				.getDelay();
	}

	@Override
	public Delay onSuccessRunDelay(HttpResponse<String> result) {
		// Update at every 15-minute boundary (00, 15, 30, 45 minutes past the hour)
		// with up to 60 seconds of random jitter to distribute load
		// This prevents time drift by aligning to wall-clock times
		return DelayTimeProviderChain.fixedAtEveryFull(this.clock, DurationUnit.ofMinutes(15)) //
				.plusRandomDelay(60, ChronoUnit.SECONDS) //
				.getDelay();
	}
}

