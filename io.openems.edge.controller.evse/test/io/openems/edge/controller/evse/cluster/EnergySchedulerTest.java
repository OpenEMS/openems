package io.openems.edge.controller.evse.cluster;

import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.DAILY;
import static io.openems.edge.controller.evse.TestUtils.createSingleCtrl;
import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.util.function.Supplier;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.edge.controller.evse.single.Types.Payload;
import io.openems.edge.energy.api.handler.Fitness;
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

	@Test
	public void testPostponedCharging() {
		// Controller configured with mode=ZERO and a Smart task:
		//   window  : 01:30–02:15 (periods at 01:30, 01:45, 02:00 → deadline = 02:00)
		//   minimum : 2000 Wh must be accumulated by the deadline period
		// Mode indices generated by EshUtils.generateModes (SURPLUS first, then the rest):
		//   0 = SURPLUS, 1 = ZERO, 2 = MINIMUM, 3 = FORCE
		final var CTRL = "ctrlPostponed";

		// Supplier creates a fresh ESH + tester so each sub-scenario starts with
		// sessionEnergy = 0 and a clean ClusterScheduleContext.
		final Supplier<EnergyScheduleTester> newTester = () -> {
			var ctrl = createSingleCtrl() //
					.setId(CTRL) //
					.setMode(Mode.ZERO) // default: no charging outside Smart window
					.setTasks(JSCalendar.Tasks.<Payload>create() //
							.add(t -> t //
									.setStart("01:30") //
									.setDuration(Duration.ofMinutes(45)) //
									.addRecurrenceRule(r -> r.setFrequency(DAILY)) //
									.setPayload(new Payload.Smart(2000))) //
							.build()) //
					.build();
			var esh = new DummyEnergySchedulable<>("Evse.Controller.Cluster", "ctrlEvseCluster0",
					cmp -> EnergyScheduler.buildEnergyScheduleHandler(cmp, //
							() -> EnergyScheduler.ClusterEshConfig.from(//
									DistributionStrategy.EQUAL_POWER, //
									ImmutableList.of(ctrl.getParams()))));
			return EnergyScheduleTester.from(esh.getEnergyScheduleHandler());
		};

		// --- Scenario 1: Boundary guard ---
		// Period 0 (00:00) is outside the Smart window.
		// Even when the optimizer suggests FORCE (modeIndex=3), the boundary guard
		// must override it with ZERO → no energy consumed.
		{
			var t = newTester.get();
			var sp = t.simulatePeriodIndex(0, 3); // modeIndex 3 = FORCE
			assertEquals("Boundary guard: no charging outside Smart window", //
					0, sp.ef().getManagedConsumption(CTRL));
		}

		// --- Scenario 2: Deadline not violated ---
		// Charge at FORCE (modeIndex=3) for all 3 periods inside the window.
		// Each period contributes 2760 Wh → total = 8280 Wh ≥ 2000 minimum.
		// The deadline period (index 8, 02:00) must produce zero hard-constraint violations.
		{
			var t = newTester.get();
			t.simulatePeriodIndex(6, 3); // 01:30 — FORCE: +2760 Wh
			t.simulatePeriodIndex(7, 3); // 01:45 — FORCE: +2760 Wh
			// index 8 = 02:00 = deadline; pass explicit Fitness to inspect violations
			var fitness = new Fitness();
			t.simulatePeriodIndex(8, fitness, 3); // 02:00 — FORCE: +2760 Wh, total = 8280
			assertEquals("Deadline not violated when enough energy was charged", //
					0, fitness.getHardConstraintViolations());
		}

		// --- Scenario 3: Deadline violated ---
		// ZERO mode (modeIndex=1) throughout the window → 0 Wh accumulated.
		// At the deadline period (index 8) the minimum of 2000 Wh is not met →
		// exactly one hard-constraint violation must be recorded.
		{
			var t = newTester.get();
			t.simulatePeriodIndex(6, 1); // 01:30 — ZERO: 0 Wh
			t.simulatePeriodIndex(7, 1); // 01:45 — ZERO: 0 Wh
			var fitness = new Fitness();
			t.simulatePeriodIndex(8, fitness, 1); // 02:00 — ZERO: 0 Wh, total = 0 < 2000
			assertEquals("Deadline violated when minimum energy was not reached", //
					1, fitness.getHardConstraintViolations());
		}
	}
}
