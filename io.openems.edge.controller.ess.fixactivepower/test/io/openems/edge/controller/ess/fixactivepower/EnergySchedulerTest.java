package io.openems.edge.controller.ess.fixactivepower;

import static io.openems.edge.energy.api.simulation.Coefficient.ESS;
import static io.openems.edge.ess.power.api.Relationship.EQUALS;
import static io.openems.edge.ess.power.api.Relationship.GREATER_OR_EQUALS;
import static io.openems.edge.ess.power.api.Relationship.LESS_OR_EQUALS;
import static org.apache.commons.math3.optim.nonlinear.scalar.GoalType.MAXIMIZE;
import static org.apache.commons.math3.optim.nonlinear.scalar.GoalType.MINIMIZE;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.controller.ess.fixactivepower.EnergyScheduler.OptimizationContext;
import io.openems.edge.controller.test.DummyController;
import io.openems.edge.energy.api.test.EnergyScheduleTester;

public class EnergySchedulerTest {

	@Test
	public void testNull() {
		var esh = EnergyScheduler.buildEnergyScheduleHandler(new DummyController("ctrl0"), () -> null);
		var t = EnergyScheduleTester.from(esh);
		var t0 = t.simulatePeriod();
		assertEquals(4000 /* no discharge limitation */, (int) t0.ef().getExtremeCoefficientValue(ESS, MAXIMIZE));
	}

	@Test
	public void testEquals() {
		var esh = EnergyScheduler.buildEnergyScheduleHandler(new DummyController("ctrl0"),
				() -> new OptimizationContext(500, EQUALS));
		assertEquals("ctrl0", esh.getId());

		var t = EnergyScheduleTester.from(esh);
		var t0 = t.simulatePeriod();
		assertEquals(500 /* max limited */, (int) t0.ef().getExtremeCoefficientValue(ESS, MAXIMIZE));
		assertEquals(500 /* min limited */, (int) t0.ef().getExtremeCoefficientValue(ESS, MINIMIZE));
	}

	@Test
	public void testGreaterOrEquals() {
		var esh = EnergyScheduler.buildEnergyScheduleHandler(new DummyController("ctrl0"),
				() -> new OptimizationContext(500, GREATER_OR_EQUALS));
		var t = EnergyScheduleTester.from(esh);

		var t0 = t.simulatePeriod();
		t0.ef().logMinMaxValues();
		assertEquals(4000 /* max not limited */, (int) t0.ef().getExtremeCoefficientValue(ESS, MAXIMIZE));
		assertEquals(500 /* min limited */, (int) t0.ef().getExtremeCoefficientValue(ESS, MINIMIZE));
	}

	@Test
	public void testLessOrEquals() {
		var esh = EnergyScheduler.buildEnergyScheduleHandler(new DummyController("ctrl0"),
				() -> new OptimizationContext(500, LESS_OR_EQUALS));
		var t = EnergyScheduleTester.from(esh);

		var t0 = t.simulatePeriod();
		t0.ef().logMinMaxValues();
		assertEquals(500 /* max limited */, (int) t0.ef().getExtremeCoefficientValue(ESS, MAXIMIZE));
		assertEquals(-3894 /* min not limited */, (int) t0.ef().getExtremeCoefficientValue(ESS, MINIMIZE));
	}
}