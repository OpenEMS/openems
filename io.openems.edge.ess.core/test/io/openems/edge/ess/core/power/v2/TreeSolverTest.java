package io.openems.edge.ess.core.power.v2;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyMetaEss;

public class TreeSolverTest {

	private static DummyManagedSymmetricEss ess(String id) {
		return new DummyManagedSymmetricEss(id) //
				.withSoc(50) //
				.withAllowedChargePower(-10000) //
				.withAllowedDischargePower(10000) //
				.withMaxApparentPower(10000);
	}

	private static int activePower(PowerDistribution pd, String essId) {
		return ((PowerDistribution.Entry.Actual) pd.getEntries().get(essId)).activePowerSetPoint;
	}

	/**
	 * Flat cluster, equal capacity. target = -10000W → each gets -2500W.
	 */
	@Test
	public void testProportionalDistribution() {
		var ess1 = ess("ess1");
		var ess2 = ess("ess2");
		var ess3 = ess("ess3");
		var ess4 = ess("ess4");
		var ess0 = new DummyMetaEss("ess0", ess1, ess2, ess3, ess4);

		var pd = PowerDistribution.from(List.of(ess0, ess1, ess2, ess3, ess4));
		pd.setEquals("ess0", -10000);

		TreeSolver.solve(pd.getEntries());

		assertEquals(-2500, activePower(pd, "ess1"));
		assertEquals(-2500, activePower(pd, "ess2"));
		assertEquals(-2500, activePower(pd, "ess3"));
		assertEquals(-2500, activePower(pd, "ess4"));
	}

	/**
	 * Pinned entry (EQUALS constraint → ownMin==ownMax). ess1 pinned at +5000W,
	 * remaining = -10000 - 5000 = -15000W distributed to ess2/3/4.
	 */
	@Test
	public void testPinnedEntryTwoPass() {
		var ess1 = ess("ess1");
		var ess2 = ess("ess2");
		var ess3 = ess("ess3");
		var ess4 = ess("ess4");
		var ess0 = new DummyMetaEss("ess0", ess1, ess2, ess3, ess4);

		var pd = PowerDistribution.from(List.of(ess0, ess1, ess2, ess3, ess4));
		pd.setEquals("ess0", -10000);
		pd.setEquals("ess1", 5000); // pinned — force discharge

		TreeSolver.solve(pd.getEntries());

		// ess1 pinned at +5000, remaining = -15000 across ess2/3/4 (each -5000)
		assertEquals(5000, activePower(pd, "ess1"));
		assertEquals(-5000, activePower(pd, "ess2"));
		assertEquals(-5000, activePower(pd, "ess3"));
		assertEquals(-5000, activePower(pd, "ess4"));
	}

	/**
	 * Infeasible: cluster wants -10000W but ess1/2/3 force-discharge +2000W each.
	 * ownActiveMin = 2000+2000+2000-10000 = -4000 → target clamped to -4000W.
	 */
	@Test
	public void testInfeasibleTargetClamped() {
		var ess1 = ess("ess1");
		var ess2 = ess("ess2");
		var ess3 = ess("ess3");
		var ess4 = ess("ess4");
		var ess0 = new DummyMetaEss("ess0", ess1, ess2, ess3, ess4);

		var pd = PowerDistribution.from(List.of(ess0, ess1, ess2, ess3, ess4));
		pd.setEquals("ess0", -10000);
		pd.setGreaterOrEquals("ess1", 2000);
		pd.setGreaterOrEquals("ess2", 2000);
		pd.setGreaterOrEquals("ess3", 2000);

		TreeSolver.solve(pd.getEntries());

		// best effort: ess1/2/3 pinned at +2000, ess4 at -10000, net = -4000W
		assertEquals(2000, activePower(pd, "ess1"));
		assertEquals(2000, activePower(pd, "ess2"));
		assertEquals(2000, activePower(pd, "ess3"));
		assertEquals(-10000, activePower(pd, "ess4"));
	}

	/**
	 * Nested cluster: ess0=[sub1, sub2], sub1=[ess1..ess4], sub2=[ess5..ess8].
	 * sub1 pinned at +8000W, remaining = -10000-8000 = -18000W to sub2.
	 */
	@Test
	public void testNestedClusterWithPinnedSubCluster() {
		var ess1 = ess("ess1");
		var ess2 = ess("ess2");
		var ess3 = ess("ess3");
		var ess4 = ess("ess4");
		var ess5 = ess("ess5");
		var ess6 = ess("ess6");
		var ess7 = ess("ess7");
		var ess8 = ess("ess8");
		var sub1 = new DummyMetaEss("sub1", ess1, ess2, ess3, ess4);
		var sub2 = new DummyMetaEss("sub2", ess5, ess6, ess7, ess8);
		var ess0 = new DummyMetaEss("ess0", sub1, sub2);

		var pd = PowerDistribution.from(
				List.of(ess0, sub1, sub2, ess1, ess2, ess3, ess4, ess5, ess6, ess7, ess8));
		pd.setEquals("ess0", -10000);
		pd.setEquals("sub1", 8000); // sub1 pinned at +8000W

		TreeSolver.solve(pd.getEntries());

		// sub1 pinned: each of ess1..ess4 gets 8000/4 = 2000W
		assertEquals(2000, activePower(pd, "ess1"));
		assertEquals(2000, activePower(pd, "ess2"));
		assertEquals(2000, activePower(pd, "ess3"));
		assertEquals(2000, activePower(pd, "ess4"));

		// remaining = -10000 - 8000 = -18000W to sub2 → each gets -18000/4 = -4500W
		assertEquals(-4500, activePower(pd, "ess5"));
		assertEquals(-4500, activePower(pd, "ess6"));
		assertEquals(-4500, activePower(pd, "ess7"));
		assertEquals(-4500, activePower(pd, "ess8"));
	}

	/**
	 * No constraint → all setpoints = 0W.
	 */
	@Test
	public void testNoConstraint() {
		var ess1 = ess("ess1");
		var ess2 = ess("ess2");
		var ess0 = new DummyMetaEss("ess0", ess1, ess2);

		var pd = PowerDistribution.from(List.of(ess0, ess1, ess2));

		TreeSolver.solve(pd.getEntries());

		assertEquals(0, activePower(pd, "ess1"));
		assertEquals(0, activePower(pd, "ess2"));
	}
}
