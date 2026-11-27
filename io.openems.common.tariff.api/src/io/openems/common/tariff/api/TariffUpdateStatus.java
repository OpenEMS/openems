package io.openems.common.tariff.api;

import java.time.Instant;

import io.openems.common.types.HttpStatus;

public sealed interface TariffUpdateStatus {
	/**
	 * Gets the point in time when this update status was created.
	 *
	 * @return the update status timestamp
	 */
	Instant time();

	record Successful(Instant time) implements TariffUpdateStatus {
	}

	record FailedWithHttpError(Instant time, HttpStatus statusCode, String body) implements TariffUpdateStatus {
	}

	record FailedWithException(Instant time, Throwable exception) implements TariffUpdateStatus {
	}

	record NotFetchedYet(Instant time) implements TariffUpdateStatus {
	}
}
