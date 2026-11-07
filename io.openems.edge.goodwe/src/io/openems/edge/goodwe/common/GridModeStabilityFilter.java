package io.openems.edge.goodwe.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import io.openems.edge.common.sum.GridMode;
import io.openems.edge.goodwe.common.enums.MeterCommunicateStatus;

/**
 * Filters unconfirmed {@link GridMode#OFF_GRID} transitions reported by the
 * GoodWe grid-mode register.
 *
 * <p>
 * C50 inverters occasionally report short false {@code OFF_GRID} values, which
 * would otherwise be published and counted as outage time. An unconfirmed
 * {@code OFF_GRID} is therefore published only after the stability period has
 * elapsed without another grid mode being received. All other grid modes pass
 * through immediately and reset a pending {@code OFF_GRID} transition.
 *
 * <p>
 * A transition confirmed by the meter communication status may bypass the
 * stability period.
 *
 */
final class GridModeStabilityFilter {

	private static final Duration OFF_GRID_STABILITY_DURATION = Duration.ofSeconds(30);

	private final Clock clock;
	private final Supplier<CompletableFuture<Integer>> readMeterCommunicateStatus;

	private GridMode lastOutput = GridMode.UNDEFINED;
	private Instant pendingOffGridSince = null;
	private AdditionalRegistersState additionalRegistersState;

	GridModeStabilityFilter(Supplier<CompletableFuture<Integer>> readMeterCommunicateStatus) {
		this(Clock.systemDefaultZone(), readMeterCommunicateStatus);
	}

	GridModeStabilityFilter(Clock clock, Supplier<CompletableFuture<Integer>> readMeterCommunicateStatus) {
		this.clock = clock;
		this.readMeterCommunicateStatus = readMeterCommunicateStatus;
	}

	GridMode applyC50(GridMode rawGridMode) {
		if (rawGridMode == null) {
			return this.passThrough(GridMode.UNDEFINED);
		}
		if (rawGridMode == GridMode.OFF_GRID
				&& (this.lastOutput == GridMode.UNDEFINED || this.lastOutput == GridMode.OFF_GRID_GENSET)) {
			this.lastOutput = GridMode.ON_GRID;
		}
		return switch (rawGridMode) {
		case ON_GRID, OFF_GRID_GENSET, UNDEFINED -> this.passThrough(rawGridMode);
		case OFF_GRID -> this.applyOffGridStabilityFilter();
		};
	}

	private GridMode confirmOffGridImmediately() {
		this.resetPendingOffGridCandidate();
		this.lastOutput = GridMode.OFF_GRID;
		return this.lastOutput;
	}

	private GridMode passThrough(GridMode gridMode) {
		this.resetPendingOffGridCandidate();
		this.lastOutput = gridMode;
		return this.lastOutput;
	}

	private GridMode applyOffGridStabilityFilter() {
		if (this.lastOutput == GridMode.OFF_GRID) {
			return this.lastOutput;
		}

		if (this.pendingOffGridSince == null) {
			this.pendingOffGridSince = this.clock.instant();
			this.additionalRegistersState = new AdditionalRegistersState(this.readMeterCommunicateStatus);
		}

		if (Objects.equals(this.additionalRegistersState.meterCommunicateStatus, MeterCommunicateStatus.NG.getValue())
				|| Duration.between(this.pendingOffGridSince, this.clock.instant())
				.compareTo(OFF_GRID_STABILITY_DURATION) >= 0) {
			return this.confirmOffGridImmediately();
		}
		return this.lastOutput;
	}

	private void resetPendingOffGridCandidate() {
		this.pendingOffGridSince = null;
		this.additionalRegistersState = null;
	}

	private static class AdditionalRegistersState {
		private volatile Integer meterCommunicateStatus;

		private AdditionalRegistersState(Supplier<CompletableFuture<Integer>> readMeterCommunicateStatus) {
			readMeterCommunicateStatus.get().thenAccept(status -> this.meterCommunicateStatus = status);
		}
	}
}
