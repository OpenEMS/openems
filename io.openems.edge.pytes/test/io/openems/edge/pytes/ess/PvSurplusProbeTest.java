package io.openems.edge.pytes.ess;

import static io.openems.edge.pytes.ess.PvSurplusProbe.AVERAGE_CYCLES;
import static io.openems.edge.pytes.ess.PvSurplusProbe.HOLD_MIN_MS;
import static io.openems.edge.pytes.ess.PvSurplusProbe.MARGIN_W;
import static io.openems.edge.pytes.ess.PvSurplusProbe.PROBE_INTERVAL_MS;
import static io.openems.edge.pytes.ess.PvSurplusProbe.RESERVE_W;
import static io.openems.edge.pytes.ess.PvSurplusProbe.STEP_MAX_W;
import static io.openems.edge.pytes.ess.PvSurplusProbe.STEP_W;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PvSurplusProbeTest {

	private static final int RATED = 15_000;
	private static final int CYCLE = 1000;
	private static final int CYCLES_PER_INTERVAL = PROBE_INTERVAL_MS / CYCLE;
	private static final int CONSUMPTION = 500;

	private final PvSurplusProbe sut = new PvSurplusProbe();

	/**
	 * Simulated plant: the inverter outputs the floor (or the consumption if the
	 * floor is below it), PV follows up to {@code pvAvailable}, the battery
	 * delivers the rest. Battery full: charge limit 0, BMS battery power = gap.
	 */
	private int floor = 0;

	private int cycle(int pvAvailable, Integer feedInLimit, boolean pvLimitActive) {
		int ac = Math.max(CONSUMPTION, this.floor);
		int pv = Math.min(pvAvailable, ac);
		int battery = ac - pv; // positive = discharge
		int grid = CONSUMPTION - ac; // negative = export
		this.floor = this.sut.compute(pv, ac, grid, battery, 0, feedInLimit, RATED, pvLimitActive, CYCLE);
		return this.floor;
	}

	private int cycles(int n, int pvAvailable, Integer feedInLimit) {
		int f = 0;
		for (int i = 0; i < n; i++) {
			f = this.cycle(pvAvailable, feedInLimit, false);
		}
		return f;
	}

	@Test
	public void batteryWithHeadroomIsNotProbed() {
		// battery charges 900 W with a 1800 W limit: the inverter would charge more
		// before curtailing PV, so the measured PV is the available one
		for (int i = 0; i < 3 * CYCLES_PER_INTERVAL; i++) {
			assertEquals(0, this.sut.compute(3000, 2100, -1600, -900, -1800, 5000, RATED, false, CYCLE));
		}
	}

	@Test
	public void idleBatteryIsProbedDespiteBmsAllowance() {
		// 99 % SoC: the BMS still allows 634 W, the inverter does not charge (its
		// own max-charge SoC) and curtails the PV to the 500 W consumption
		int f = 0;
		for (int i = 0; i < AVERAGE_CYCLES; i++) {
			f = this.sut.compute(500, 500, 0, 20, -634, 5000, RATED, false, CYCLE);
		}
		assertEquals(500 + STEP_W, f);
	}

	@Test
	public void fullBatteryRampsUpWhilePvFollows() {
		// PV curtailed to consumption, 6 kW available, 5 kW feed-in limit
		this.cycles(AVERAGE_CYCLES - 1, 6000, 5000);
		assertEquals(0, this.floor); // the gap has to be steady over a full window first
		this.cycles(1, 6000, 5000);
		assertEquals(CONSUMPTION + STEP_W, this.floor);

		// one step per interval, doubled while the PV follows (300, 600, 1200, 1200)
		this.cycles(CYCLES_PER_INTERVAL, 6000, 5000);
		assertEquals(CONSUMPTION + 300 + 600, this.floor);
		this.cycles(CYCLES_PER_INTERVAL, 6000, 5000);
		assertEquals(CONSUMPTION + 300 + 600 + 1200, this.floor);
		this.cycles(CYCLES_PER_INTERVAL, 6000, 5000);
		assertEquals(CONSUMPTION + 300 + 600 + 1200 + STEP_MAX_W, this.floor);

		// ... up to consumption + limit - reserve, never above
		int f = this.cycles(30 * CYCLES_PER_INTERVAL, 6000, 5000);
		assertEquals(CONSUMPTION + 5000 - RESERVE_W, f);
		assertFalse(this.sut.isProbing());
	}

	@Test
	public void failedStepFallsBackAndHolds() {
		// 1000 W available: 500 -> 800 succeeds, the next step is doubled:
		// 800 -> 1400 fails (the battery has to deliver 400 W)
		this.cycles(AVERAGE_CYCLES, 1000, 5000);
		assertEquals(800, this.floor);
		this.cycles(CYCLES_PER_INTERVAL, 1000, 5000);
		assertEquals(1400, this.floor); // step pending
		assertTrue(this.sut.isProbing());

		// after EVAL_MS the gap of 400 W is detected: back to the last good level
		this.cycles(CYCLES_PER_INTERVAL, 1000, 5000);
		assertFalse(this.sut.isProbing());
		assertTrue("floor " + this.floor, this.floor <= 1000 && this.floor >= 800);

		// no new step during the hold time
		int before = this.floor;
		this.cycles(HOLD_MIN_MS / CYCLE - 2 * CYCLES_PER_INTERVAL, 1000, 5000);
		assertFalse(this.sut.isProbing());
		assertTrue(this.floor <= before);

		// then it probes again, with the small step
		this.cycles(2 * CYCLES_PER_INTERVAL, 1000, 5000);
		assertTrue(this.sut.isProbing());
		assertTrue("floor " + this.floor, this.floor <= before + STEP_W);
	}

	@Test
	public void pvDropPullsTheFloorDown() {
		this.cycles(3 * CYCLES_PER_INTERVAL, 6000, 5000);
		int high = this.floor;
		assertTrue(high >= CONSUMPTION + 3 * STEP_W);

		// cloud: only 900 W available -> the battery fills the gap; within a few
		// cycles the floor is back at the measured PV (+ one probe step at most)
		this.cycles(2 * CYCLES_PER_INTERVAL, 900, 5000);
		assertTrue("floor " + this.floor, this.floor <= 900 + STEP_MAX_W);
	}

	@Test
	public void offAtNightAndWhileTheDynamicLimitCurtails() {
		this.cycles(3 * CYCLES_PER_INTERVAL, 6000, 5000);
		assertTrue(this.floor > 0);

		assertEquals(0, this.cycle(6000, 5000, true)); // reg 43052 active
		assertEquals(0, this.sut.compute(20, 40, -20, 0, 0, 5000, RATED, false, CYCLE)); // night
	}

	@Test
	public void boundsWithoutMetaLimitAndWithoutMeter() {
		// no feed-in limit: rated power is the bound
		int f = this.cycles(60 * CYCLES_PER_INTERVAL, 20_000, null);
		assertEquals(RATED, f);

		// no meter: consumption unknown -> limit - reserve
		var probe = new PvSurplusProbe();
		int floor = 0;
		for (int i = 0; i < 20 * CYCLES_PER_INTERVAL; i++) {
			int ac = Math.max(CONSUMPTION, floor);
			floor = probe.compute(Math.min(6000, ac), ac, null, 0, 0, 2000, RATED, false, CYCLE);
		}
		assertEquals(2000 - RESERVE_W, floor);
	}

	@Test
	public void noStepWhileTheAcOutputIsStillSettling() {
		// after a restart the AC output lags the PV by hundreds of watts: the gap
		// swings, so no probe step is taken until it is steady
		var probe = new PvSurplusProbe();
		assertEquals(0, probe.compute(2000, 1200, -700, 0, 0, 5000, RATED, false, CYCLE)); // gap -800
		assertEquals(0, probe.compute(2400, 1800, -1300, 0, 0, 5000, RATED, false, CYCLE)); // gap -600
		assertEquals(0, probe.compute(2900, 2100, -1600, 0, 0, 5000, RATED, false, CYCLE)); // gap -800
		// steady for five cycles -> step
		int f = 0;
		for (int i = 0; i < 5; i++) {
			f = probe.compute(2900, 2800, -2300, 0, 0, 5000, RATED, false, CYCLE);
		}
		assertTrue("floor " + f, f > 2800);
	}

	@Test
	public void firstFailedStepFallsBackToTheDeliveredLevel() {
		// full battery, the inverter delivers 2700 W (= PV) without a floor; the
		// first step to 3000 fails -> the floor holds the 2700, not 0
		var probe = new PvSurplusProbe();
		int f = 0;
		for (int i = 0; i < AVERAGE_CYCLES; i++) {
			f = probe.compute(2700, 2700, -2200, 0, 0, 5000, RATED, false, CYCLE);
		}
		assertEquals(3000, f);
		for (int i = 0; i < CYCLES_PER_INTERVAL; i++) {
			f = probe.compute(2700, 3000, -2500, 300, 0, 5000, RATED, false, CYCLE); // battery fills 300 W
		}
		assertFalse(probe.isProbing());
		assertEquals(2700, f);
	}

	@Test
	public void cloudDuringAStepDoesNotPauseTheSearch() {
		this.cycles(3 * CYCLES_PER_INTERVAL, 6000, 5000);
		assertTrue(this.sut.isProbing() || this.floor > CONSUMPTION + 2 * STEP_W);
		// cloud right in the step: PV drops to 900 W, the step "fails"
		this.cycles(CYCLES_PER_INTERVAL, 900, 5000);
		assertTrue("floor " + this.floor, this.floor <= 900 + STEP_MAX_W);
		// sun is back: the search continues within the next interval, no hold
		this.cycles(2 * CYCLES_PER_INTERVAL, 6000, 5000);
		assertTrue("floor " + this.floor, this.floor >= 900 + STEP_W);
	}

	@Test
	public void marginConstantsAreConsistent() {
		// a failed step has to be detectable: the step must exceed the margin
		assertTrue(STEP_W > MARGIN_W);
	}
}
