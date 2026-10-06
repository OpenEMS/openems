package io.openems.edge.heat.askoma;

import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.DAILY;
import static io.openems.edge.heat.askoma.HeatAskomaImpl.FACTORY_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.test.TestUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.OneMode;
import io.openems.edge.energy.api.test.AbstractDummyEnergySchedulable;
import io.openems.edge.energy.api.test.DummyEnergySchedulable;
import io.openems.edge.energy.api.test.EnergyScheduleTester;
import io.openems.edge.heat.api.Heat;

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

	@Test
	void testRemainingHeatEnergyLimitsTotalEnergyAcrossMultiplePeriods() {
		final var clock = TestUtils.createDummyClock();
		var remainingHeatEnergy = 2500;
		var maxHeatPower = 4000;

		var tasks = JSCalendar.Tasks.<HeatAskomaPayload>create(clock) //
				.add(t -> t //
						.setStart("00:00") //
						.setDuration(Duration.ofMinutes(60)) //
						.addRecurrenceRule(r -> r //
								.setFrequency(DAILY)) //
						.setPayload(new HeatAskomaPayload(Mode.FAST_HEAT))) //
				.build();

		var t = EnergyScheduleTester
				.from(new TestEnergySchedulable(clock, new EnergyScheduler.Config(Mode.OFF, maxHeatPower, tasks),
						remainingHeatEnergy).getEnergyScheduleHandler());

		var totalEnergy = IntStream.range(0, 4) //
				.map(i -> t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID)) //
				.sum();

		assertEquals(remainingHeatEnergy, totalEnergy);
	}

	@Test
	void testLastPeriodIsLimitedToRemainingHeatEnergy() {
		final var clock = TestUtils.createDummyClock();
		var remainingHeatEnergy = 1250;
		var maxHeatPower = 4000;

		var tasks = JSCalendar.Tasks.<HeatAskomaPayload>create(clock) //
				.add(t -> t //
						.setStart("00:00") //
						.setDuration(Duration.ofMinutes(30)) //
						.addRecurrenceRule(r -> r //
								.setFrequency(DAILY)) //
						.setPayload(new HeatAskomaPayload(Mode.FAST_HEAT))) //
				.build();
		var t = EnergyScheduleTester
				.from(new TestEnergySchedulable(clock, new EnergyScheduler.Config(Mode.OFF, maxHeatPower, tasks),
						remainingHeatEnergy).getEnergyScheduleHandler());

		assertEquals(1000, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
		assertEquals(250, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
	}

	@Test
	void testOnlyAcceptedEnergyIsDeductedFromRemainingHeatEnergy() {
		final var clock = TestUtils.createDummyClock();
		var tasks = JSCalendar.Tasks.<HeatAskomaPayload>create(clock) //
				.add(t -> t //
						.setStart("00:00") //
						.setDuration(Duration.ofMinutes(30)) //
						.addRecurrenceRule(r -> r //
								.setFrequency(DAILY)) //
						.setPayload(new HeatAskomaPayload(Mode.FAST_HEAT))) //
				.build();
		var heat = new TestEnergySchedulable(clock, new EnergyScheduler.Config(Mode.OFF, 4000, tasks), 1000);
		var firstPeriodLimit = new OneMode.Builder<Void, Void>("Controller.Dummy", "limit0") //
				.setSimulator((id, period, gsc, coc, csc, ef, fitness) -> {
					if (period.time().equals(gsc.goc.periods().getFirst().time())) {
						ef.setEssMaxDischarge(0);
						ef.setGridMaxBuy(400 - ef.getSurplus());
					}
				}) //
				.build();
		var t = EnergyScheduleTester.from(firstPeriodLimit, heat.getEnergyScheduleHandler());

		assertEquals(400, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
		assertEquals(600, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
	}

	@Test
	void testOffDoesNotConsumeRemainingHeatEnergy() {
		final var clock = TestUtils.createDummyClock();

		var tasks = JSCalendar.Tasks.<HeatAskomaPayload>create(clock) //
				.add(t -> t //
						.setStart("00:00") //
						.setDuration(Duration.ofMinutes(15)) //
						.addRecurrenceRule(r -> r //
								.setFrequency(DAILY)) //
						.setPayload(new HeatAskomaPayload(Mode.FAST_HEAT))) //
				.add(t -> t //
						.setStart("00:15") //
						.setDuration(Duration.ofMinutes(15)) //
						.addRecurrenceRule(r -> r //
								.setFrequency(DAILY)) //
						.setPayload(new HeatAskomaPayload(Mode.OFF))) //
				.add(t -> t //
						.setStart("00:30") //
						.setDuration(Duration.ofMinutes(15)) //
						.addRecurrenceRule(r -> r //
								.setFrequency(DAILY)) //
						.setPayload(new HeatAskomaPayload(Mode.FAST_HEAT))) //
				.build();

		var t = EnergyScheduleTester
				.from(new TestEnergySchedulable(clock, new EnergyScheduler.Config(Mode.OFF, 4000, tasks), 1500)
						.getEnergyScheduleHandler());

		assertEquals(1000, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
		assertEquals(500, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
	}

	@Test
	void testMissingRemainingHeatEnergyKeepsPreviousSchedulerBehavior() {
		final var clock = TestUtils.createDummyClock();

		var tasks = JSCalendar.Tasks.<HeatAskomaPayload>create(clock) //
				.add(t -> t //
						.setStart("00:00") //
						.setDuration(Duration.ofMinutes(45)) //
						.addRecurrenceRule(r -> r //
								.setFrequency(DAILY)) //
						.setPayload(new HeatAskomaPayload(Mode.FAST_HEAT))) //
				.build();

		var t = EnergyScheduleTester
				.from(new TestEnergySchedulable(clock, new EnergyScheduler.Config(Mode.OFF, 4000, tasks), null)
						.getEnergyScheduleHandler());

		assertEquals(1000, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
		assertEquals(1000, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
		assertEquals(1000, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
	}

	@Test
	void testSurplusIsLimitedToRemainingHeatEnergy() {
		final var clock = TestUtils.createDummyClock();

		var tasks = JSCalendar.Tasks.<HeatAskomaPayload>create(clock) //
				.add(t -> t //
						.setStart("12:00") //
						.setDuration(Duration.ofMinutes(30)) //
						.addRecurrenceRule(r -> r //
								.setFrequency(DAILY)) //
						.setPayload(new HeatAskomaPayload(Mode.SURPLUS))) //
				.build();

		var config = new EnergyScheduler.Config(Mode.OFF, 4000, tasks);
		var unlimited = EnergyScheduleTester
				.from(new TestEnergySchedulable(clock, config, null).getEnergyScheduleHandler());
		var periodIndex = findPeriodIndex(unlimited, //
				unlimited.goc.periods().getFirst().time().withHour(12).withMinute(0));
		assertTrue(unlimited.simulatePeriodIndex(periodIndex).ef().getManagedConsumption(COMPONENT_ID) > 1);

		var limited = EnergyScheduleTester.from(new TestEnergySchedulable(clock, config, 1).getEnergyScheduleHandler());
		assertEquals(1, limited.simulatePeriodIndex(periodIndex).ef().getManagedConsumption(COMPONENT_ID));
	}

	@Test
	void testZeroRemainingHeatEnergyPreventsHeatingConsumption() {
		final var clock = TestUtils.createDummyClock();

		var tasks = JSCalendar.Tasks.<HeatAskomaPayload>create(clock) //
				.add(t -> t //
						.setStart("00:00") //
						.setDuration(Duration.ofMinutes(15)) //
						.addRecurrenceRule(r -> r //
								.setFrequency(DAILY)) //
						.setPayload(new HeatAskomaPayload(Mode.FAST_HEAT))) //
				.build();

		var t = EnergyScheduleTester
				.from(new TestEnergySchedulable(clock, new EnergyScheduler.Config(Mode.OFF, 4000, tasks), 0)
						.getEnergyScheduleHandler());

		assertEquals(0, t.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
	}

	private static int findPeriodIndex(EnergyScheduleTester t, ZonedDateTime targetTime) {
		return IntStream.range(0, t.goc.periods().size()) //
				.filter(i -> t.goc.periods().get(i).time().equals(targetTime)) //
				.findFirst() //
				.orElseThrow(() -> new AssertionError("Period not found: " + targetTime));
	}

	private static class TestEnergySchedulable extends AbstractDummyEnergySchedulable<TestEnergySchedulable>
			implements EnergySchedulable, OpenemsComponent {

		private final EnergyScheduleHandler esh;

		public TestEnergySchedulable(Clock clock, EnergyScheduler.Config config, Integer remainingHeatEnergy) {
			super(FACTORY_ID, COMPONENT_ID, //
					OpenemsComponent.ChannelId.values(), //
					Heat.ChannelId.values(), //
					HeatAskoma.ChannelId.values());
			if (remainingHeatEnergy != null) {
				Channel<Integer> channel = this.channel(Heat.ChannelId.REMAINING_HEAT_ENERGY);
				channel.setNextValue(remainingHeatEnergy);
				channel.nextProcessImage();
			}
			this.esh = EnergyScheduler.buildEnergyScheduleHandler(this, () -> clock, () -> config);
		}

		@Override
		protected TestEnergySchedulable self() {
			return this;
		}

		@Override
		public EnergyScheduleHandler getEnergyScheduleHandler() {
			return this.esh;
		}
	}

}