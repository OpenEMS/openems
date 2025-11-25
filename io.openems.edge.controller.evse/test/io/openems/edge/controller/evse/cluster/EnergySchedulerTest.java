package io.openems.edge.controller.evse.cluster;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.controller.evse.single.EnergyScheduler.buildManualEnergyScheduleHandler;
import static io.openems.edge.controller.evse.single.EnergySchedulerTest.createAbilities;
import static io.openems.edge.controller.evse.single.EnergySchedulerTest.createSmartEsh;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.edge.controller.evse.single.EnergyScheduler.EshEvseSingle;
import io.openems.edge.controller.test.DummyController;
import io.openems.edge.energy.api.test.DummyEnergySchedulable;
import io.openems.edge.energy.api.test.EnergyScheduleTester;
import io.openems.edge.evse.api.chargepoint.Mode;

public class EnergySchedulerTest {

	@Test
	public void test() {
		var esh0 = new EshEvseSingle(null, buildManualEnergyScheduleHandler(new DummyController("ctrl0"),
				() -> new io.openems.edge.controller.evse.single.EnergyScheduler.Config.ManualOptimizationContext(
						Mode.Actual.FORCE, createAbilities(THREE_PHASE, true), //
						false /* appearsToBeFullyCharged */, //
						0, /* sessionEnergy */ //
						10000 /* sessionEnergyLimit */)));
		var esh1 = new EshEvseSingle(createSmartEsh(), null);
		var esh2 = new EshEvseSingle(createSmartEsh(), null);

		var ctrl = new DummyEnergySchedulable<>("Evse.Controller.Cluster", "ctrlEvseCluster0",
				cmp -> EnergyScheduler.buildEnergyScheduleHandler(cmp, //
						() -> new EnergyScheduler.EshConfig(//
								DistributionStrategy.EQUAL_POWER, //
								ImmutableList.of(esh0, esh1, esh2))));
		var esh = ctrl.getEnergyScheduleHandler();

		var t = EnergyScheduleTester.from(esh);

		// t0 -> esh0: FORCE (2760); esh1: SURPLUS (0); esh2: SURPLUS (0)
		var t0 = t.simulatePeriodIndex(0, 0);
		assertEquals(2760, t0.ef().getManagedConsumption());

		var t1 = t.simulatePeriodIndex(1, 0);
		// t1 -> esh0: FORCE (2760); esh1: SURPLUS (0); esh2: SURPLUS (0)
		assertEquals(2760, t1.ef().getManagedConsumption());

		// t2 -> esh0: FORCE (2760); esh1: SURPLUS (0); esh2: SURPLUS (0)
		var t2 = t.simulatePeriodIndex(2, 0);
		assertEquals(2760, t2.ef().getManagedConsumption());

		// t3 -> esh0: FORCE (1720); esh1: SURPLUS (0); esh2: SURPLUS (0)
		var t3 = t.simulatePeriodIndex(3, 0);
		assertEquals(1720 /* sessionEnergyLimit reached */, t3.ef().getManagedConsumption());

		// t4 -> esh0: FORCE (0); esh1: SURPLUS (0); esh2: SURPLUS (0)
		var t4 = t.simulatePeriodIndex(4, 0);
		assertEquals(0, t4.ef().getManagedConsumption());

		// ...

		// t40 -> esh0: FORCE (0); esh1: SURPLUS (291); esh2: SURPLUS (291)
		var t40 = t.simulatePeriodIndex(40, 0);
		assertEquals(582 /* 2436 W - 105 W -> 15-Minutes */, t40.ef().getManagedConsumption());
	}

}
