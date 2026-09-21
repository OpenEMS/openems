package io.openems.edge.heat.mypv;

import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.DAILY;
import static io.openems.edge.heat.mypv.HeatMyPvImpl.FACTORY_ID;
import static java.lang.Math.clamp;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.test.TestUtils;
import io.openems.edge.energy.api.simulation.periods.PeriodData;
import io.openems.edge.energy.api.test.DummyEnergySchedulable;
import io.openems.edge.energy.api.test.EnergyScheduleTester;

class EnergySchedulerTest {

	private static final String COMPONENT_ID = "heat0";
	private static final int MAX_HEAT_POWER = 3000; // [W]

	@Test
	void testFastHeatTask() {
		final var clock = TestUtils.createDummyClock();

		// A daily FAST_HEAT task starting at 00:00 for 15 minutes — covers period 0
		var tasks = JSCalendar.Tasks.<HeatMyPvPayload>create(clock) //
				.add(t -> t //
						.setStart("00:00") //
						.setDuration(Duration.ofMinutes(15)) //
						.addRecurrenceRule(r -> r //
								.setFrequency(DAILY)) //
						.setPayload(new HeatMyPvPayload(Mode.FAST_HEAT))) //
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

		var tasks = JSCalendar.Tasks.<HeatMyPvPayload>create(clock) //
				.add(t -> t //
						.setStart("07:00") //
						.setDuration(Duration.ofMinutes(15)) //
						.addRecurrenceRule(r -> r //
								.setFrequency(DAILY)) //
						.setPayload(new HeatMyPvPayload(Mode.SURPLUS))) //
				.build();

		var energySchedulable = new DummyEnergySchedulable<>(FACTORY_ID, COMPONENT_ID,
				cmp -> EnergyScheduler.buildEnergyScheduleHandler(cmp, () -> clock,
						() -> new EnergyScheduler.Config(Mode.OFF, MAX_HEAT_POWER, tasks)));

		var t = EnergyScheduleTester.from(energySchedulable.getEnergyScheduleHandler());

		var firstTime = t.goc.periods().getFirst().time();
		var targetTime = firstTime.withHour(7).withMinute(0);
		var periodIndex = findPeriodIndex(t, targetTime);

		var sp = t.simulatePeriodIndex(periodIndex);

		var period = sp.period();
		var unmanaged = period.data().consumption().map(PeriodData.Prediction::riskAdjusted).orElse(0);
		var surplus = period.data().production() - unmanaged;
		var maxHeatEnergy = period.duration().convertPowerToEnergy(MAX_HEAT_POWER);
		var expectedManagedConsumption = clamp(surplus, 0, maxHeatEnergy);

		assertEquals(targetTime, period.time());
		assertEquals(expectedManagedConsumption, sp.ef().getManagedConsumption(COMPONENT_ID));
	}

	@Test
	void testNoTasksDefaultOff() {
		final var clock = TestUtils.createDummyClock();

		var ctrl = new DummyEnergySchedulable<>(FACTORY_ID, COMPONENT_ID,
				cmp -> EnergyScheduler.buildEnergyScheduleHandler(cmp, () -> clock,
						() -> new EnergyScheduler.Config(Mode.OFF, MAX_HEAT_POWER, JSCalendar.Tasks.empty())));

		var t = EnergyScheduleTester.from(ctrl.getEnergyScheduleHandler());

		// No tasks, default mode is OFF → expected energy = 0 Wh for all periods
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
	}

	private static int findPeriodIndex(EnergyScheduleTester t, ZonedDateTime targetTime) {
		return IntStream.range(0, t.goc.periods().size()) //
				.filter(i -> t.goc.periods().get(i).time().equals(targetTime)) //
				.findFirst() //
				.orElseThrow(() -> new AssertionError("Period not found: " + targetTime));
	}
}