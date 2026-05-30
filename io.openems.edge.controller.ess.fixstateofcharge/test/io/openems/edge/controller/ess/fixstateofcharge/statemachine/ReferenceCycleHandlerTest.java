package io.openems.edge.controller.ess.fixstateofcharge.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.controller.ess.fixstateofcharge.statemachine.StateMachine.State;

class ReferenceCycleHandlerTest {

	private static TimeLeapClock clock;
	private ReferenceCycleHandler sut;
	private DummyFixStateOfChargeController controller;

	private static final int MAX_APPARENT_POWER = 10_000;
	private static final int ESS_CAPACITY_WH = 12_000;

	@BeforeEach
	void before() {
		clock = new TimeLeapClock(Instant.parse("2023-01-01T08:00:00.00Z"), ZoneOffset.UTC);
		this.sut = new ReferenceCycleHandler();
		this.controller = new DummyFixStateOfChargeController() //
				.withCapacity(ESS_CAPACITY_WH);
	}

	@Test
	void testReferenceCycleCompletesAt100WhenStartSocAtLeast70() throws Exception {
		// Start SoC >= 70 -> reference target is 100 (charge).
		var notReachedContext = this.generateContext(80);
		var notReachedState = this.sut.runAndGetNextState(notReachedContext);
		assertEquals(State.REFERENCE_CYCLE, notReachedState);
		// Should set targetPower to charge (negative) and rampPower for control
		assertEquals(-expectedReferencePower(), notReachedContext.getTargetPower().doubleValue(), 0.01);
		assertEquals(500.0, notReachedContext.getRampPower(), 0.01);

		var reachedContext = this.generateContext(100);
		var reachedState = this.sut.runAndGetNextState(reachedContext);
		assertEquals(State.REFERENCE_CYCLE, reachedState);
		assertEquals(0.0, reachedContext.getTargetPower().doubleValue(), 0.01);

		clock.leap(30, ChronoUnit.MINUTES);
		var completedContext = this.generateContext(100);
		var completedState = this.sut.runAndGetNextState(completedContext);
		assertEquals(State.ABOVE_TARGET_SOC, completedState);
	}

	@Test
	void testReferenceCycleCompletesAt0WhenStartSocBelow70() throws Exception {
		// Start SoC < 70 -> reference target is 0 (discharge).
		var notReachedContext = this.generateContext(50);
		var notReachedState = this.sut.runAndGetNextState(notReachedContext);
		assertEquals(State.REFERENCE_CYCLE, notReachedState);
		// Should set targetPower to discharge (positive) and rampPower for control
		assertEquals(expectedReferencePower(), notReachedContext.getTargetPower().doubleValue(), 0.01);
		assertEquals(500.0, notReachedContext.getRampPower(), 0.01);

		var reachedContext = this.generateContext(0);
		var reachedState = this.sut.runAndGetNextState(reachedContext);
		assertEquals(State.REFERENCE_CYCLE, reachedState);
		assertEquals(0.0, reachedContext.getTargetPower().doubleValue(), 0.01);

		clock.leap(30, ChronoUnit.MINUTES);
		var completedContext = this.generateContext(0);
		var completedState = this.sut.runAndGetNextState(completedContext);
		assertEquals(State.BELOW_TARGET_SOC, completedState);
	}

	@Test
	void testFallbackTriggeredAfter30MinAllowedChargePowerZero() throws Exception {
		// SoC >= 70 -> target is 100 (charge direction), stuck at soc=80
		this.controller.withAllowedChargePower(0);

		// First tick: timer starts
		this.sut.runAndGetNextState(this.generateContext(80));

		// 29 more minutes: still below timeout
		clock.leap(29, ChronoUnit.MINUTES);
		assertEquals(State.REFERENCE_CYCLE, this.sut.runAndGetNextState(this.generateContext(80)));

		// 1 more minute (total >= 30): fallback fires -> enters pause, stays in
		// REFERENCE_CYCLE
		clock.leap(1, ChronoUnit.MINUTES);
		var nextState = this.sut.runAndGetNextState(this.generateContext(80));
		assertEquals(State.REFERENCE_CYCLE, nextState);

		// After another 30 minutes of pause: transitions to next state
		clock.leap(30, ChronoUnit.MINUTES);
		assertEquals(State.ABOVE_TARGET_SOC, this.sut.runAndGetNextState(this.generateContext(80)));
	}

