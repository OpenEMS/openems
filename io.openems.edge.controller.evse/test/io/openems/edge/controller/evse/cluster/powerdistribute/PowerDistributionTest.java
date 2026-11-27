package io.openems.edge.controller.evse.cluster.powerdistribute;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.controller.evse.single.Mode.FORCE;
import static io.openems.edge.controller.evse.single.Mode.MINIMUM;
import static io.openems.edge.controller.evse.single.Mode.SURPLUS;
import static io.openems.edge.controller.evse.single.Mode.ZERO;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.openems.edge.controller.evse.cluster.DistributionStrategy;
import io.openems.edge.controller.evse.cluster.powerdistribute.ramp.LimitIncreaseByPercentageRamp;
import io.openems.edge.controller.evse.single.EvseSingleState;
import io.openems.edge.controller.evse.single.PhaseSwitching;
import io.openems.edge.controller.evse.single.Types;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection;
import io.openems.edge.evse.api.common.ApplySetPoint;

class PowerDistributionTest {

	@Test
	void test1() {
		var ct = CalculateTester.generateControllers(5); //
		ct.clock.leap(500, ChronoUnit.MILLIS);

		ct //
				.set(1, 2, 3, c -> c //
						.setMode(SURPLUS)) //
				.set(1, c -> c //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Ampere(THREE_PHASE, 6, 16)))) //
				.sum(s -> s //
						.withGridActivePower(-32000)); //
		var sut = ct.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(0, sut.get(0).getApplySetPointInMilliAmpere());
		assertEquals(15, sut.get(1).getApplySetPointInAmpere());
		assertEquals(15916, sut.get(2).getApplySetPointInMilliAmpere());
		assertEquals(15458, sut.get(3).getApplySetPointInMilliAmpere());
		assertEquals(0, sut.get(4).getApplySetPointInMilliAmpere());

		assertArrayEquals(new int[] { 0, 15, 15916, 15458, 0 }, sut.getApplySetPoints());
	}

	@Test
	void test2() {
		var sut = CalculateTester.generateControllers(5) //
				.set(0, 4, c -> c //
						.setMode(FORCE)) //
				.set(1, 2, 3, c -> c //
						.setMode(SURPLUS)) //
				.sum(s -> s //
						.withGridActivePower(-27000)) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertArrayEquals(new int[] { 16000, 7130, 0, 0, 16000 }, sut.getApplySetPoints());
	}

	@Test
	void testRamp() {
		var ct = CalculateTester.generateControllers(5);

		ct //
				.set(1, 2, 3, c -> c //
						.setMode(SURPLUS)) //
				.set(1, c -> c //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Ampere(THREE_PHASE, 6, 16)))) //
				.sum(s -> s //
						.withGridActivePower(-32000)) //
				.setRamp(new LimitIncreaseByPercentageRamp(
						EvsePowerDistributionManager.MAX_PERCENTAGE_CHANGE_PER_SECOND, ct.clock)); //
		var sut = ct.execute(DistributionStrategy.EQUAL_POWER);

		assertArrayEquals(new int[] { 0, 6, 6000, 6000, 0 }, sut.getApplySetPoints());

		ct.ramp.setLastSetPoint(new PowerDistributionKey.Evse(sut.get(2).entry().ctrl().id()), 10000);
		ct.clock.leap(1000, ChronoUnit.MILLIS);

		// test initial ramp values

		sut = ct.execute(DistributionStrategy.EQUAL_POWER);
		assertArrayEquals(new int[] { 0, 6, 14928, 6181, 0 }, sut.getApplySetPoints());

		// test finished ramp values

		ct.clock.leap(50, SECONDS);
		sut = ct.execute(DistributionStrategy.EQUAL_POWER);
		assertArrayEquals(new int[] { 0, 15, 15916, 15454, 0 }, sut.getApplySetPoints());
	}

	@Test
	void testMinimumWithSurplus() {
		var sut = CalculateTester.generateControllers(3) //
				.set(0, c -> c //
						.setMode(FORCE)) //
				.set(1, 2, c -> c.setMode(MINIMUM)) //
				.sum(s -> s //
						.withGridActivePower(-27000)) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertArrayEquals(new int[] { 16000, 11565, 11565 }, sut.getApplySetPoints());
	}

	@Test
	void testMinimumWithoutSurplus() {
		var sut = CalculateTester.generateControllers(4) //
				.set(0, 3, c -> c //
						.setMode(FORCE)) //
				.set(1, 2, c -> c.setMode(MINIMUM)) //
				.sum(s -> s //
						.withGridActivePower(0)) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertArrayEquals(new int[] { 16000, 6000, 6000, 16000 }, sut.getApplySetPoints());
	}

	@Test
	void test3() {
		var sut = CalculateTester.generateControllers(5) //
				.set(1, c -> c //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Ampere(THREE_PHASE, 6, 16)))) //
				.set(2, c -> c //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)))) //
				.set(3, c -> c //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1000, 5000)))) //
				.setAll(c -> c //
						.setMode(MINIMUM)) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertArrayEquals(new int[] { 6000, 6, 6000, 1380, 6000 }, sut.getApplySetPoints());
	}

	@Test
	void test4() {
		var sut = CalculateTester.generateControllers(5) //
				.set(1, c -> c //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Ampere(THREE_PHASE, 6, 16)))) //
				.set(2, c -> c //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)))) //
				.set(3, c -> c //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1000, 5000)))) //
				.setAll(c -> c //
						.setMode(FORCE)) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertArrayEquals(new int[] { 16000, 16, 16000, 5000, 16000 }, sut.getApplySetPoints());
	}

	@Test
	void test5() {
		var sut = CalculateTester.generateControllers(5) //
				.set(0, c -> c //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Ampere(SINGLE_PHASE, 6, 16)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true))) //
				.setAll(c -> c //
						.setMode(FORCE) //
						.setPhaseSwitching(PhaseSwitching.FORCE_THREE_PHASE)) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(PhaseSwitchDirection.TO_THREE_PHASE, sut.get(0).getPhaseSwitchDirection());
		assertNull(sut.get(1).getPhaseSwitchDirection());
	}

	@Test
	void test6() {
		final var history = new Types.History();
		var sut = CalculateTester.generateControllers(5) //
				.set(0, c -> c //
						.setMode(SURPLUS) //
						.setChargePointAbilities(cp -> cp //
								.setIsReadyForCharging(false))) //
				.set(1, c -> c //
						.setMode(FORCE) //
						.setChargePointAbilities(cp -> cp //
								.setIsReadyForCharging(false))) //
				.set(2, c -> c //
						.setMode(ZERO) // zero stays zero
						.setChargePointAbilities(cp -> cp //
								.setIsReadyForCharging(false))) //
				.set(3, c -> c //
						.setMode(MINIMUM) //
						.setHistory(history))
				.set(4, c -> c //
						.setMode(FORCE)) // not-limited
				.execute(DistributionStrategy.EQUAL_POWER);

		assertArrayEquals(new int[] { 0, 0, 0, 6000, 16000 }, sut.getApplySetPoints());
	}

	@Test
	void test7() {
		var sut = CalculateTester.generateControllers(2) //
				.sum(s -> s //
						.withGridActivePower(-29000)) //
				.set(0, c -> c //
						.setMode(SURPLUS) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 32000)) //
								.setIsReadyForCharging(true)) //
						.setElectricVehicleAbilities(a -> a //
								.setThreePhaseLimitInMilliAmpere(6000, 32000))) //
				.set(1, c -> c //
						.setMode(SURPLUS) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(SINGLE_PHASE, 6000, 32000)) //
								.setIsReadyForCharging(true))) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertArrayEquals(new int[] { 31362, 32000 }, sut.getApplySetPoints());
	}

	@Test
	void test8() {
		var sut = CalculateTester.generateControllers(2) //
				.sum(s -> s //
						.withGridActivePower(-29000)) //
				.set(0, c -> c //
						.setMode(SURPLUS) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(SINGLE_PHASE, 6000, 32000)) //
								.setIsReadyForCharging(true))) //
				.set(1, c -> c //
						.setMode(SURPLUS) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 32000)) //
								.setIsReadyForCharging(true)) //
						.setElectricVehicleAbilities(a -> a //
								.setThreePhaseLimitInMilliAmpere(6000, 32000))) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertArrayEquals(new int[] { 32000, 31362 }, sut.getApplySetPoints());
	}

	@Nested
	@DisplayName("testDelays()")
	class TestDelays {

		@Test
		void initialSurplusShouldCharge() {
			final var history = new Types.History();

			var sut = CalculateTester.generateControllers(1) //
					.sum(s -> s //
							.withGridActivePower(-500)) //
					.set(0, c -> c //
							.setMode(SURPLUS) //
							.setState(EvseSingleState.CHARGING) //
							.setHistory(history));

			var execution = sut.execute(DistributionStrategy.EQUAL_POWER);
			assertNotEquals(0, execution.get(0).getApplySetPointInMilliAmpere());
		}

		@Test
		void shouldNotDirectlyStopSurplusCharge() {
			final var history = new Types.History();

			var sut = CalculateTester.generateControllers(1) //
					.sum(s -> s //
							.withGridActivePower(-500)) //
					.set(0, c -> c //
							.setMode(SURPLUS) //
							.setState(EvseSingleState.CHARGING) //
							.setHistory(history));

			for (int i = 0; i < 30; i++) {
				history.addEntry(sut.clock.instant(), 5000, 5000, 0, true);
				sut.clock.leap(1, SECONDS);
			}

			var execution = sut.execute(DistributionStrategy.EQUAL_POWER);
			assertNotEquals(0, execution.get(0).getApplySetPointInMilliAmpere());
		}

		@Test
		void shouldStopSurplusChargeAfterOneMinute() {
			final var history = new Types.History();

			var sut = CalculateTester.generateControllers(1) //
					.sum(s -> s //
							.withGridActivePower(-500)) //
					.set(0, c -> c //
							.setMode(SURPLUS) //
							.setState(EvseSingleState.CHARGING) //
							.setHistory(history));

			for (int i = 0; i < 60; i++) {
				history.addEntry(sut.clock.instant(), 5000, 5000, 0, true);
				sut.clock.leap(1, SECONDS);
			}

			var execution = sut.execute(DistributionStrategy.EQUAL_POWER);
			assertEquals(0, execution.get(0).getApplySetPointInMilliAmpere());
		}

		@Test
		void shouldNotDirectlyStartSurplusCharge() {
			final var history = new Types.History();

			var sut = CalculateTester.generateControllers(1) //
					.sum(s -> s //
							.withGridActivePower(-5000)) //
					.set(0, c -> c //
							.setMode(SURPLUS) //
							.setState(EvseSingleState.CHARGE_PAUSED) //
							.setHistory(history));

			var execution = sut.execute(DistributionStrategy.EQUAL_POWER);
			assertEquals(0, execution.get(0).getApplySetPointInMilliAmpere());
		}

		@Test
		void shouldStartSurplusChargeAfterSomeTime() {
			final var history = new Types.History();

			var sut = CalculateTester.generateControllers(1) //
					.sum(s -> s //
							.withGridActivePower(-5000)) //
					.set(0, c -> c //
							.setMode(SURPLUS) //
							.setState(EvseSingleState.CHARGE_PAUSED) //
							.setHistory(history));

			for (int i = 0; i < 30; i++) {
				history.addEntry(sut.clock.instant(), 0, 0, 5000, true);
				sut.clock.leap(1, SECONDS);
			}

			var execution = sut.execute(DistributionStrategy.EQUAL_POWER);
			assertNotEquals(0, execution.get(0).getApplySetPointInMilliAmpere());
		}
	}
}
