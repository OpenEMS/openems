package io.openems.edge.goodwe.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.openems.common.test.TestUtils;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.goodwe.common.enums.MeterCommunicateStatus;

class C50GridModeTest {

	@Test
	void immediateOnGridAndEarlyConfirmationStayStableUntilRawChanges() {
		var read = new CompletableFuture<Integer>();
		var filter = new GridModeStabilityFilter(() -> read);
		assertEquals(GridMode.ON_GRID, filter.applyC50(GridMode.ON_GRID));
		assertEquals(GridMode.ON_GRID, filter.applyC50(GridMode.OFF_GRID));
		read.complete(MeterCommunicateStatus.NG.getValue());
		assertEquals(GridMode.OFF_GRID, filter.applyC50(GridMode.OFF_GRID));
		assertEquals(GridMode.OFF_GRID, filter.applyC50(GridMode.OFF_GRID));
		assertEquals(GridMode.ON_GRID, filter.applyC50(GridMode.ON_GRID));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(ints = { -1, 1, 99 })
	void startupOffGridWaitsThirtySecondsForNonConfirmingRead(Integer status) {
		var clock = TestUtils.createDummyClock();
		var filter = new GridModeStabilityFilter(clock, () -> CompletableFuture.completedFuture(status));
		assertEquals(GridMode.ON_GRID, filter.applyC50(GridMode.OFF_GRID));
		clock.leap(29, ChronoUnit.SECONDS);
		assertEquals(GridMode.ON_GRID, filter.applyC50(GridMode.OFF_GRID));
		clock.leap(1, ChronoUnit.SECONDS);
		assertEquals(GridMode.OFF_GRID, filter.applyC50(GridMode.OFF_GRID));
	}

	@ParameterizedTest
	@EnumSource(value = GridMode.class, names = { "ON_GRID", "OFF_GRID_GENSET", "UNDEFINED" })
	void interruptedCandidateRestartsTimerAndIgnoresLateResults(GridMode interruption) {
		var clock = TestUtils.createDummyClock();
		var reads = new ArrayList<CompletableFuture<Integer>>();
		var filter = new GridModeStabilityFilter(clock, () -> {
			var read = new CompletableFuture<Integer>();
			reads.add(read);
			return read;
		});
		assertEquals(GridMode.ON_GRID, filter.applyC50(GridMode.OFF_GRID));
		clock.leap(29, ChronoUnit.SECONDS);
		assertEquals(interruption, filter.applyC50(interruption));
		assertEquals(GridMode.ON_GRID, filter.applyC50(GridMode.OFF_GRID));
		reads.get(0).complete(MeterCommunicateStatus.NG.getValue());
		clock.leap(1, ChronoUnit.SECONDS);
		assertEquals(GridMode.ON_GRID, filter.applyC50(GridMode.OFF_GRID));
		clock.leap(29, ChronoUnit.SECONDS);
		assertEquals(GridMode.OFF_GRID, filter.applyC50(GridMode.OFF_GRID));
		assertEquals(2, reads.size());
	}

	@Test
	void newerResultIsNotOverwrittenByLateOlderResult() {
		var reads = new ArrayList<CompletableFuture<Integer>>();
		var filter = new GridModeStabilityFilter(() -> {
			var read = new CompletableFuture<Integer>();
			reads.add(read);
			return read;
		});
		assertEquals(GridMode.ON_GRID, filter.applyC50(GridMode.OFF_GRID));
		assertEquals(GridMode.ON_GRID, filter.applyC50(GridMode.ON_GRID));
		assertEquals(GridMode.ON_GRID, filter.applyC50(GridMode.OFF_GRID));
		reads.get(1).complete(MeterCommunicateStatus.NG.getValue());
		reads.get(0).complete(MeterCommunicateStatus.OK.getValue());
		assertEquals(GridMode.OFF_GRID, filter.applyC50(GridMode.OFF_GRID));
	}

	@Test
	void alreadyCompletedReadConfirmsInTheSameEvaluation() {
		var filter = new GridModeStabilityFilter(
				() -> CompletableFuture.completedFuture(MeterCommunicateStatus.NG.getValue()));
		assertEquals(GridMode.OFF_GRID, filter.applyC50(GridMode.OFF_GRID));
		assertEquals(GridMode.OFF_GRID, filter.applyC50(GridMode.OFF_GRID));
	}

	@Test
	void exceptionallyCompletedReadFallsBackToDeadline() {
		var clock = TestUtils.createDummyClock();
		var filter = new GridModeStabilityFilter(clock,
				() -> CompletableFuture.failedFuture(new IllegalStateException("read failed")));
		assertEquals(GridMode.ON_GRID, filter.applyC50(GridMode.OFF_GRID));
		clock.leap(30, ChronoUnit.SECONDS);
		assertEquals(GridMode.OFF_GRID, filter.applyC50(GridMode.OFF_GRID));
	}
}
