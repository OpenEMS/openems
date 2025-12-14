package io.openems.edge.controller.ess.fixactivepower;

import static io.openems.edge.ess.power.api.Relationship.EQUALS;
import static io.openems.edge.ess.power.api.Relationship.GREATER_OR_EQUALS;
import static io.openems.edge.ess.power.api.Relationship.LESS_OR_EQUALS;
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
		assertEquals(4000 /* no discharge limitation */, t0.ef().setEss(4000));
	}

	@Test
	public void testEquals() {
		var esh = EnergyScheduler.buildEnergyScheduleHandler(new DummyController("ctrl0"),
				() -> new OptimizationContext(2000, EQUALS));
		assertEquals("", esh.getParentFactoryPid());
		assertEquals("ctrl0", esh.getParentId());

		var t = EnergyScheduleTester.from(esh);
		var t0 = t.simulatePeriod();
		assertEquals(500 /* max limited */, t0.ef().setEss(4000));
	}

	@Test
	public void testGreaterOrEquals() {
		var esh = EnergyScheduler.buildEnergyScheduleHandler(new DummyController("ctrl0"),
				() -> new OptimizationContext(2000, GREATER_OR_EQUALS));
		var t = EnergyScheduleTester.from(esh);

		var t0 = t.simulatePeriod();
		assertEquals(500 /* max limited */, t0.ef().setEss(4000));
	}

	@Test
	public void testLessOrEquals() {
		var esh = EnergyScheduler.buildEnergyScheduleHandler(new DummyController("ctrl0"),
				() -> new OptimizationContext(2000, LESS_OR_EQUALS));
		var t = EnergyScheduleTester.from(esh);

		var t0 = t.simulatePeriod();
		assertEquals(500 /* max limited */, t0.ef().setEss(4000));
	}
}