package io.openems.edge.controller.ess.emergencycapacityreserve;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.controller.test.DummyController;
import io.openems.edge.energy.api.test.EnergyScheduleTester;

public class EnergySchedulerTest {

	@Test
	public void testNull() {
		var esh = EnergyScheduler.buildEnergyScheduleHandler(new DummyController("ctrl0"), () -> null);
		var t = EnergyScheduleTester.from(esh);
		// No discharge limitation
		assertEquals(4000, t.simulatePeriod().ef().setEss(4000));
	}

	@Test
	public void testMinSoc() {
		var esh = EnergyScheduler.buildEnergyScheduleHandler(new DummyController("ctrl0"),
				() -> new EnergyScheduler.Config(20 /* [%] */));
		assertEquals("", esh.getParentFactoryPid());
		assertEquals("ctrl0", esh.getParentId());

		var t = EnergyScheduleTester.from(esh);
		// ESS discharge is limited to 600
		assertEquals(600, t.simulatePeriod().ef().setEss(4000));
	}
}