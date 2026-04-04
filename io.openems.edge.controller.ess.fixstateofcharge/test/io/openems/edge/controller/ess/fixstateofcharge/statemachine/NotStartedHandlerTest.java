package io.openems.edge.controller.ess.fixstateofcharge.statemachine;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.controller.ess.fixstateofcharge.api.AbstractFixStateOfCharge;
import io.openems.edge.controller.ess.fixstateofcharge.api.ConfigProperties;
import io.openems.edge.controller.ess.fixstateofcharge.api.EndCondition;
import io.openems.edge.controller.ess.fixstateofcharge.statemachine.StateMachine.State;

public class NotStartedHandlerTest {

	private static TimeLeapClock clock;
	private NotStartedHandler sut;
	private DummyFixStateOfChargeController controller;
	private ConfigProperties config;

	private static final int MAX_APPARENT_POWER = 10_000;
	private static final int ESS_CAPACITY_WH = 12_000;
	private static final int TARGET_TIME_BUFFER_MIN = 5;
	private static final int SOC_START = 20; // Below target (30) by 10%, requires ~3 min charging
	private static final int SOC_TARGET = 30;

	@BeforeEach
	void beforeEach() {
		clock = new TimeLeapClock(Instant.parse("2023-01-01T08:00:00.00Z"), ZoneOffset.UTC);
		this.sut = new NotStartedHandler();
		this.controller = new DummyFixStateOfChargeController()
				.withCapacity(ESS_CAPACITY_WH); //
		this.config = new ConfigProperties(true, SOC_TARGET, true, "2023-01-01T09:00:00+00:00", TARGET_TIME_BUFFER_MIN,
				false, 0, false, EndCondition.CAPACITY_CHANGED);
	}

	/**
	 * Test A1: Immediate start when considerTargetTime() == false with
	 * referenceCycleEnabled == true -> REFERENCE_CYCLE.
	 */
	@Test
	public void testImmediateStartNoTargetTimeWithReferenceCycleEnabled() throws Exception {
		this.controller.withReferenceCycle();
		var configNoTargetTime = new ConfigProperties(true, SOC_TARGET, false, null, TARGET_TIME_BUFFER_MIN, false, 0,
				false, EndCondition.CAPACITY_CHANGED);
		var context = new Context(this.controller, configNoTargetTime, MAX_APPARENT_POWER, SOC_START, SOC_TARGET,
				null, clock);

		var nextState = this.sut.runAndGetNextState(context);
		assertEquals(State.REFERENCE_CYCLE, nextState);
	}

	/**
	 * Test A2: Immediate start when considerTargetTime() == false with
	 * referenceCycleEnabled == false -> getSocState(soc, targetSoc).
	 */
	@Test
	public void testImmediateStartNoTargetTimeWithoutReferenceCycle() throws Exception {
		var configNoTargetTime = new ConfigProperties(true, SOC_TARGET, false, null, TARGET_TIME_BUFFER_MIN, false, 0,
				false, EndCondition.CAPACITY_CHANGED);
		var context = new Context(this.controller, configNoTargetTime, MAX_APPARENT_POWER, SOC_START, SOC_TARGET,
				null, clock);

		var nextState = this.sut.runAndGetNextState(context);
		// SOC_START (20) < SOC_TARGET-2 (28) -> BELOW_TARGET_SOC
		assertEquals(State.BELOW_TARGET_SOC, nextState);
	}

	/**
	 * Test A3: Immediate start when passedTargetTime() == true with
	 * referenceCycleEnabled == true -> REFERENCE_CYCLE.
	 */
	@Test
	public void testImmediateStartPassedTargetTimeWithReferenceCycleEnabled() throws Exception {
		this.controller.withReferenceCycle();
		// Target time is in the past (1 hour ago from clock now)
		var targetTime = ZonedDateTime.now(clock).minusHours(1);
		var context = new Context(this.controller, this.config, MAX_APPARENT_POWER, SOC_START, SOC_TARGET, targetTime,
				clock);

		var nextState = this.sut.runAndGetNextState(context);
		assertEquals(State.REFERENCE_CYCLE, nextState);
	}

