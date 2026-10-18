package io.openems.common.tariff.api.http;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.types.HttpStatus;
import io.openems.common.types.TariffData;

public class TariffDelayTimeProvider implements DelayTimeProvider<TariffData> {
	private final Clock clock;
	private final LocalTime updateTime;

	public TariffDelayTimeProvider(Clock clock, LocalTime updateTime) {
		this.clock = clock;
		this.updateTime = updateTime;
	}

	public TariffDelayTimeProvider(Clock clock) {
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
	public Delay onSuccessRunDelay(TariffData result) {
		if (result == null) {
			// Mapping failed
			return Delay.of(Duration.ofMinutes((10)));
		}

		if (!result.prices().hasDataInFuture(this.clock)) {
			return Delay.of(Duration.ofMinutes(10));
		}

		final var now = Instant.now(this.clock);
		final var currentTime = LocalTime.ofInstant(now, this.clock.getZone());
		if (currentTime.isBefore(this.updateTime)) {
			return Delay.of(Duration.between(currentTime, this.updateTime));
		}

		final var lastTime = result.prices().getLastTime();
		final var midnight = now.truncatedTo(DurationUnit.ofDays(1)).plus(1, ChronoUnit.DAYS);
		if (lastTime == null || midnight.isAfter(lastTime)) {
			return Delay.of(Duration.ofMinutes(15));
		}

		return Delay.of(Duration.between(now, midnight.plus(this.updateTime.toNanoOfDay(), ChronoUnit.NANOS)));
	}

}