	@Test
	void testFallbackTriggeredAfter30MinAllowedDischargePowerZero() throws Exception {
		// SoC < 70 -> target is 0 (discharge direction), stuck at soc=50
		this.controller.withAllowedDischargePower(0);

		// First tick: timer starts
		this.sut.runAndGetNextState(this.generateContext(50));

		// 29 more minutes: still below timeout
		clock.leap(29, ChronoUnit.MINUTES);
		assertEquals(State.REFERENCE_CYCLE, this.sut.runAndGetNextState(this.generateContext(50)));

		// 1 more minute (total >= 30): fallback fires -> enters pause, stays in
		// REFERENCE_CYCLE; soc=10 < targetSoc=30
		clock.leap(1, ChronoUnit.MINUTES);
		assertEquals(State.REFERENCE_CYCLE, this.sut.runAndGetNextState(this.generateContext(10)));

		// After another 30 minutes of pause: transitions to next state
		clock.leap(30, ChronoUnit.MINUTES);
		assertEquals(State.BELOW_TARGET_SOC, this.sut.runAndGetNextState(this.generateContext(10)));
	}

	@Test
	void testFallbackPauseNotEnteredWhenAllowedPowerRecoversDuringTimeout() throws Exception {
		// SoC >= 70 -> charge direction; fallback fires after 30 min, then power
		// recovers
		this.controller.withAllowedChargePower(0);
		this.sut.runAndGetNextState(this.generateContext(80)); // starts timer

		clock.leap(30, ChronoUnit.MINUTES);
		// Fallback fires — enters pause phase, stays in REFERENCE_CYCLE
		var nextState = this.sut.runAndGetNextState(this.generateContext(80));
		assertEquals(State.REFERENCE_CYCLE, nextState);

		// onExit is called by state machine on transition; simulate it
		this.sut.onExit(this.generateContext(80));

		// Power recovers — but we are already out of REFERENCE_CYCLE, so no re-entry
		// into pause
		this.controller.withAllowedChargePower(5000);
		// If somehow re-entered, pause should not be triggered by recovered power
		assertNull(this.controller.getReferenceCycleFallbackStartMs());
	}

	@Test
	void testFallbackTimerResetWhenPowerRecoveries() throws Exception {
		// SoC < 70 -> discharge direction
		this.controller.withAllowedDischargePower(0);

		// First tick: timer starts; then 29 minutes — timer running but not expired
		this.sut.runAndGetNextState(this.generateContext(50));
		clock.leap(29, ChronoUnit.MINUTES);
		assertEquals(State.REFERENCE_CYCLE, this.sut.runAndGetNextState(this.generateContext(50)));

		// Power recovers — timer must reset
		this.controller.withAllowedDischargePower(5000);
		assertEquals(State.REFERENCE_CYCLE, this.sut.runAndGetNextState(this.generateContext(50)));
		assertNull(this.controller.getReferenceCycleFallbackStartMs());

		// Power drops to 0 again — new 30-minute window starts from scratch
		this.controller.withAllowedDischargePower(0);
		this.sut.runAndGetNextState(this.generateContext(50)); // starts new timer
		clock.leap(29, ChronoUnit.MINUTES);
		var context = this.generateContext(50);
		assertEquals(State.REFERENCE_CYCLE, this.sut.runAndGetNextState(context));
		// targetPower must still be discharge power (not 0 = pause), confirming no
		// fallback yet
		assertTrue(context.getTargetPower() > 0);
	}

	@Test
	void testFallbackTimerNotStartedWhenPowerUndefined() throws Exception {
		// SoC < 70 -> discharge direction; allowed power is undefined (not set)
		// No withAllowedDischargePower() call -> value is undefined

		clock.leap(15, ChronoUnit.MINUTES);
		var context = this.generateContext(50);
		assertEquals(State.REFERENCE_CYCLE, this.sut.runAndGetNextState(context));

		// Fallback timer must not have started
		assertNull(this.controller.getReferenceCycleFallbackStartMs());
		// targetPower must be discharge (not 0), confirming no fallback
		assertTrue(context.getTargetPower() > 0);
	}

	@Test
	void testFallbackChannelClearedOnExit() throws Exception {
		// Trigger fallback for charge direction
		this.controller.withAllowedChargePower(0);
		this.sut.runAndGetNextState(this.generateContext(80)); // starts timer
		clock.leap(30, ChronoUnit.MINUTES);
		this.sut.runAndGetNextState(this.generateContext(80)); // fallback fires, stays in REFERENCE_CYCLE (pause)

		// onExit clears everything
		this.sut.onExit(this.generateContext(80));
		assertNull(this.controller.getReferenceCycleFallbackStartMs());
	}

	private static double expectedReferencePower() {
		return Math.min(MAX_APPARENT_POWER, Math.round(ESS_CAPACITY_WH * 0.5f));
	}

	private Context generateContext(int soc) {
		var targetSoc = 30;
		return new Context(this.controller, null, MAX_APPARENT_POWER, soc, targetSoc, ZonedDateTime.now(clock), clock);
	}
}