	/**
	 * Test A4: Immediate start when passedTargetTime() == true with
	 * referenceCycleEnabled == false -> getSocState(soc, targetSoc).
	 */
	@Test
	public void testImmediateStartPassedTargetTimeWithoutReferenceCycle() throws Exception {
		// Target time is in the past (1 hour ago from clock now)
		var targetTime = ZonedDateTime.now(clock).minusHours(1);
		var context = new Context(this.controller, this.config, MAX_APPARENT_POWER, SOC_START, SOC_TARGET, targetTime,
				clock);

		var nextState = this.sut.runAndGetNextState(context);
		// SOC_START (20) < SOC_TARGET-2 (28) -> BELOW_TARGET_SOC
		assertEquals(State.BELOW_TARGET_SOC, nextState);
	}

	/**
	 * Test B: Planned start - start time not reached. Expect to stay in NOT_STARTED
	 * and set expectedStartEpochSeconds (without asserting exact value, which
	 * depends on internal calculation).
	 */
	@Test
	public void testPlannedStartNotReachedYet() throws Exception {
		// Target time is 2 hours in the future
		var targetTime = ZonedDateTime.now(clock).plusHours(2);
		var context = new Context(this.controller, this.config, MAX_APPARENT_POWER, SOC_START, SOC_TARGET, targetTime,
				clock);

		var nextState = this.sut.runAndGetNextState(context);
		assertEquals(State.NOT_STARTED, nextState);

		// expectedStartEpochSeconds should be set (non-null)
		var expectedStartEpochSeconds = this.controller.getExpectedStartEpochSecondsValue();
		assertNotNull(expectedStartEpochSeconds);
	}

	/**
	 * Test C1: Planned start - start time reached with referenceCycleEnabled ==
	 * true -> REFERENCE_CYCLE and clear expectedStartEpochSeconds.
	 */
	@Test
	public void testPlannedStartReachedWithReferenceCycleEnabled() throws Exception {
		this.controller.withReferenceCycle();
		// Target time is 2 hours in the future to ensure start time is in the future
		var targetTime = ZonedDateTime.now(clock).plusHours(2);
		var context = new Context(this.controller, this.config, MAX_APPARENT_POWER, SOC_START, SOC_TARGET, targetTime,
				clock);

		// First run - should stay in NOT_STARTED
		var firstState = this.sut.runAndGetNextState(context);
		assertEquals(State.NOT_STARTED, firstState);

		// Leap time forward by 2 hours (past any calculated start time)
		clock.leap(2, ChronoUnit.HOURS);

		// Second run - should now transition to REFERENCE_CYCLE
		var context2 = new Context(this.controller, this.config, MAX_APPARENT_POWER, SOC_START, SOC_TARGET,
				targetTime, clock);
		var secondState = this.sut.runAndGetNextState(context2);
		assertEquals(State.REFERENCE_CYCLE, secondState);

		// expectedStartEpochSeconds should be cleared (null)
		assertNull(this.controller.getExpectedStartEpochSecondsValue());
	}

	/**
	 * Test C2: Planned start - start time reached with referenceCycleEnabled ==
	 * false -> getSocState(soc, targetSoc) and clear expectedStartEpochSeconds.
	 */
	@Test
	public void testPlannedStartReachedWithoutReferenceCycle() throws Exception {
		// Target time is 1 day in the future to ensure start time is in the future
		var targetTime = ZonedDateTime.now(clock).plusDays(1);
		var context = new Context(this.controller, this.config, MAX_APPARENT_POWER, SOC_START, SOC_TARGET, targetTime,
				clock);

		// First run - should stay in NOT_STARTED (start time is in future)
		var firstState = this.sut.runAndGetNextState(context);
		assertEquals(State.NOT_STARTED, firstState);

		// Leap time forward by 1 day (definitely past any calculated start time)
		clock.leap(1, ChronoUnit.DAYS);

		// Second run - should now transition to SoC state (start time passed)
		var context2 = new Context(this.controller, this.config, MAX_APPARENT_POWER, SOC_START, SOC_TARGET,
				targetTime, clock);
		var secondState = this.sut.runAndGetNextState(context2);
		// SOC_START (20) < SOC_TARGET-2 (28) -> BELOW_TARGET_SOC
		assertEquals(State.BELOW_TARGET_SOC, secondState);

		// expectedStartEpochSeconds should be cleared (null)
		assertNull(this.controller.getExpectedStartEpochSecondsValue());
	}

