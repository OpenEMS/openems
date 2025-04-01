package io.openems.edge.controller.ess.limittotaldischarge;

import static io.openems.edge.controller.ess.limittotaldischarge.EnergyScheduler.buildEnergyScheduleHandler;
import static org.junit.Assert.assertEquals;

import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.junit.Test;

import io.openems.edge.controller.test.DummyController;
import io.openems.edge.energy.api.simulation.Coefficient;
import io.openems.edge.energy.api.test.EnergyScheduleTester;

public class EnergySchedulerTest {

	@Test
	public void testNull() {
		var esh = buildEnergyScheduleHandler(new DummyController("ctrl0"), () -> null);
		var t = EnergyScheduleTester.from(esh);
		assertEquals(4000 /* no discharge limitation */,
				(int) t.simulatePeriod().ef().getExtremeCoefficientValue(Coefficient.ESS, GoalType.MAXIMIZE));
	}

	@Test
	public void testMinSoc() {
		var esh = buildEnergyScheduleHandler(new DummyController("ctrl0"),
				() -> new EnergyScheduler.Config(20 /* [%] */));
		assertEquals("", esh.getParentFactoryPid());
		assertEquals("ctrl0", esh.getParentId());

		var t = EnergyScheduleTester.from(esh);
		assertEquals(600 /* discharge limited */,
				(int) t.simulatePeriod().ef().getExtremeCoefficientValue(Coefficient.ESS, GoalType.MAXIMIZE));
	}
}