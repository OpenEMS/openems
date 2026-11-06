package io.openems.edge.ess.core.power.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyMetaEss;

public class TreeSolverConservationRegressionTest {

	private static DummyManagedSymmetricEss ess(String id, int chargeMin, int dischargeMax) {
		return new DummyManagedSymmetricEss(id) //
				.withSoc(50) //
				.withAllowedChargePower(chargeMin) //
				.withAllowedDischargePower(dischargeMax) //
				.withMaxApparentPower(10000);
	}

	private static int activePower(PowerDistribution pd, String essId) {
		return ((PowerDistribution.Entry.Actual) pd.getEntries().get(essId)).activePowerSetPoint;
	}

	@Test
	public void testFeasibleClusterTargetWithPositiveMinimumDischarge() {
		var a = ess("essA", -10000, 10000);
		var b = ess("essB", 4000, 10000);
		var cluster = new DummyMetaEss("cluster", a, b);

		var pd = PowerDistribution.from(List.of(cluster, a, b));
		pd.setEquals("cluster", 5000);
		pd.solve();

		assertEquals(1000, activePower(pd, "essA"), "ESS A should compensate the forced minimum on B");
		assertEquals(4000, activePower(pd, "essB"), "BMS-enforced minimum");
		assertEquals(5000, activePower(pd, "essA") + activePower(pd, "essB"), "Feasible cluster power target must be conserved");
	}

	@Test
	public void testSameTargetWithReverseMemberOrder() {
		var a = ess("essA", -10000, 10000);
		var b = ess("essB", 4000, 10000);
		var cluster = new DummyMetaEss("cluster", b, a);

		var pd = PowerDistribution.from(List.of(cluster, a, b));
		pd.setEquals("cluster", 5000);
		pd.solve();

		assertEquals(5000, activePower(pd, "essA") + activePower(pd, "essB"), "Aggregate output cannot depend on child order");
	}

	@Test
	public void testZeroClusterTargetWithForcedDischarge() {
		var a = ess("essA", -10000, 10000);
		var b = ess("essB", 4000, 10000);
		var cluster = new DummyMetaEss("cluster", a, b);

		var pd = PowerDistribution.from(List.of(cluster, a, b));
		pd.setEquals("cluster", 0);
		pd.solve();

		assertEquals(-4000, activePower(pd, "essA"), "ESS A must absorb the forced discharge");
		assertEquals(4000, activePower(pd, "essB"), "ESS B retains its hardware minimum");
		assertEquals(0, activePower(pd, "essA") + activePower(pd, "essB"), "Feasible zero cluster target must be conserved");
	}

	@Test
	public void testOrdinaryProportionalDistributionUnchanged() {
		var a = ess("essA", -10000, 10000);
		var b = ess("essB", -10000, 10000);
		var cluster = new DummyMetaEss("cluster", a, b);

		var pd = PowerDistribution.from(List.of(cluster, a, b));
		pd.setEquals("cluster", 5000);
		pd.solve();

		assertEquals(2500, activePower(pd, "essA"));
		assertEquals(2500, activePower(pd, "essB"));
	}

	@Test
	public void testResidualSharedByAvailableHeadroom() {
		var a = ess("essA", -5000, 5000);
		var c = ess("essC", -10000, 10000);
		var b = ess("essB", 4000, 10000);
		var cluster = new DummyMetaEss("cluster", a, c, b);

		var pd = PowerDistribution.from(List.of(cluster, a, c, b));
		pd.setEquals("cluster", 7000);
		pd.solve();

		assertEquals(1000, activePower(pd, "essA"));
		assertEquals(2000, activePower(pd, "essC"));
		assertEquals(4000, activePower(pd, "essB"));
	}

	@Test
	public void testResidualSharesUnaffectedByChildOrder() {
		var a = ess("essA", -5000, 5000);
		var c = ess("essC", -10000, 10000);
		var b = ess("essB", 4000, 10000);
		var cluster = new DummyMetaEss("cluster", c, a, b);

		var pd = PowerDistribution.from(List.of(cluster, c, a, b));
		pd.setEquals("cluster", 7000);
		pd.solve();

		assertEquals(1000, activePower(pd, "essA"));
		assertEquals(2000, activePower(pd, "essC"));
		assertEquals(4000, activePower(pd, "essB"));
	}

	@Test
	public void testFeasibleNegativeTargetWithForcedCharging() {
		var a = ess("essA", -10000, 10000);
		var b = ess("essB", -10000, -4000);
		var cluster = new DummyMetaEss("cluster", a, b);

		var pd = PowerDistribution.from(List.of(cluster, a, b));
		pd.setEquals("cluster", -5000);
		pd.solve();

		assertEquals(-1000, activePower(pd, "essA"));
		assertEquals(-4000, activePower(pd, "essB"));
		assertEquals(-5000, activePower(pd, "essA") + activePower(pd, "essB"));
	}

	@Test
	public void testInfeasibleTargetStillRespectsHardwareMaximum() {
		var a = ess("essA", -10000, 10000);
		var b = ess("essB", 4000, 10000);
		var cluster = new DummyMetaEss("cluster", a, b);

		var pd = PowerDistribution.from(List.of(cluster, a, b));
		pd.setEquals("cluster", 25000);
		pd.solve();

		assertEquals(10000, activePower(pd, "essA"));
		assertEquals(10000, activePower(pd, "essB"));
	}

	@Test
	public void testNoClusterConstraintPreservesForcedDischarge() {
		var a = ess("essA", -10000, 10000);
		var b = ess("essB", 4000, 10000);
		var cluster = new DummyMetaEss("cluster", a, b);

		var pd = PowerDistribution.from(List.of(cluster, a, b));
		pd.solve();

		assertEquals(0, activePower(pd, "essA"));
		assertEquals(4000, activePower(pd, "essB"));
	}
}