	/**
	 * Test D: Reference cycle enabled - start time calculation with cycle duration.
	 *
	 * <p>
	 * When referenceCycleEnabled=true in planned mode, calculateStartTime() must
	 * include extra time for the reference cycle: - Time to reach reference target
	 * (0 or 100 SoC) - 30 minute pause - Time to reach actual target SoC
	 *
	 * <p>
	 * This test verifies the calculation: expectedStartEpochSeconds = targetTime -
	 * (refCycleRequiredSeconds + bufferSeconds)
	 *
	 * <p>
	 * Uses fixed, deterministic values to allow exact assertion without mock clock
	 * manipulation.
	 */
	@Test
	public void testReferenceCycleEnabledIncludesCycleDuration() throws Exception {
		// Setup: Use deterministic values
		var fixedCapacityWh = 8_800; // ESS capacity
		var fixedMaxApparentPower = 10_000; // High enough so ref power isn't clamped
		var fixedSocStart = 80; // Start at 80%
		var fixedTargetSoc = 30; // Target at 30%
		var fixedTargetTime = ZonedDateTime.now(clock).plusDays(2);

		// Create config with 0 buffer for simplicity
		var configZeroBuffer = new ConfigProperties(true, fixedTargetSoc, true,
				fixedTargetTime.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME), 0,
				false, 0, false, EndCondition.CAPACITY_CHANGED);

		this.controller.withReferenceCycle();
		var context = new Context(this.controller, configZeroBuffer, fixedMaxApparentPower, fixedSocStart,
				fixedTargetSoc, fixedTargetTime, clock);

		// Calculate expected start time manually (what production code should compute)
		var refTarget = fixedSocStart >= 70 ? 100 : 0; // Reference cycle target: 100% (since 80>=70)
		var refPowerW = Math.min(fixedMaxApparentPower, Math.round(fixedCapacityWh * 0.5f)); // 0.5C
		var powerToTargetW = context.getTimeEstimationPowerW(fixedCapacityWh);

		var requiredToRefTarget = AbstractFixStateOfCharge.calculateRequiredTime(fixedSocStart, refTarget,
				fixedCapacityWh, refPowerW, clock);
		var pauseDurationSeconds = 30 * 60; // 30 minute pause
		var requiredFromRefToTarget = AbstractFixStateOfCharge.calculateRequiredTime(refTarget, fixedTargetSoc,
				fixedCapacityWh, powerToTargetW, clock);

		var totalRefCycleRequiredSeconds = requiredToRefTarget + pauseDurationSeconds + requiredFromRefToTarget;
		var bufferSeconds = 0; // Using 0 buffer for simplicity
		var expectedStartTime = fixedTargetTime.minusSeconds(totalRefCycleRequiredSeconds + bufferSeconds);

		// Execute
		this.sut.runAndGetNextState(context);

		// Assert: expectedStartEpochSeconds must exactly match the calculated start
		// time
		var actualStartEpochSeconds = this.controller.getExpectedStartEpochSecondsValue();
		assertNotNull(actualStartEpochSeconds, "Start time should be set");
		assertEquals(expectedStartTime.toEpochSecond(), actualStartEpochSeconds.longValue(), "Start time must include full reference cycle duration");
	}
}
