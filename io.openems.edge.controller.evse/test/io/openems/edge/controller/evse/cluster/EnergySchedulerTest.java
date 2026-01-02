package io.openems.edge.controller.evse.cluster;

import static io.openems.edge.controller.evse.TestUtils.createSingleCtrl;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.edge.energy.api.test.DummyEnergySchedulable;
import io.openems.edge.energy.api.test.EnergyScheduleTester;
import io.openems.edge.evse.api.chargepoint.Mode;

public class EnergySchedulerTest {

	@Test
	public void test() {
		var ctrl0 = createSingleCtrl() //
				.setId("ctrlEvseSingle0") //
				.setMode(Mode.FORCE) //
				.setSessionEnergyLimit(10000) //
				.build();
		var ctrl1 = createSingleCtrl() //
				.setId("ctrlEvseSingle1") //
				.setMode(Mode.SURPLUS) //
				.build();
		var ctrl2 = createSingleCtrl() //
				.setId("ctrlEvseSingle2") //
				.setMode(Mode.SURPLUS) //
				.build();

		var ctrl = new DummyEnergySchedulable<>("Evse.Controller.Cluster", "ctrlEvseCluster0",
				cmp -> EnergyScheduler.buildEnergyScheduleHandler(cmp, //
						() -> EnergyScheduler.ClusterEshConfig.from(//
								DistributionStrategy.EQUAL_POWER, //
								ImmutableList.of(ctrl0.getParams(), ctrl1.getParams(), ctrl2.getParams()))));
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

		// t40 -> esh0: FORCE (0); esh1: SURPLUS (1166); esh2: SURPLUS (1166)
		var t40 = t.simulatePeriodIndex(40, 0);
		assertEquals(2331 /* 2436 - 105 */, t40.ef().getManagedConsumption());
	}

}
