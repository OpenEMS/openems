package io.openems.edge.controller.ess.emergencycapacityreserve;

import static io.openems.edge.energy.api.simulation.Coefficient.ESS;
import static org.apache.commons.math3.optim.nonlinear.scalar.GoalType.MAXIMIZE;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.controller.test.DummyController;
import io.openems.edge.energy.api.test.EnergyScheduleTester;

public class EnergySchedulerTest {

	@Test
	public void testNull() {
		var esh = EnergyScheduler.buildEnergyScheduleHandler(new DummyController("ctrl0"), () -> null);
		var t = EnergyScheduleTester.from(esh);
		assertEquals(4000 /* no discharge limitation */,
				(int) t.simulatePeriod().ef().getExtremeCoefficientValue(ESS, MAXIMIZE));
	}

	@Test
	public void testMinSoc() {
		var esh = EnergyScheduler.buildEnergyScheduleHandler(new DummyController("ctrl0"), () -> 20 /* [%] */);
		assertEquals("ctrl0", esh.getId());

		var t = EnergyScheduleTester.from(esh);
		assertEquals(600 /* discharge limited */,
				(int) t.simulatePeriod().ef().getExtremeCoefficientValue(ESS, MAXIMIZE));
	}
}