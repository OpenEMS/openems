package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.buildEnergyScheduleHandler;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import io.openems.edge.controller.test.DummyController;
import io.openems.edge.energy.api.test.EnergyScheduleTester;

public class EnergySchedulerTest {

	@Test
	public void testNull() {
		var esh = buildEnergyScheduleHandler(new DummyController("ctrl0"), () -> null);
		var t = EnergyScheduleTester.from(esh);
		var t0 = t.simulatePeriod(0 /* BALANCING */);
		assertEquals(106 /* fallback to balancing */, t0.ef().solve().getEss());
	}

	@Test
	public void testChargeConsumption() {
		var esh = buildEnergyScheduleHandler(new DummyController("ctrl0"),
				() -> new EnergyScheduler.Config(ControlMode.CHARGE_CONSUMPTION));
		var t = EnergyScheduleTester.from(esh);

		// Initial Population: DELAY_DISCHARGE and CHARGE_GRID
		var ip = t.perEsh.get(0).initialPopulation();
		assertEquals(4, ip.size());
		assertEquals(//
				"[0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]", //
				Arrays.toString(ip.get(0).modeIndexes()));
		assertEquals(//
				"[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]", //
				Arrays.toString(ip.get(1).modeIndexes()));
		assertEquals(//
				"[0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]", //
				Arrays.toString(ip.get(2).modeIndexes()));
		assertEquals(//
				"[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]", //
				Arrays.toString(ip.get(3).modeIndexes()));

		assertEquals(106, t.simulatePeriod(0 /* BALANCING */).ef().solve().getEss());
		assertEquals(0, t.simulatePeriod(1 /* DELAY_DISCHARGE */).ef().solve().getEss());
		assertEquals(-982, t.simulatePeriod(2 /* CHARGE_GRID */).ef().solve().getEss());
	}

	@Test
	public void testDelayDischarge() {
		var esh = buildEnergyScheduleHandler(new DummyController("ctrl0"),
				() -> new EnergyScheduler.Config(ControlMode.DELAY_DISCHARGE));
		var t = EnergyScheduleTester.from(esh);

		// Initial Population: DELAY_DISCHARGE only
		var ip = t.perEsh.get(0).initialPopulation();
		assertEquals(2, ip.size());
		assertEquals(//
				"[0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]", //
				Arrays.toString(ip.get(0).modeIndexes()));
		assertEquals(//
				"[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]", //
				Arrays.toString(ip.get(1).modeIndexes()));

		assertEquals(106, t.simulatePeriod(0 /* BALANCING */).ef().solve().getEss());
		assertEquals(0, t.simulatePeriod(1 /* DELAY_DISCHARGE */).ef().solve().getEss());
	}

	@Test
	public void testDischargeToGrid() {
		var esh = buildEnergyScheduleHandler(new DummyController("ctrl0"),
				() -> new EnergyScheduler.Config(ControlMode.DISCHARGE_TO_GRID));
		var t = EnergyScheduleTester.from(esh);

		// Initial Population: DELAY_DISCHARGE, CHARGE_GRID and DISCHARGE_GRID
		var ip = t.perEsh.get(0).initialPopulation();
		assertEquals(5, ip.size());
	}
}