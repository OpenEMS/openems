package io.openems.edge.evcs.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PhasesTest {

	@Test
	public void calculateLimitsTest() {

		var limit = Evcs.DEFAULT_MAXIMUM_HARDWARE_POWER;
		assertEquals(22_080, Phases.THREE_PHASE.getFromThreePhase(limit));
		assertEquals(14_720, Phases.TWO_PHASE.getFromThreePhase(limit));
		assertEquals(7_360, Phases.ONE_PHASE.getFromThreePhase(limit));

		limit = Evcs.DEFAULT_MINIMUM_HARDWARE_POWER;
		assertEquals(4_140, Phases.THREE_PHASE.getFromThreePhase(limit));
		assertEquals(2_760, Phases.TWO_PHASE.getFromThreePhase(limit));
		assertEquals(1_380, Phases.ONE_PHASE.getFromThreePhase(limit));

		limit = 11_040; // 16A
		assertEquals(11_040, Phases.THREE_PHASE.getFromThreePhase(limit));
		assertEquals(7_360, Phases.TWO_PHASE.getFromThreePhase(limit));
		assertEquals(3_680, Phases.ONE_PHASE.getFromThreePhase(limit));

		limit = 6900; // 10A
		assertEquals(6_900, Phases.THREE_PHASE.getFromThreePhase(limit));
		assertEquals(4_600, Phases.TWO_PHASE.getFromThreePhase(limit));
		assertEquals(2_300, Phases.ONE_PHASE.getFromThreePhase(limit));

		limit = 10_000;
		assertEquals(10_000, Phases.THREE_PHASE.getFromThreePhase(limit));
		assertEquals(6_667, Phases.TWO_PHASE.getFromThreePhase(limit));
		assertEquals(3_333, Phases.ONE_PHASE.getFromThreePhase(limit));
	}
}
