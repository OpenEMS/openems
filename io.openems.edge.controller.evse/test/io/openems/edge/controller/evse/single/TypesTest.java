package io.openems.edge.controller.evse.single;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.controller.evse.single.Types.History.allActivePowersAreZero;
import static io.openems.edge.controller.evse.single.Types.History.allReadyForCharging;
import static io.openems.edge.controller.evse.single.Types.History.allSetPointsAreZero;
import static io.openems.edge.controller.evse.single.Types.History.noSetPointsAreZero;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.stream.IntStream;

import org.junit.Test;

import io.openems.edge.controller.evse.single.Types.History;
import io.openems.edge.controller.evse.single.Types.Hysteresis;

public class TypesTest {

	@Test
	public void testHistory() {
		var now = Instant.now(createDummyClock());
		var h = new History();
		assertEquals(0, h.streamAll().count());
		assertEquals(0, h.streamAllButLast().count());

		IntStream.range(0, 400) //
				.forEach(i -> {
					h.addEntry(now.plusSeconds(i), 0, i, null, true);

					switch (i) {
					// Test streamAllButLast() vs streamAll()
					case 49 -> {
						h.addEntry(now.plusSeconds(i), 999, 0, null, false);
						assertEquals(999, h.getLastEntry().getValue().activePower().intValue());

						assertTrue(allActivePowersAreZero(h.streamAllButLast()));
						assertFalse(allActivePowersAreZero(h.streamAll()));

						assertFalse(allSetPointsAreZero(h.streamAllButLast()));
						assertFalse(noSetPointsAreZero(h.streamAll()));

						assertTrue(allReadyForCharging(h.streamAllButLast()));
						assertFalse(allReadyForCharging(h.streamAll()));
					}
					case 50 -> {
						assertEquals(50, h.streamAllButLast().count());
						assertEquals(51, h.streamAll().count());
					}

					// Test isEntriesAreFullyInitialized()
					case 300 -> assertFalse(h.isEntriesAreFullyInitialized());
					case 301 -> assertTrue(h.isEntriesAreFullyInitialized());
					}
				});
		assertEquals(301, h.streamAll().count());
	}

	@Test
	public void testHysteresis() {
		var now = Instant.now(createDummyClock());
		var h = new History();
		h.addEntry(now.minusSeconds(310), null, 7000, null, true);
		h.addEntry(now.minusSeconds(300), null, 8000, null, true);
		h.addEntry(now.minusSeconds(290), null, 9000, null, true);
		assertEquals(Hysteresis.INACTIVE, Hysteresis.from(h));
	}

	@Test
	public void testHysteresis2() {
		var now = Instant.now(createDummyClock());
		var h = new History();
		h.addEntry(now.minusSeconds(310), null, 7000, null, true);
		h.addEntry(now.minusSeconds(300), null, 8000, null, false);
		assertEquals(Hysteresis.KEEP_CHARGING, Hysteresis.from(h));
	}

	@Test
	public void testAutomaticPhaseSwitchRequiresMinimumSampleCount() {
		var now = Instant.now(createDummyClock());
		var h = new History();

		for (int i = 0; i < 59; i++) {
			var evaluation = h.evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitation(now.plusSeconds(i), 5_000,
					4_100, History.AutomaticPhaseSwitchThresholdDirection.ABOVE);
			assertFalse(evaluation.shouldSwitch());
			assertEquals(i + 1, evaluation.sampleCount());
			h.addEntry(now.plusSeconds(i), 1_000, 1_000, 5_000, true);
		}

		var sixtiethEvaluation = h.evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitation(now.plusSeconds(59),
				5_000, 4_100, History.AutomaticPhaseSwitchThresholdDirection.ABOVE);
		assertTrue(sixtiethEvaluation.shouldSwitch());
		assertEquals(60, sixtiethEvaluation.sampleCount());
	}

}
