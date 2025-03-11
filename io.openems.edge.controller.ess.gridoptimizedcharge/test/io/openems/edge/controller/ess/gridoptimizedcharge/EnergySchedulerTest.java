package io.openems.edge.controller.ess.gridoptimizedcharge;

import static io.openems.edge.controller.ess.gridoptimizedcharge.EnergyScheduler.buildEnergyScheduleHandler;
import static io.openems.edge.energy.api.simulation.Coefficient.ESS;
import static org.apache.commons.math3.optim.nonlinear.scalar.GoalType.MINIMIZE;
import static org.junit.Assert.assertEquals;

import java.time.LocalTime;
import java.util.OptionalInt;

import org.junit.Test;

import io.openems.edge.controller.ess.gridoptimizedcharge.EnergyScheduler.OptimizationContext;
import io.openems.edge.controller.test.DummyController;
import io.openems.edge.energy.api.test.EnergyScheduleTester;

public class EnergySchedulerTest {

	@Test
	public void testNull() {
		var esh = buildEnergyScheduleHandler(new DummyController("ctrl0"), () -> null);
		var t = EnergyScheduleTester.from(esh);
		assertEquals(-3894 /* no charge limitation */,
				(int) t.simulatePeriod().ef().getExtremeCoefficientValue(ESS, MINIMIZE));
	}

	@Test
	public void testManual() {
		var esh = buildEnergyScheduleHandler(new DummyController("ctrl0"),
				() -> new EnergyScheduler.Config.Manual(LocalTime.of(10, 00)));
		assertEquals("ctrl0", esh.getId());

		var t = EnergyScheduleTester.from(esh);
		var csc = (OptimizationContext) t.perEsh.get(0).csc();
		var limits = csc.limits().values().toArray(OptionalInt[]::new);
		assertEquals(3, limits.length);
		assertEquals(OptionalInt.empty(), limits[0]);
		assertEquals(OptionalInt.of(1214), limits[1]);
		assertEquals(OptionalInt.empty(), limits[2]);

		assertEquals(-3894, (int) t.simulatePeriod().ef().getExtremeCoefficientValue(ESS, MINIMIZE));
		assertEquals(-1214, (int) t.simulatePeriodIndex(26).ef().getExtremeCoefficientValue(ESS, MINIMIZE));
		assertEquals(-4000, (int) t.simulatePeriodIndex(40).ef().getExtremeCoefficientValue(ESS, MINIMIZE));
	}
}
