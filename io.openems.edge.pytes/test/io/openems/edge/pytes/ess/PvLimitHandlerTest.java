package io.openems.edge.pytes.ess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PvLimitHandlerTest {

	private static final int RATED = 15_000;
	private static final int LIMIT = 400;
	private static final int CYCLE = 1000;

	private final PvLimitHandler sut = new PvLimitHandler();

	private PvLimitHandler.Result run(Integer grid, Integer ess, Integer limit, int cycles) {
		PvLimitHandler.Result r = null;
		for (int i = 0; i < cycles; i++) {
			r = this.sut.compute(grid, ess, limit, RATED, CYCLE);
		}
		return r;
	}

	// runs cycles until a register write happens (or the cycles are used up)
	private Integer runUntilWrite(Integer grid, Integer ess, Integer limit, int maxCycles) {
		for (int i = 0; i < maxCycles; i++) {
			var r = this.sut.compute(grid, ess, limit, RATED, CYCLE);
			if (r.writePercent() != null) {
				return r.writePercent();
			}
		}
		return null;
	}

	@Test
	public void noLimitWithoutMetaLimit() {
		var r = this.run(-3000, 5000, null, 3);
		assertFalse(r.limiting());
		assertNull(r.acLimitW());
		assertEquals(100, r.percent());
	}

	@Test
	public void writes100PercentOnceAtStart() {
		// restores the register after a restart (or a leftover test value)
		var r = this.run(0, 0, LIMIT, 1);
		assertEquals(Integer.valueOf(100), r.writePercent());
		r = this.run(0, 0, LIMIT, 1);
		assertNull(r.writePercent());
	}

	@Test
	public void staysReleasedBelowLimit() {
		this.run(0, 0, LIMIT, 1);
		var r = this.run(-300, 2000, LIMIT, 10);
		assertFalse(r.limiting());
		assertNull(r.writePercent());
	}

	@Test
	public void capsOutputWhenExportExceedsLimit() {
		this.run(0, 0, LIMIT, 1);
		// consumption 2000 W: PV 5000 W -> ess 5000, export 3000
		var r = this.run(-3000, 5000, LIMIT, 10);
		assertTrue(r.limiting());
		// tolerance = min(500, 400 / 2) = 200: cap = 2000 + 400 - 200 = 2200 W
		// -> ceil(14.67 %) = 15 %
		assertEquals(Integer.valueOf(2200), r.acLimitW());
		assertEquals(15, r.percent());
	}

	@Test
	public void rateLimitsRegisterWrites() {
		this.run(0, 0, LIMIT, 1);
		// first cap is written immediately (MIN_WRITE_MS since the 100 % write
		// has passed after 10 cycles)
		var r = this.run(-3000, 5000, LIMIT, 10);
		assertEquals(Integer.valueOf(15), r.writePercent());
		// inverter follows the cap: ess 2200, grid -200 -> cap unchanged
		r = this.run(-200, 2200, LIMIT, 5);
		assertTrue(r.limiting());
		assertNull(r.writePercent());
		// consumption rises by 1500 W: new percentage, but the write waits
		// for MIN_WRITE_MS (10 s after the 15 % write, 5 s have passed)
		r = this.run(1300, 2200, LIMIT, 3);
		assertTrue(r.percent() > 15);
		assertNull(r.writePercent());
		// cap = 3500 + 400 - 200 = 3700 W -> 25 %
		assertEquals(Integer.valueOf(25), this.runUntilWrite(1300, 2200, LIMIT, 8));
	}

	@Test
	public void followsConsumptionWhileLimiting() {
		this.run(0, 0, LIMIT, 1);
		this.run(-3000, 5000, LIMIT, 10);
		// consumption doubles -> cap rises so the house is still supplied by PV
		// cap = 4000 + 400 - 200 = 4200 W -> 28 %
		assertEquals(Integer.valueOf(28), this.runUntilWrite(1800, 2200, LIMIT, 20));
		// the inverter follows within the settle time: stays limited
		var r = this.run(-200, 4200, LIMIT, 20);
		assertTrue(r.limiting());
		assertEquals(Integer.valueOf(4200), r.acLimitW());
		assertEquals(28, r.percent());
	}

	@Test
	public void releasesWhenCapNotBindingAnymore() {
		this.run(0, 0, LIMIT, 1);
		this.run(-3000, 5000, LIMIT, 10);
		// PV drops: the inverter outputs clearly less than the cap allows
		var r = this.run(1000, 800, LIMIT, 10);
		assertFalse(r.limiting());
		assertEquals(100, r.percent());
		// the release was written on the way
		r = this.run(1000, 800, LIMIT, 1);
		assertNull(r.writePercent());
	}

	@Test
	public void releaseIsNeverDelayed() {
		this.run(0, 0, LIMIT, 1);
		this.run(-3000, 5000, LIMIT, 10);
		// meter drops out one cycle after the cap was written
		var r = this.run(null, 5000, LIMIT, 1);
		assertFalse(r.limiting());
		assertEquals(Integer.valueOf(100), r.writePercent());
	}

	@Test
	public void releasesWhenPvCannotFollowRaisedCap() {
		this.run(0, 0, LIMIT, 1);
		this.run(-3000, 5000, LIMIT, 10);
		// consumption rises, cap is raised, but PV stays at 2200 W: after the
		// settle time the cap is not binding -> released; no export follows
		assertEquals(Integer.valueOf(28), this.runUntilWrite(1800, 2200, LIMIT, 20));
		var r = this.run(1800, 2200, LIMIT, 9);
		assertTrue(r.limiting());
		r = this.run(1800, 2200, LIMIT, 1);
		assertFalse(r.limiting());
		assertEquals(Integer.valueOf(100), r.writePercent());
	}

	@Test
	public void smallLimitNeverCapsBelowTheConsumption() {
		// 30 W house consumption, 400 W limit: a fixed 500 W tolerance would cap
		// the output at 0 W (house from the grid, never released)
		this.run(0, 0, LIMIT, 1);
		var r = this.run(-450, 480, LIMIT, 10);
		assertTrue(r.limiting());
		assertEquals(Integer.valueOf(30 + LIMIT - 200), r.acLimitW());
		assertEquals(2, r.percent());

		// 100 W limit: tolerance 50 -> cap 80 W, still above the consumption
		var small = new PvLimitHandler();
		PvLimitHandler.Result s = null;
		for (int i = 0; i < 10; i++) {
			s = small.compute(-150, 180, 100, RATED, CYCLE);
		}
		assertEquals(Integer.valueOf(80), s.acLimitW());
		assertEquals(50, PvLimitHandler.tolerance(100));
		assertEquals(500, PvLimitHandler.tolerance(5000));
	}

	@Test
	public void releasesWhenCapWouldExceedRatedPower() {
		this.run(0, 0, LIMIT, 1);
		this.run(-3000, 5000, LIMIT, 10);
		var r = this.run(14_000, 1900, LIMIT, 20);
		assertFalse(r.limiting());
		assertEquals(100, r.percent());
	}

	@Test
	public void averagesOverFiveCycles() {
		this.run(0, 0, LIMIT, 1);
		this.run(-100, 2000, LIMIT, 5);
		// a single export spike does not trigger the limitation
		var r = this.run(-1500, 3400, LIMIT, 1);
		assertFalse(r.limiting());
		r = this.run(-1500, 3400, LIMIT, 2);
		assertTrue(r.limiting());
	}

	@Test
	public void handlesMissingValues() {
		var r = this.run(-3000, null, LIMIT, 3);
		assertFalse(r.limiting());
		r = this.run(-3000, 5000, LIMIT, 0);
		assertNull(r);
		r = this.sut.compute(-3000, 5000, LIMIT, 0, CYCLE);
		assertFalse(r.limiting());
	}
}
