package io.openems.edge.heat.askoma;

import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.DAILY;
import static io.openems.edge.heat.askoma.HeatAskomaImpl.FACTORY_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.test.TestUtils;
import io.openems.edge.energy.api.test.DummyEnergySchedulable;
import io.openems.edge.energy.api.test.EnergyScheduleTester;

class EnergySchedulerTest {

	private static final String COMPONENT_ID = "heat0";
	private static final int MAX_HEAT_POWER = 3000; // [W]

	@Test
	void testFastHeatTask() {
		final var clock = TestUtils.createDummyClock();

		// A daily FAST_HEAT task starting at 00:00 for 15 minutes — covers period 0
		var tasks = JSCalendar.Tasks.<HeatAskomaPayload>create(clock) //
				.add(t -> t //
						.setStart("00:00") //
						.setDuration(Duration.ofMinutes(15)) //
						.addRecurrenceRule(r -> r //
								.setFrequency(DAILY)) //
						.setPayload(new HeatAskomaPayload(Mode.FAST_HEAT))) //
				.build();

		var energySchedulable = new DummyEnergySchedulable<>(FACTORY_ID, COMPONENT_ID,
				cmp -> EnergyScheduler.buildEnergyScheduleHandler(cmp, () -> clock,
						() -> new EnergyScheduler.Config(Mode.OFF, MAX_HEAT_POWER, tasks)));

		var t = EnergyScheduleTester.from(energySchedulable.getEnergyScheduleHandler());

		// Period 0 is at 00:00 — task is active → expected energy = 3000 W / 4 = 750 Wh
		var sp = t.simulatePeriod();
		assertEquals(750, sp.ef().getManagedConsumption(COMPONENT_ID));
	}

	@Test
	void testSurplusHeatTask() {
		final var clock = TestUtils.createDummyClock();

		// A daily FAST_HEAT task starting at 00:00 for 15 minutes — covers period 0
		var tasks = JSCalendar.Tasks.<HeatAskomaPayload>create(clock) //
				.add(t -> t //
						.setStart("00:00") //
						.setDuration(Duration.ofMinutes(15)) //
						.addRecurrenceRule(r -> r //
								.setFrequency(DAILY)) //
						.setPayload(new HeatAskomaPayload(Mode.SURPLUS))) //
				.build();

		var energySchedulable = new DummyEnergySchedulable<>(FACTORY_ID, COMPONENT_ID,
				cmp -> EnergyScheduler.buildEnergyScheduleHandler(cmp, () -> clock,
						() -> new EnergyScheduler.Config(Mode.OFF, MAX_HEAT_POWER, tasks)));

		var t = EnergyScheduleTester.from(energySchedulable.getEnergyScheduleHandler());

		// covering period 0 → consumption = 0
		var sp = t.simulatePeriod();
		assertEquals(0, sp.ef().getManagedConsumption(COMPONENT_ID));
	}

	@Test
	void testNoTasksDefaultOff() {
		final var clock = TestUtils.createDummyClock();

		var ctrl = new DummyEnergySchedulable<>("Heat.Askoma", COMPONENT_ID,
				cmp -> EnergyScheduler.buildEnergyScheduleHandler(cmp, () -> clock,
						() -> new EnergyScheduler.Config(Mode.OFF, MAX_HEAT_POWER, JSCalendar.Tasks.empty())));

		var t = EnergyScheduleTester.from(ctrl.getEnergyScheduleHandler());

		// No tasks, default mode is OFF → expected energy = 0 Wh for all periods
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
	}

}