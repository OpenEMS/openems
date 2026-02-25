package io.openems.edge.controller.evse.cluster;

import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.DAILY;
import static io.openems.edge.controller.evse.TestUtils.createSingleCtrl;
import static org.junit.Assert.assertEquals;

import java.time.Duration;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.edge.controller.evse.single.Types.Payload;
import io.openems.edge.energy.api.test.DummyEnergySchedulable;
import io.openems.edge.energy.api.test.EnergyScheduleTester;
import io.openems.edge.evse.api.chargepoint.Mode;

public class EnergySchedulerTest {

	private static final String CTRL_FORCE = "ctrlEvseSingle0";
	private static final String CTRL_SURPLUS0 = "ctrlEvseSingle1";
	private static final String CTRL_SURPLUS1 = "ctrlEvseSingle2";
	private static final String CTRL_MINIMUM = "ctrlEvseSingle3";
	private static final String CTRL_NOT_READY = "ctrlEvseSingle4";

	@Test
	public void test() {
		var ctrl0 = createSingleCtrl() //
				.setId(CTRL_FORCE) //
				.setMode(Mode.FORCE) //
				.setSessionEnergyLimit(10000) //
				.build();
		var ctrl1 = createSingleCtrl() //
				.setId(CTRL_SURPLUS0) //
				.setMode(Mode.SURPLUS) //
				.build();
		var ctrl2 = createSingleCtrl() //
				.setId(CTRL_SURPLUS1) //
				.setMode(Mode.SURPLUS) //
				.setTasks(JSCalendar.Tasks.<Payload>create() //
						.add(t -> t //
								.setStart("01:15") //
								.setDuration(Duration.ofMinutes(15)) //
								.addRecurrenceRule(r -> r //
										.setFrequency(DAILY)) //
								.setPayload(new Payload.Manual(Mode.FORCE))) //
						.build()) //
				.build();
		var ctrl3 = createSingleCtrl() //
				.setId(CTRL_MINIMUM) //
				.setMode(Mode.MINIMUM) //
				.setSessionEnergyLimit(2000) //
				.build();
		var ctrl4 = createSingleCtrl() //
				.setId(CTRL_NOT_READY) //
				.setMode(Mode.MINIMUM) //
				.setCombinedAbilities(ca -> ca. //
						setIsReadyForCharging(false)) //
				.build();

		var ctrl = new DummyEnergySchedulable<>("Evse.Controller.Cluster", "ctrlEvseCluster0",
				cmp -> EnergyScheduler.buildEnergyScheduleHandler(cmp, //
						() -> EnergyScheduler.ClusterEshConfig.from(//
								DistributionStrategy.EQUAL_POWER, //
								ImmutableList.of(ctrl0.getParams(), ctrl1.getParams(), ctrl2.getParams(),
										ctrl3.getParams(), ctrl4.getParams()))));
		var esh = ctrl.getEnergyScheduleHandler();

		var t = EnergyScheduleTester.from(esh);

		{
			// esh0: FORCE (2760); esh1: SURPLUS (0); esh2: SURPLUS (0);
			// esh3: MINIMUM (1035); esh4: ZERO (not ready)
			var sp = t.simulatePeriodIndex(0, 0);
			assertEquals(2760, sp.ef().getManagedConsumption(CTRL_FORCE));
			assertEquals(1035, sp.ef().getManagedConsumption(CTRL_MINIMUM));
			assertEquals(3795, sp.ef().getManagedConsumption());
		}
		{
			// esh0: FORCE (2760); esh1: SURPLUS (0); esh2: SURPLUS (0);
			// esh3: MINIMUM (965, energy limit); esh4: ZERO (not ready)
			var sp = t.simulatePeriodIndex(1, 0);
			assertEquals(2760, sp.ef().getManagedConsumption(CTRL_FORCE));
			assertEquals(965, sp.ef().getManagedConsumption(CTRL_MINIMUM));
			assertEquals(3725, sp.ef().getManagedConsumption());
		}
		{
			// esh0: FORCE (2760); esh1: SURPLUS (0); esh2: SURPLUS (0)
			// esh3: MINIMUM (0); esh4: ZERO (not ready)
			var sp = t.simulatePeriodIndex(2, 0);
			assertEquals(2760, sp.ef().getManagedConsumption(CTRL_FORCE));
			assertEquals(0, sp.ef().getManagedConsumption(CTRL_MINIMUM));
			assertEquals(2760, sp.ef().getManagedConsumption());
		}
		{
			// esh0: FORCE (1720, energy limit); esh1: SURPLUS (0); esh2: SURPLUS (0)
			var sp = t.simulatePeriodIndex(3, 0);
			assertEquals(1720, sp.ef().getManagedConsumption(CTRL_FORCE));
			assertEquals(1720, sp.ef().getManagedConsumption());
		}
		{
			// esh0: FORCE (0); esh1: SURPLUS (0); esh2: SURPLUS (0)
			var sp = t.simulatePeriodIndex(4, 0);
			assertEquals(0, sp.ef().getManagedConsumption());
		}
		{
			// esh2: Manual Task FORCE
			var sp = t.simulatePeriod(5, 0);
			assertEquals(2760, sp.ef().getManagedConsumption(CTRL_SURPLUS1));
			assertEquals(2760, sp.ef().getManagedConsumption());
		}
		{
			// esh2: No more Manual Task
			var sp = t.simulatePeriod(6, 0);
			assertEquals(0, sp.ef().getManagedConsumption(CTRL_SURPLUS1));
			assertEquals(0, sp.ef().getManagedConsumption());
		}
		// ...
		{
			// esh0: FORCE (0); esh1: SURPLUS (5953); esh2: SURPLUS (5953)
			var sp = t.simulatePeriodIndex(30, 0);
			assertEquals(5953, sp.ef().getManagedConsumption(CTRL_SURPLUS0));
			assertEquals(5953, sp.ef().getManagedConsumption(CTRL_SURPLUS1));
			assertEquals(11906, sp.ef().getManagedConsumption());
		}
	}

}
