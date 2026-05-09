package io.openems.edge.controller.ess.fixstateofcharge.statemachine;


import static org.junit.jupiter.api.Assertions.assertEquals;

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
		this.controller = new DummyFixStateOfChargeController().withCapacity(ESS_CAPACITY_WH);
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

	private static double expectedReferencePower() {
		return Math.min(MAX_APPARENT_POWER, Math.round(ESS_CAPACITY_WH * 0.5f));
	}

	private Context generateContext(int soc) {
		var targetSoc = 30;
		return new Context(this.controller, null, MAX_APPARENT_POWER, soc, targetSoc, ZonedDateTime.now(clock),
				clock);
	}
}
