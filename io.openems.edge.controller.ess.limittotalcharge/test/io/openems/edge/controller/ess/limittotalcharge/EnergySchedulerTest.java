package io.openems.edge.controller.ess.limittotalcharge;

import static io.openems.edge.controller.ess.limittotalcharge.EnergyScheduler.buildEnergyScheduleHandler;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.openems.edge.controller.test.DummyController;
import io.openems.edge.energy.api.test.EnergyScheduleTester;

public class EnergySchedulerTest {

	@Test
	public void testNull() {
		var esh = buildEnergyScheduleHandler(new DummyController("ctrl0"), () -> null);
		var t = EnergyScheduleTester.from(esh);
		assertEquals(-3894 /* no charge limitation */, t.simulatePeriod().ef().setEss(-4000));
	}

	@Test
	public void testMaxSoc() {
		var esh = buildEnergyScheduleHandler(new DummyController("ctrl0"),
				() -> new EnergyScheduler.Config(30 /* [%] */));
		assertEquals("", esh.getParentFactoryPid());
		assertEquals("ctrl0", esh.getParentId());

		var t = EnergyScheduleTester.from(esh);
		assertEquals(-1600 /* charge limited */, t.simulatePeriod().ef().setEss(-4000));
	}
}
