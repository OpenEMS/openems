package io.openems.edge.evcs.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestStatic {

	@Test
	public void testPrefferedPhaseBehaviour() {
		assertEquals(Phases.ONE_PHASE, Phases.preferredPhaseBehavior(200, Phases.THREE_PHASE, 6000, 32000));
		assertEquals(Phases.ONE_PHASE, Phases.preferredPhaseBehavior(0, Phases.TWO_PHASE, 6000, 16000));
		assertEquals(Phases.ONE_PHASE, Phases.preferredPhaseBehavior(4300, Phases.THREE_PHASE, 10000 /* zoe */, 32000));
		assertEquals(Phases.ONE_PHASE, Phases.preferredPhaseBehavior(3000, Phases.ONE_PHASE, 6000, 16000));

		assertEquals(Phases.ONE_PHASE, Phases.preferredPhaseBehavior(6000, Phases.ONE_PHASE, 6000, 32000));
		assertEquals(Phases.THREE_PHASE, Phases.preferredPhaseBehavior(6000, Phases.ONE_PHASE, 6000, 16000 /* lower maximum*/));

		assertEquals(Phases.THREE_PHASE, Phases.preferredPhaseBehavior(11000, Phases.ONE_PHASE, 6000, 32000));
		assertEquals(Phases.THREE_PHASE, Phases.preferredPhaseBehavior(11000, Phases.THREE_PHASE, 6000, 32000));
		assertEquals(Phases.THREE_PHASE, Phases.preferredPhaseBehavior(4300, Phases.THREE_PHASE, 6000, 16000));
		
		assertEquals(Phases.TWO_PHASE, Phases.preferredPhaseBehavior(4300, Phases.TWO_PHASE, 6000, 16000));
		assertEquals(Phases.ONE_PHASE, Phases.preferredPhaseBehavior(300, Phases.TWO_PHASE, 6000, 16000));
		assertEquals(Phases.ONE_PHASE, Phases.preferredPhaseBehavior(1800, Phases.TWO_PHASE, 6000, 16000));
		assertEquals(Phases.THREE_PHASE, Phases.preferredPhaseBehavior(8000, Phases.TWO_PHASE, 6000, 16000));
	}
}
