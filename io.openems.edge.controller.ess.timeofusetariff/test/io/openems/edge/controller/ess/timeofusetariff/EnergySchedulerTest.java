package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.buildEnergyScheduleHandler;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.energy.api.test.EnergyScheduleTester;

public class EnergySchedulerTest {

	@Test
	public void testNull() {
		var esh = buildEnergyScheduleHandler(() -> null, () -> null);
		assertTrue(esh.getId().startsWith("ESH.WithDifferentModes."));

		var t = EnergyScheduleTester.from(esh);
		var t0 = t.simulatePeriod(0, 0); // BALANCING
		assertEquals(106 /* fallback to balancing */, t0.ef().solve().getEss());
	}

	@Test
	public void testChargeConsumption() {
		var esh = buildEnergyScheduleHandler(() -> "ctrl0",
				() -> new EnergyScheduler.Config(ControlMode.CHARGE_CONSUMPTION));
		var t = EnergyScheduleTester.from(esh);

		var t0 = t.simulatePeriod(0, 0); // BALANCING
		assertEquals(106, t0.ef().solve().getEss());

		var t1 = t.simulatePeriod(1, 1); // DELAY_DISCHARGE
		assertEquals(0, t1.ef().solve().getEss());

		var t2 = t.simulatePeriod(2, 2); // CHARGE
		assertEquals(-983, t2.ef().solve().getEss());
	}

	@Test
	public void testDelayDischarge() {
		var esh = buildEnergyScheduleHandler(() -> "ctrl0",
				() -> new EnergyScheduler.Config(ControlMode.DELAY_DISCHARGE));
		var t = EnergyScheduleTester.from(esh);

		var t0 = t.simulatePeriod(0, 0); // BALANCING
		assertEquals(106, t0.ef().solve().getEss());

		var t1 = t.simulatePeriod(1, 1); // DELAY_DISCHARGE
		assertEquals(0, t1.ef().solve().getEss());
	}
}