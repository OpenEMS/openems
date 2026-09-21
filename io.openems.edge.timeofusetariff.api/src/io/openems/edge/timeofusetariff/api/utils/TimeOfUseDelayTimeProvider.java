package io.openems.edge.timeofusetariff.api.utils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.types.HttpStatus;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

public record TimeOfUseDelayTimeProvider(//
		Clock clock, //
		LocalTime updateTime //
) implements DelayTimeProvider<TimeOfUsePrices> {

	public TimeOfUseDelayTimeProvider(Clock clock) {
		this(clock, LocalTime.of(16, 0));
	}

	@Override
	public Delay onFirstRunDelay() {
		return Delay.immediate();
	}

	@Override
	public Delay onErrorRunDelay(HttpError error) {
		if (error instanceof HttpError.ResponseError responseError
				&& responseError.status.code() == HttpStatus.UNAUTHORIZED.code()) {
			return Delay.infinite();
		}
		return Delay.of(Duration.ofMinutes(10));
	}

	@Override
	public Delay onSuccessRunDelay(TimeOfUsePrices result) {
		if (!result.hasDataInFuture(this.clock)) {
			return Delay.immediate();
		}

		final var now = Instant.now(this.clock);
		final var currentTime = LocalTime.ofInstant(now, this.clock.getZone());

		if (currentTime.isBefore(this.updateTime)) {
			return Delay.of(Duration.between(currentTime, this.updateTime));
		}
		final var lastTime = result.getLastTime();
		final var midnight = now.truncatedTo(DurationUnit.ofDays(1)).plus(1, ChronoUnit.DAYS);
		if (lastTime == null || midnight.isAfter(lastTime)) {
			return Delay.of(Duration.ofMinutes(15));
		}
		return Delay.of(Duration.between(now, midnight.plus(this.updateTime.toNanoOfDay(), ChronoUnit.NANOS)));
	}

}
