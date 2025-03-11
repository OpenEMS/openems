package io.openems.edge.controller.evse.single;

import static io.openems.edge.controller.evse.single.EnergyScheduler.buildManualEnergyScheduleHandler;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.edge.energy.api.simulation.Coefficient;
import io.openems.edge.energy.api.test.EnergyScheduleTester;
import io.openems.edge.evse.api.Limit;
import io.openems.edge.evse.api.SingleThreePhase;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint.ChargeParams;
import io.openems.edge.evse.api.chargepoint.Mode;

public class EnergySchedulerTest {

	@Test
	public void testNull() {
		var esh = buildManualEnergyScheduleHandler(() -> null, () -> null);
		assertTrue(esh.getId().startsWith("ESH.WithOnlyOneMode."));

		var t = EnergyScheduleTester.from(esh);
		assertEquals(4000 /* no discharge limitation */,
				(int) t.simulatePeriod().ef().getExtremeCoefficientValue(Coefficient.ESS, GoalType.MAXIMIZE));
	}

	@Test
	public void testMinimum() {
		var esh = buildManualEnergyScheduleHandler(//
				() -> "ctrl0", //
				() -> new EnergyScheduler.ManualOptimizationContext(Mode.Actual.MINIMUM, true,
						new ChargeParams(new Limit(SingleThreePhase.THREE, 6000, 32000), ImmutableList.of()), //
						1000, 5_000));
		assertEquals("ctrl0", esh.getId());

		var t = EnergyScheduleTester.from(esh);
		assertEquals(1035, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(1035, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(1035, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(895, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());
	}

	@Test
	public void testForce() {
		var esh = buildManualEnergyScheduleHandler(//
				() -> "ctrl0", //
				() -> new EnergyScheduler.ManualOptimizationContext(Mode.Actual.FORCE, true,
						new ChargeParams(new Limit(SingleThreePhase.THREE, 6000, 32000), ImmutableList.of()), //
						1000, 20_000));
		assertEquals("ctrl0", esh.getId());

		var t = EnergyScheduleTester.from(esh);
		assertEquals(5520, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(5520, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(5520, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(2440, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());
	}

	@Test
	public void testZero() {
		var esh = buildManualEnergyScheduleHandler(//
				() -> "ctrl0", //
				() -> new EnergyScheduler.ManualOptimizationContext(Mode.Actual.ZERO, true,
						new ChargeParams(new Limit(SingleThreePhase.THREE, 6000, 32000), ImmutableList.of()), //
						1000, 20_000));
		assertEquals("ctrl0", esh.getId());

		var t = EnergyScheduleTester.from(esh);
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());
	}

	@Test
	public void testSurplus() {
		var esh = buildManualEnergyScheduleHandler(//
				() -> "ctrl0", //
				() -> new EnergyScheduler.ManualOptimizationContext(Mode.Actual.SURPLUS, true,
						new ChargeParams(new Limit(SingleThreePhase.SINGLE, 6000, 32000), ImmutableList.of()), //
						1000, 15_000));
		assertEquals("ctrl0", esh.getId());

		var t = EnergyScheduleTester.from(esh);
		for (var i = 0; i < 35; i++) {
			assertEquals(345, t.simulatePeriod().ef().getManagedConsumption());
		}
		assertEquals(379, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(424, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(362, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(403, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(357, t.simulatePeriod().ef().getManagedConsumption());
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption());
	}
}
