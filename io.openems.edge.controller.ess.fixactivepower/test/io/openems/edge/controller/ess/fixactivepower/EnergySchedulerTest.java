package io.openems.edge.controller.ess.fixactivepower;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.openems.edge.controller.ess.fixactivepower.EnergyScheduler.ScheduleContext;
import io.openems.edge.controller.ess.fixactivepower.enums.Mode;
import io.openems.edge.controller.test.DummyController;
import io.openems.edge.energy.api.test.EnergyScheduleTester;

class EnergySchedulerTest {

	private static EnergyScheduleTester buildTester(Mode mode, int power, Integer targetSoc) {
		final var esh = EnergyScheduler.buildEnergyScheduleHandler(//
				new DummyController("ctrl0"), //
				() -> new EnergyScheduler.Config(mode, power, targetSoc));
		return EnergyScheduleTester.from(esh);
	}

	@Nested
	class ManualOnTest {

		@Test
		void shouldApplyConfiguredPower_whenModeIsManualOn() {
			final var tester = buildTester(Mode.MANUAL_ON, 2000, null);

			final var result = tester.simulatePeriod();

			// energy = 2000 W × 15 min = 500 Wh (discharge, positive)
			assertEquals(500, result.ef().setEss(99_999));
		}

		@Test
		void shouldDoNothing_whenConfigIsNull() {
			final var esh = EnergyScheduler.buildEnergyScheduleHandler(//
					new DummyController("ctrl0"), () -> null);

			final var result = EnergyScheduleTester.from(esh).simulatePeriod();

			// ef untouched -> max discharge available = 4000 Wh
			assertEquals(4000, result.ef().setEss(99_999));
		}
	}

	@Nested
	class ChargeOnceTest {

		@Test
		void shouldChargeWithFullPeriodEnergy_whenFarBelowTargetSoc() {
			// targetSoc = 90 % -> targetEssEnergy = 19800 Wh, remaining = 14800 Wh > 1000
			// Wh
			final var tester = buildTester(Mode.CHARGE_ONCE, 4000, 90);

			final var result = tester.simulatePeriod();

			assertEquals(-1000, result.ef().setEss(99_999));
		}

		@Test
		void shouldLimitChargeToRemainingEnergy_whenNearTargetSoc() {
			// targetSoc = 23% -> targetEssEnergy = 5060 Wh, remaining = 60Wh < 1000 Wh
			final var tester = buildTester(Mode.CHARGE_ONCE, 4000, 23);

			final var result = tester.simulatePeriod();

			assertEquals(-60, result.ef().setEss(99_999));
		}

		@Test
		void shouldNotChargeAndMarkTargetReached_whenAlreadyAtOrAboveTargetSoc() {
			// targetSoc = 22 % -> targetEssEnergy = 4840 Wh <= initialEnergy = 5000 Wh
			// -> remainingEnergy <= 0 -> markTargetReached(), return without setting ef
			final var tester = buildTester(Mode.CHARGE_ONCE, 4000, 22);

			final var result = tester.simulatePeriod();

			assertEquals(4000, result.ef().setEss(99_999)); // max discharge
		}

		@Test
		void shouldNotCharge_whenTargetAlreadyReachedInPreviousPeriod() {
			// 1st period: targetEssEnergy = 4840 Wh <= 5000 Wh -> markTargetReached
			// 2nd period: isTargetReached() = true -> immediate return
			final var tester = buildTester(Mode.CHARGE_ONCE, 4000, 22);

			tester.simulatePeriod();
			final var result = tester.simulatePeriod();

			assertEquals(4000, result.ef().setEss(99_999)); // max discharge
		}

		@Test
		void shouldChargeToFull_whenNoTargetSocConfigured() {
			// targetSoc = null → targetEssEnergy = 22000 Wh, remaining = 17 000 Wh
			final var tester = buildTester(Mode.CHARGE_ONCE, 4000, null);

			final var result = tester.simulatePeriod();

			assertEquals(-1000, result.ef().setEss(99_999));
		}
	}

	@Nested
	class DischargeOnceTest {

		@Test
		void shouldDischargeWithFullPeriodEnergy_whenFarAboveTargetSoc() {
			// targetSoc = 10 % -> targetEssEnergy = 2200 Wh, remaining = 2800 Wh > 1000 Wh
			final var tester = buildTester(Mode.DISCHARGE_ONCE, 4000, 10);

			final var result = tester.simulatePeriod();

			assertEquals(1000, result.ef().setEss(99_999));
		}

		@Test
		void shouldLimitDischargeToRemainingEnergy_whenNearTargetSoc() {
			// targetSoc = 22% -> targetEssEnergy = 4840 Wh, remaining = 160 Wh < 1000 Wh
			final var tester = buildTester(Mode.DISCHARGE_ONCE, 4000, 22);

			final var result = tester.simulatePeriod();

			assertEquals(160, result.ef().setEss(99_999));
		}

		@Test
		void shouldNotDischargeAndMarkTargetReached_whenAlreadyAtOrBelowTargetSoc() {
			// targetSoc = 23 % -> targetEssEnergy = 5060 Wh >= initialEnergy = 5000 Wh
			// -> remainingEnergy <= 0 -> markTargetReached(), return without setting ef
			final var tester = buildTester(Mode.DISCHARGE_ONCE, 4000, 23);

			final var result = tester.simulatePeriod();

			assertEquals(4000, result.ef().setEss(99_999)); // max discharge
		}

		@Test
		void shouldNotDischarge_whenTargetAlreadyReachedInPreviousPeriod() {
			// 1st period: targetEssEnergy = 5060 Wh >= 5000 Wh -> markTargetReached
			// 2nd period: isTargetReached() = true -> immediate return
			final var tester = buildTester(Mode.DISCHARGE_ONCE, 4000, 23);

			tester.simulatePeriod();
			final var result = tester.simulatePeriod();

			assertEquals(4000, result.ef().setEss(99_999)); // max discharge
		}

		@Test
		void shouldDischargeToEmpty_whenNoTargetSocConfigured() {
			// targetSoc = null -> targetEssEnergy = 0 Wh, remaining = 5000 Wh > 1000 Wh
			final var tester = buildTester(Mode.DISCHARGE_ONCE, 4000, null);

			final var result = tester.simulatePeriod();

			assertEquals(1000, result.ef().setEss(99_999));
		}
	}

	@Nested
	class ScheduleContextTest {

		@Test
		void shouldReturnFalse_whenCreated() {
			assertFalse(new ScheduleContext().isTargetReached());
		}

		@Test
		void shouldReturnTrue_whenMarkedReached() {
			final var scheduleContext = new ScheduleContext();

			scheduleContext.markTargetReached();

			assertTrue(scheduleContext.isTargetReached());
		}
	}
}