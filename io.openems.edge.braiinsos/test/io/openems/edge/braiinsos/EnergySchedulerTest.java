package io.openems.edge.braiinsos;

import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.DAILY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.test.TestUtils;
import io.openems.edge.energy.api.test.DummyEnergySchedulable;
import io.openems.edge.energy.api.test.EnergyScheduleTester;

class EnergySchedulerTest {

	private static final String FACTORY_ID = "Controller.BraiinsOS.Single";
	private static final String COMPONENT_ID = "ctrlBraiinsSingle0";
	private static final int CONSUMPTION_W = 3000; // [W]
	private static final Duration PERIOD_DURATION = Duration.ofMinutes(15);

	private static EnergyScheduleTester buildTester(Clock clock, Mode defaultMode, int consumptionW,
			JSCalendar.Tasks<Payload> tasks) {
		var energySchedulable = new DummyEnergySchedulable<>(FACTORY_ID, COMPONENT_ID,
				cmp -> EnergyScheduler.buildEnergyScheduleHandler(cmp, () -> clock,
						() -> new EnergyScheduler.Config(defaultMode, consumptionW, tasks)));

		return EnergyScheduleTester.from(energySchedulable.getEnergyScheduleHandler());
	}

	private static JSCalendar.Tasks<Payload> createDailyManualTask(Clock clock, String startTime, Mode mode) {
		return JSCalendar.Tasks.<Payload>create(clock)//
				.add(t -> t//
						.setStart(startTime)//
						.setDuration(PERIOD_DURATION)//
						.addRecurrenceRule(r -> r//
								.setFrequency(DAILY))//
						.setPayload(new Payload.Manual(mode)))//
				.build();
	}

	@Nested
	class DefaultModeTest {

		@Test
		void shouldApplyDefaultOn_whenNoTaskIsActive() {
			final var clock = TestUtils.createDummyClock();
			final var tester = buildTester(clock, Mode.ON, CONSUMPTION_W, JSCalendar.Tasks.empty());

			final var result = tester.simulatePeriod();

			// 3000 W for 15 minutes -> 750 Wh
			assertEquals(750, result.ef().getManagedConsumption(COMPONENT_ID));
		}

		@Test
		void shouldApplyDefaultOff_whenNoTaskIsActive() {
			final var clock = TestUtils.createDummyClock();
			final var tester = buildTester(clock, Mode.OFF, CONSUMPTION_W, JSCalendar.Tasks.empty());

			final var result = tester.simulatePeriod();

			assertEquals(0, result.ef().getManagedConsumption(COMPONENT_ID));
		}

		@Test
		void shouldClampNegativeConsumptionToZero_whenModeIsOn() {
			final var clock = TestUtils.createDummyClock();
			final var tester = buildTester(clock, Mode.ON, -1, JSCalendar.Tasks.empty());

			final var result = tester.simulatePeriod();

			assertEquals(0, result.ef().getManagedConsumption(COMPONENT_ID));
		}
	}

	@Nested
	class ManualTaskTest {

		@Test
		void shouldOverrideDefaultOffWithOnTask_whenTaskIsActive() {
			final var clock = TestUtils.createDummyClock();
			final var tasks = createDailyManualTask(clock, "00:00", Mode.ON);
			final var tester = buildTester(clock, Mode.OFF, CONSUMPTION_W, tasks);

			final var result = tester.simulatePeriod();

			assertEquals(750, result.ef().getManagedConsumption(COMPONENT_ID));
		}

		@Test
		void shouldOverrideDefaultOnWithOffTask_whenTaskIsActive() {
			final var clock = TestUtils.createDummyClock();
			final var tasks = createDailyManualTask(clock, "00:00", Mode.OFF);
			final var tester = buildTester(clock, Mode.ON, CONSUMPTION_W, tasks);

			final var result = tester.simulatePeriod();

			assertEquals(0, result.ef().getManagedConsumption(COMPONENT_ID));
		}

		@Test
		void shouldApplyTaskOnlyInItsPeriod_whenTaskStartsLater() {
			final var clock = TestUtils.createDummyClock();
			final var tasks = createDailyManualTask(clock, "00:15", Mode.ON);
			final var tester = buildTester(clock, Mode.OFF, CONSUMPTION_W, tasks);

			assertEquals(0, tester.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
			assertEquals(750, tester.simulatePeriod().ef().getManagedConsumption(COMPONENT_ID));
		}
	}

	@Nested
	class NullConfigTest {

		@Test
		void shouldNotAddManagedConsumption_whenConfigIsNull() {
			final var clock = TestUtils.createDummyClock();
			final var energySchedulable = new DummyEnergySchedulable<>(FACTORY_ID, COMPONENT_ID,
					cmp -> EnergyScheduler.buildEnergyScheduleHandler(cmp, () -> clock, () -> null));

			final var result = EnergyScheduleTester.from(energySchedulable.getEnergyScheduleHandler()).simulatePeriod();

			assertEquals(0, result.ef().getManagedConsumption(COMPONENT_ID));
		}
	}
}
