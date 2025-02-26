package io.openems.edge.controller.ess.limittotaldischarge;

import static io.openems.edge.controller.ess.limittotaldischarge.EnergyScheduler.buildEnergyScheduleHandler;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.junit.Test;

import io.openems.edge.energy.api.simulation.Coefficient;
import io.openems.edge.energy.api.test.EnergyScheduleTester;

public class EnergySchedulerTest {

	@Test
	public void testNull() {
		var esh = buildEnergyScheduleHandler(() -> null, () -> null);
		assertTrue(esh.getId().startsWith("ESH.WithOnlyOneMode."));

		var t = EnergyScheduleTester.from(esh);
		var t0 = t.simulatePeriod(0);
		assertEquals(4000 /* no discharge limitation */,
				(int) t0.ef().getExtremeCoefficientValue(Coefficient.ESS, GoalType.MAXIMIZE));
	}

	@Test
	public void testMinSoc() {
		var esh = buildEnergyScheduleHandler(() -> "ctrl0", () -> 20 /* [%] */);
		assertEquals("ctrl0", esh.getId());

		var t = EnergyScheduleTester.from(esh);
		var t0 = t.simulatePeriod(0);
		assertEquals(600 /* discharge limited */,
				(int) t0.ef().getExtremeCoefficientValue(Coefficient.ESS, GoalType.MAXIMIZE));
	}
}