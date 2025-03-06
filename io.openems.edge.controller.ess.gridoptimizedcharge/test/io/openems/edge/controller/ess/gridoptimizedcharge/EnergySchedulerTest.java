package io.openems.edge.controller.ess.gridoptimizedcharge;

import static io.openems.edge.energy.api.simulation.Coefficient.ESS;
import static org.apache.commons.math3.optim.nonlinear.scalar.GoalType.MINIMIZE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalTime;
import java.util.OptionalInt;

import org.junit.Test;

import io.openems.edge.controller.ess.gridoptimizedcharge.EnergyScheduler.OptimizationContext;
import io.openems.edge.energy.api.test.EnergyScheduleTester;

public class EnergySchedulerTest {

	@Test
	public void testNull() {
		var esh = EnergyScheduler.buildEnergyScheduleHandler(() -> null, () -> null);
		assertTrue(esh.getId().startsWith("ESH.WithOnlyOneMode."));

		var t = EnergyScheduleTester.from(esh);
		var t0 = t.simulatePeriod(0);
		assertEquals(-3894 /* no charge limitation */,
				(int) t0.ef().getExtremeCoefficientValue(ESS, MINIMIZE));
	}

	@Test
	public void testManual() {
		var esh = EnergyScheduler.buildEnergyScheduleHandler(() -> "ctrl0",
				() -> new EnergyScheduler.Config.Manual(LocalTime.of(10, 00)));
		assertEquals("ctrl0", esh.getId());

		var t = EnergyScheduleTester.from(esh);
		var csc = (OptimizationContext) t.cscs.get(0);
		var limits = csc.limits().values().toArray(OptionalInt[]::new);
		assertEquals(3, limits.length);
		assertEquals(OptionalInt.empty(), limits[0]);
		assertEquals(OptionalInt.of(1214), limits[1]);
		assertEquals(OptionalInt.empty(), limits[2]);

		var t0 = t.simulatePeriod(0);
		assertEquals(-3894, (int) t0.ef().getExtremeCoefficientValue(ESS, MINIMIZE));

		var t26 = t.simulatePeriod(26);
		assertEquals(-1214, (int) t26.ef().getExtremeCoefficientValue(ESS, MINIMIZE));

		var t40 = t.simulatePeriod(40);
		assertEquals(-4000, (int) t40.ef().getExtremeCoefficientValue(ESS, MINIMIZE));
	}
}
