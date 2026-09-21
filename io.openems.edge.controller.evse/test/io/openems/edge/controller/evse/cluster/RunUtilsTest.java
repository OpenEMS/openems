package io.openems.edge.controller.evse.cluster;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.controller.evse.cluster.RunUtils.calculate;
import static io.openems.edge.controller.evse.cluster.RunUtils.findFirstEntryWithSameSetPoint;
import static io.openems.edge.controller.evse.single.Mode.FORCE;
import static io.openems.edge.controller.evse.single.Mode.MINIMUM;
import static io.openems.edge.controller.evse.single.Mode.SURPLUS;
import static io.openems.edge.controller.evse.single.Mode.ZERO;
import static io.openems.edge.controller.evse.single.PhaseSwitching.DISABLE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.controller.evse.TestUtils;
import io.openems.edge.controller.evse.TestUtils.CtrlBuilder;
import io.openems.edge.controller.evse.cluster.RunUtils.PowerDistribution;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.controller.evse.single.Mode;
import io.openems.edge.controller.evse.single.PhaseSwitching;
import io.openems.edge.controller.evse.single.Types.History;
import io.openems.edge.energy.api.handler.DifferentModes.Modes.JointModes;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection;
import io.openems.edge.evse.api.common.ApplySetPoint;

class RunUtilsTest {

	protected static class CalculateTester {

		public static CalculateTester generateControllers(int count) {
			final var clock = createDummyClock();

			// Add History with high value to tick Utils::applyChangeLimit.
			final var history = new History();
			history.addEntry(Instant.now(clock), null, 22000 /* [W] */, null, false);
			clock.leap(500, ChronoUnit.MILLIS);

			return new CalculateTester(clock, IntStream.range(0, count) //
					.<CtrlBuilder>mapToObj(i -> TestUtils.createSingleCtrl() //
							.setCtrlSingleId("ctrlEvseSingle" + i) //
							.setChargePointId("evseChargePoint" + i) //
							.setMode(ZERO) //
							.setActivePower(0) //
							.setHistory(history) //
							.setPhaseSwitching(DISABLE) //
							.setChargePointAbilities(a -> a //
									.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)) //
									.setIsReadyForCharging(true)) //
							.setElectricVehicleAbilities(a -> a //
									.setSinglePhaseLimitInMilliAmpere(6000, 32000) //
									.setThreePhaseLimitInMilliAmpere(6000, 16000)) //
							.setCombinedAbilities(a -> a //
									.setIsReadyForCharging(true)))
					.toList());
		}

		public final TimeLeapClock clock;

		private final List<CtrlBuilder> ctrls;
		private final DummySum sum = new DummySum();

		private CalculateTester(TimeLeapClock clock, List<CtrlBuilder> ctrls) {
			this.clock = clock;
			this.ctrls = ctrls;
		}

		protected CalculateTester set(int a, Consumer<CtrlBuilder> callback) {
			return this.set(new int[] { a }, callback);
		}

		protected CalculateTester set(int a, int b, Consumer<CtrlBuilder> callback) {
			return this.set(new int[] { a, b }, callback);
		}

		protected CalculateTester set(int a, int b, int c, Consumer<CtrlBuilder> callback) {
			return this.set(new int[] { a, b, c }, callback);
		}

		protected CalculateTester set(int[] indexes, Consumer<CtrlBuilder> callback) {
			for (var i : indexes) {
				callback.accept(this.ctrls.get(i));
			}
			return this;
		}

		protected CalculateTester setAll(Consumer<CtrlBuilder> callback) {
			for (var ctrl : this.ctrls) {
				callback.accept(ctrl);
			}
			return this;
		}

		protected CalculateTester sum(Consumer<DummySum> sum) {
			sum.accept(this.sum);
			return this;
		}

		protected PowerDistributionTester execute(DistributionStrategy distributionStrategy) {
			return new PowerDistributionTester(calculate(//
					this.clock, //
					distributionStrategy, //
					this.sum, //
					this.ctrls.stream() //
							.<ControllerEvseSingle>map(CtrlBuilder::build) //
							.toList(), //
					new JointModes.JointMode<Mode>(ImmutableMap.of(), true, null), //
					LogVerbosity.NONE, log -> doNothing()));
		}

		protected static record PowerDistributionTester(PowerDistribution powerDistribution) {
			protected static record EntryTester(PowerDistribution.Entry entry) {
				protected int getSetPointInWatt() {
					return this.entry.setPointInWatt;
				}

				protected int getApplySetPointInMilliAmpere() {
					return ((ApplySetPoint.Action.MilliAmpere) this.entry.actions.build().applySetPoint()).value();
				}

				protected int getApplySetPointInAmpere() {
					return ((ApplySetPoint.Action.Ampere) this.entry.actions.build().applySetPoint()).value();
				}

				protected int getApplySetPointInWatt() {
					return ((ApplySetPoint.Action.Watt) this.entry.actions.build().applySetPoint()).value();
				}

				protected Integer getPvLimitInWatt() {
					return this.entry.actions.build().setPointWithoutPhaseLimitation();
				}

				protected Long getProbableNextPhaseSwitchEpochSeconds() {
					return (Long) this.entry.ctrl
							.channel(ControllerEvseSingle.ChannelId.PROBABLE_NEXT_PHASE_SWITCH_EPOCH_SECONDS)
							.getNextValue().get();
				}

				protected PhaseSwitchDirection getPhaseSwitchDirection() {
					final var phaseSwitch = this.entry.actions.build().phaseSwitch();
					if (phaseSwitch == null) {
						return null;
					}

					return phaseSwitch.direction();
				}
			}

			protected EntryTester get(int i) {
				return new EntryTester(this.powerDistribution.entries.get(i));
			}

			protected int[] getApplySetPoints() {
				return this.powerDistribution.entries.stream() //
						.mapToInt(e -> e.actions.build().applySetPoint().value()) //
						.toArray();
			}
		}
	}

	@Test
	void test1() {
		var ct = CalculateTester.generateControllers(5); //
		final var history = new History();
		history.addEntry(Instant.now(ct.clock), null, 10000 /* [W] */, null, false);
		ct.clock.leap(500, ChronoUnit.MILLIS);

		ct //
				.set(1, 2, 3, c -> c //
						.setMode(SURPLUS)) //
				.set(1, c -> c //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Ampere(THREE_PHASE, 6, 16)))) //
				.set(2, c -> c //
						.setHistory(history)) //
				.sum(s -> s //
						.withGridActivePower(-32000)); //
		var sut = ct.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(0, sut.get(0).getApplySetPointInMilliAmpere());
		assertEquals(15, sut.get(1).getApplySetPointInAmpere());
		assertEquals(14710, sut.get(2).getApplySetPointInMilliAmpere());
		assertEquals(15458, sut.get(3).getApplySetPointInMilliAmpere());
		assertEquals(0, sut.get(4).getApplySetPointInMilliAmpere());

		assertArrayEquals(new int[] { 0, 15, 14710, 15458, 0 }, sut.getApplySetPoints());

		// #2 apply ramp on [2]

		history.addEntry(Instant.now(ct.clock), null, 10150 /* [W] */, null, false);
		ct.clock.leap(1000, ChronoUnit.MILLIS);
		sut = ct.execute(DistributionStrategy.EQUAL_POWER);
		assertArrayEquals(new int[] { 0, 15, 15152, 15458, 0 }, sut.getApplySetPoints());

		// #3 apply ramp on [2]

		history.addEntry(Instant.now(ct.clock), null, 10455 /* [W] */, null, false);
		ct.clock.leap(1000, ChronoUnit.MILLIS);
		sut = ct.execute(DistributionStrategy.EQUAL_POWER);
		assertArrayEquals(new int[] { 0, 15, 15607, 15458, 0 }, sut.getApplySetPoints());

		// #4 finished ramp on [2]

		history.addEntry(Instant.now(ct.clock), null, 10769 /* [W] */, null, false);
		ct.clock.leap(1000, ChronoUnit.MILLIS);
		sut = ct.execute(DistributionStrategy.EQUAL_POWER);
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

		assertArrayEquals(new int[] { 16000, 6000, 6000, 6000, 16000 }, sut.getApplySetPoints());
	}

	@Test
	void testMinimumWithSurplus() {
		var sut = CalculateTester.generateControllers(3) //
				.set(0, c -> c //
						.setMode(FORCE)) //
				.set(1, 2, c -> c //
						.setMode(MINIMUM)) //
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
				.set(1, 2, c -> c //
						.setMode(MINIMUM)) //
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
		var sut = CalculateTester.generateControllers(2) //
				.set(0, c -> c //
						.setMode(FORCE) //
						.setActivePower(123) //
						.setSessionEnergy(1000)) //
				.set(1, c -> c //
						.setMode(FORCE) //
						.setActivePower(456) //
						.setSessionEnergy(2000)) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertTrue(sut.powerDistribution.toString().startsWith("PowerDistribution{totalActivePower=579, entries="));
	}

	@Test
	void testAutomaticPhaseSwitchToSingleAtLowSurplus() {
		final var history = new History();
		var ct = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-1380)) //
				.set(0, c -> c //
						.setMode(SURPLUS) //
						.setHistory(history) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_SINGLE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInWatt(4140, 11040)));

		executeAutomaticPhaseSwitchWarmup(ct, history, 0);
		var sut = ct.execute(DistributionStrategy.EQUAL_POWER);

		// Switch happens after enough pvLimit samples are present in the rolling
		// window.
		// During the switching cycle the 3p charger keeps charging at 3p-min.
		assertEquals(4140, sut.get(0).getSetPointInWatt());
		assertEquals(PhaseSwitchDirection.TO_SINGLE_PHASE, sut.get(0).getPhaseSwitchDirection());
	}

	@Test
	void testZeroModeNeverInitiatesPhaseSwitch() {
		var sut = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-8000)) //
				.set(0, c -> c //
						.setMode(ZERO) //
						.setPhaseSwitching(PhaseSwitching.FORCE_THREE_PHASE) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 7360)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInWatt(4140, 11040))) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(0, sut.get(0).getSetPointInWatt());
		assertNull(sut.get(0).getPhaseSwitchDirection());
	}

	@Test
	void testAutomaticPhaseSwitchToSingleKeepsThreePhaseAboveThreePhaseMinimum() {
		var sut = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-5000)) //
				.set(0, c -> c //
						.setMode(SURPLUS) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_SINGLE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInWatt(4140, 11040))) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(5000, sut.get(0).getSetPointInWatt());
		assertNull(sut.get(0).getPhaseSwitchDirection());
	}

	@Test
	void testAutomaticPhaseSwitchToThreeAtSinglePhaseMaximum() {
		final var history = new History();
		var ct = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-8000)) //
				.set(0, c -> c //
						.setMode(SURPLUS) //
						.setHistory(history) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 7360)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInWatt(4140, 11040)));

		executeAutomaticPhaseSwitchWarmup(ct, history, 0);
		var sut = ct.execute(DistributionStrategy.EQUAL_POWER);

		// Switch happens after enough pvLimit samples are present in the rolling
		// window.
		assertEquals(7360, sut.get(0).getSetPointInWatt());
		assertEquals(PhaseSwitchDirection.TO_THREE_PHASE, sut.get(0).getPhaseSwitchDirection());
		assertNull(sut.get(0).getProbableNextPhaseSwitchEpochSeconds());
	}

	@Test
	void testAutomaticPhaseSwitchToThreeAtSinglePhaseMaximumInMinimumMode() {
		final var history = new History();
		var ct = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-8000)) //
				.set(0, c -> c //
						.setMode(MINIMUM) //
						.setHistory(history) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 7360)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInWatt(4140, 11040)));

		executeAutomaticPhaseSwitchWarmup(ct, history, 0);
		var sut = ct.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(7360, sut.get(0).getSetPointInWatt());
		assertEquals(PhaseSwitchDirection.TO_THREE_PHASE, sut.get(0).getPhaseSwitchDirection());
		assertNull(sut.get(0).getProbableNextPhaseSwitchEpochSeconds());
	}

	@Test
	void testAutomaticMinimumUsesRawSurplusAboveSinglePhaseMinimum() {
		var sut = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-3000)) //
				.set(0, c -> c //
						.setMode(MINIMUM) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 7360)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInWatt(4140, 11040))) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(3000, sut.get(0).getSetPointInWatt());
		assertEquals(Integer.valueOf(3000), sut.get(0).getPvLimitInWatt());
		assertNull(sut.get(0).getPhaseSwitchDirection());
	}

	@Test
	void testAutomaticMinimumUsesSinglePhaseMinimumBelowSinglePhaseMinimum() {
		var sut = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-1000)) //
				.set(0, c -> c //
						.setMode(MINIMUM) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 7360)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInWatt(4140, 11040))) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(1380, sut.get(0).getSetPointInWatt());
		assertEquals(Integer.valueOf(1000), sut.get(0).getPvLimitInWatt());
		assertNull(sut.get(0).getPhaseSwitchDirection());
	}

	@Test
	void testAutomaticMinimumKeepsRawPvLimitAndThreePhaseMinimumSetPointBelowThreePhaseMinimum() {
		final var history = new History();
		var sut = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-3200)) //
				.set(0, c -> c //
						.setMode(MINIMUM) //
						.setHistory(history) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_SINGLE_PHASE) //
								.setIsReadyForCharging(true)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInMilliAmpere(6000, 16000))) //
				.execute(DistributionStrategy.EQUAL_POWER);

		// pvLimit(3200) is above single-phase minimum, so MINIMUM uses the regular
		// 90avg path instead of switching immediately. While still in 3p, it keeps the
		// three-phase minimum setpoint.
		assertEquals(4140, sut.get(0).getSetPointInWatt());
		assertEquals(Integer.valueOf(3200), sut.get(0).getPvLimitInWatt());
		assertNull(sut.get(0).getPhaseSwitchDirection());
	}

	@Test
	void testAutomaticMinimumSwitchesFromThreePhaseToSinglePhaseWhenRawPvLimitSupportsSinglePhase() {
		final var history = new History();
		var ct = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-2000)) //
				.set(0, c -> c //
						.setMode(MINIMUM) //
						.setHistory(history) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_SINGLE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInWatt(4140, 11040))) //
		;

		executeAutomaticPhaseSwitchWarmup(ct, history, 0);
		var sut = ct.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(4140, sut.get(0).getSetPointInWatt());
		assertEquals(Integer.valueOf(2000), sut.get(0).getPvLimitInWatt());
		assertEquals(PhaseSwitchDirection.TO_SINGLE_PHASE, sut.get(0).getPhaseSwitchDirection());
	}

	@Test
	void testAutomaticMinimumThreeToSinglePhaseImmediateSwitchBoundaryAtSinglePhaseMinimum() {
		var belowSinglePhaseMinimum = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-1379)) //
				.set(0, c -> c //
						.setMode(MINIMUM) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_SINGLE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInWatt(4140, 11040))) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(4140, belowSinglePhaseMinimum.get(0).getSetPointInWatt());
		assertEquals(Integer.valueOf(1379), belowSinglePhaseMinimum.get(0).getPvLimitInWatt());
		assertEquals(PhaseSwitchDirection.TO_SINGLE_PHASE, belowSinglePhaseMinimum.get(0).getPhaseSwitchDirection());

		final var history = new History();
		var atSinglePhaseMinimum = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-1380)) //
				.set(0, c -> c //
						.setMode(MINIMUM) //
						.setHistory(history) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_SINGLE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInWatt(4140, 11040))) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(4140, atSinglePhaseMinimum.get(0).getSetPointInWatt());
		assertEquals(Integer.valueOf(1380), atSinglePhaseMinimum.get(0).getPvLimitInWatt());
		assertNull(atSinglePhaseMinimum.get(0).getPhaseSwitchDirection());
	}

	/**
	 * MINIMUM mode must always switch from three-phase to single-phase even when
	 * there is no PV at all (pvLimit=0). MINIMUM mode is explicitly allowed to draw
	 * from the grid at minimum power, so no history build-up is needed.
	 */
	@Test
	void testAutomaticMinimumSwitchesFromThreePhaseToSinglePhaseWithoutPv() {
		// No PV: gridActivePower=0 → pvLimit=0 < singlePhaseMin(1380): immediate switch
		var sut = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(0)) // no PV
				.set(0, c -> c //
						.setMode(MINIMUM) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_SINGLE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInWatt(4140, 11040))) //
				.execute(DistributionStrategy.EQUAL_POWER);

		// While phase switch is pending (charger still 3p), MINIMUM keeps 3p minimum.
		assertEquals(4140, sut.get(0).getSetPointInWatt());
		// Switch to single-phase happens immediately so next cycle delivers
		// single-phase minimum
		assertEquals(PhaseSwitchDirection.TO_SINGLE_PHASE, sut.get(0).getPhaseSwitchDirection());
	}

	@Test
	void testAutomaticPhaseSwitchToSingleSkipsSwitchWhenRecentWindowCannotSustainSinglePhase() {
		final var history = new History();
		var ct = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-1000)) //
				.set(0, c -> c //
						.setMode(SURPLUS) //
						.setHistory(history) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_SINGLE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInWatt(4140, 11040)));

		executeAutomaticPhaseSwitchWarmup(ct, history, 0);
		var sut = ct.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(0, sut.get(0).getSetPointInWatt());
		assertEquals(Integer.valueOf(1000), sut.get(0).getPvLimitInWatt());
		assertNull(sut.get(0).getPhaseSwitchDirection());
		assertNull(sut.get(0).getProbableNextPhaseSwitchEpochSeconds());
	}

	@Test
	void testAutomaticPhaseSwitchToThreeStaysSingleBelowSinglePhaseMaximum() {
		var sut = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-3700)) //
				.set(0, c -> c //
						.setMode(SURPLUS) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 7360)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInWatt(4140, 11040))) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(3700, sut.get(0).getSetPointInWatt());
		assertNull(sut.get(0).getPhaseSwitchDirection());
	}

	@Test
	void testAutomaticPhaseSwitchToThreeSwitchesAtConfiguredThreshold() {
		final var history = new History();
		var ct = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-4100)) //
				.set(0, c -> c //
						.setMode(SURPLUS) //
						.setHistory(history) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 7360)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInWatt(4140, 11040)));

		executeAutomaticPhaseSwitchWarmup(ct, history, 0);
		var sut = ct.execute(DistributionStrategy.EQUAL_POWER);

		// Switch happens after enough pvLimit samples are present in the rolling
		// window.
		assertEquals(4100, sut.get(0).getSetPointInWatt());
		assertEquals(PhaseSwitchDirection.TO_THREE_PHASE, sut.get(0).getPhaseSwitchDirection());
	}

	@Test
	void testAutomaticPhaseSwitchToThreeWithAmpereSetPointAbility() {
		final var history = new History();
		var ct = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-8000)) //
				.set(0, c -> c //
						.setMode(SURPLUS) //
						.setHistory(history) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Ampere(SINGLE_PHASE, 6, 32)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInWatt(4140, 11040)));

		executeAutomaticPhaseSwitchWarmup(ct, history, 0);
		var sut = ct.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(7360, sut.get(0).getSetPointInWatt());
		assertEquals(32, sut.get(0).getApplySetPointInAmpere());
		assertEquals(PhaseSwitchDirection.TO_THREE_PHASE, sut.get(0).getPhaseSwitchDirection());
	}

	@Test
	void testAutomaticPhaseSwitchCooldownBlocksImmediateReswitch() {
		final var history = new History();
		var ct = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-8000)) //
				.set(0, c -> c //
						.setMode(SURPLUS) //
						.setHistory(history) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 7360)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInWatt(4140, 11040)));

		executeAutomaticPhaseSwitchWarmup(ct, history, 0);
		var firstSwitch = ct.execute(DistributionStrategy.EQUAL_POWER);
		appendHistoryEntry(ct, history, firstSwitch, 0);
		ct.clock.leap(1, ChronoUnit.SECONDS);
		var second = ct.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(PhaseSwitchDirection.TO_THREE_PHASE, firstSwitch.get(0).getPhaseSwitchDirection());
		assertNull(second.get(0).getPhaseSwitchDirection());
	}

	@Test
	void testAutomaticPhaseSwitchCooldownRespectsCurrentSinglePhaseMaximum() {
		final var history = new History();
		var ct = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-8000)) //
				.set(0, c -> c //
						.setMode(SURPLUS) //
						.setHistory(history) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 7360)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInWatt(4140, 11040)));

		executeAutomaticPhaseSwitchWarmup(ct, history, 0);
		var firstSwitch = ct.execute(DistributionStrategy.EQUAL_POWER);
		appendHistoryEntry(ct, history, firstSwitch, 0);
		ct.clock.leap(1, ChronoUnit.SECONDS);
		var second = ct.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(PhaseSwitchDirection.TO_THREE_PHASE, firstSwitch.get(0).getPhaseSwitchDirection());
		assertEquals(7360, second.get(0).getSetPointInWatt());
		assertNull(second.get(0).getPhaseSwitchDirection());
	}

	@Test
	void testAutomaticPhaseSwitchProbableTimestampSetAndNotOverwritten() {
		final var history = new History();
		final var presetEpochSeconds = 2_000_000_000L;

		var ct = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-8000));
		ct.set(0, c -> c //
				.setMode(SURPLUS) //
				.setHistory(history) //
				.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
				.setProbableNextPhaseSwitchEpochSeconds(presetEpochSeconds) //
				.setChargePointAbilities(cp -> cp //
						.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 7360)) //
						.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE)) //
				.setElectricVehicleAbilities(ev -> ev //
						.setCanInterrupt(true) //
						.setSinglePhaseLimitInWatt(1380, 7360) //
						.setThreePhaseLimitInWatt(4140, 11040)));
		history.setAutomaticPhaseSwitchCooldown(Instant.now(ct.clock));

		var result = ct.execute(DistributionStrategy.EQUAL_POWER);
		assertNull(result.get(0).getPhaseSwitchDirection());
		assertEquals(presetEpochSeconds, result.get(0).getProbableNextPhaseSwitchEpochSeconds());
	}

	@Test
	void testAutomaticPhaseSwitchProbableTimestampResetsWhenOutdated() {
		final var history = new History();

		var ct = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-8000));
		final var outdatedEpochSeconds = Instant.now(ct.clock).minusSeconds(1).getEpochSecond();
		ct.set(0, c -> c //
				.setMode(SURPLUS) //
				.setHistory(history) //
				.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
				.setProbableNextPhaseSwitchEpochSeconds(outdatedEpochSeconds) //
				.setChargePointAbilities(cp -> cp //
						.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 7360)) //
						.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE)) //
				.setElectricVehicleAbilities(ev -> ev //
						.setCanInterrupt(true) //
						.setSinglePhaseLimitInWatt(1380, 7360) //
						.setThreePhaseLimitInWatt(4140, 11040)));
		history.setAutomaticPhaseSwitchCooldown(Instant.now(ct.clock));

		var result = ct.execute(DistributionStrategy.EQUAL_POWER);
		assertNull(result.get(0).getPhaseSwitchDirection());
		assertNull(result.get(0).getProbableNextPhaseSwitchEpochSeconds());
	}

	@Test
	void testAutomaticSurplusKeepsRawPvLimitBeforeFinalClamp() {
		var sut = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-3200)) //
				.set(0, c -> c //
						.setMode(SURPLUS) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_SINGLE_PHASE) //
								.setIsReadyForCharging(true)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setThreePhaseLimitInMilliAmpere(6000, 16000))) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(Integer.valueOf(3200), sut.get(0).getPvLimitInWatt());
	}

	@Test
	void testSurplusBelowThreePhaseMinimumSetsSetpointZeroAndKeepsPvLimit() {
		var sut = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-3200)) //
				.set(0, c -> c //
						.setMode(SURPLUS) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_SINGLE_PHASE) //
								.setIsReadyForCharging(true)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setThreePhaseLimitInMilliAmpere(6000, 16000))) //
				.execute(DistributionStrategy.EQUAL_POWER);

		// Automatic SURPLUS with pvLimit(3200) above single-phase min but below 3p-min:
		// charger keeps charging at 3p-min until phase switch is possible.
		assertEquals(4140, sut.get(0).getSetPointInWatt());
		assertEquals(Integer.valueOf(3200), sut.get(0).getPvLimitInWatt());
	}

	@Test
	void testSurplusBelowSinglePhaseMinimumKeepChargingMaintainsMinSetPoint() {
		// pvLimit(1000W) < 1p-min(1380W), KEEP_CHARGING active (default history has
		// isReadyForCharging=false).
		// KEEP_CHARGING overrides the zero-clamp when already in single-phase mode;
		// setPointWithoutPhaseLimitation is preserved
		// for the phase-switch evaluation and must not be affected.
		var sut = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-1000)) //
				.set(0, c -> c //
						.setMode(SURPLUS) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(SINGLE_PHASE, 6000, 32000)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE) //
								.setIsReadyForCharging(true)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInMilliAmpere(6000, 32000) //
								.setThreePhaseLimitInMilliAmpere(6000, 16000))) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(1380, sut.get(0).getSetPointInWatt());
		assertEquals(Integer.valueOf(1000), sut.get(0).getPvLimitInWatt());
	}

	@Test
	void testAutomaticPhaseSwitchCooldownDoesNotUseRelaxedThreeToSingleMinimum() {
		final var history = new History();
		var ct = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-1380)) //
				.set(0, c -> c //
						.setMode(SURPLUS) //
						.setHistory(history) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 16000)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_SINGLE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInWatt(4140, 11040)));

		executeAutomaticPhaseSwitchWarmup(ct, history, 0);
		var firstSwitch = ct.execute(DistributionStrategy.EQUAL_POWER);
		appendHistoryEntry(ct, history, firstSwitch, 0);
		ct.clock.leap(1, ChronoUnit.SECONDS);
		var second = ct.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(4140, firstSwitch.get(0).getSetPointInWatt());
		assertEquals(PhaseSwitchDirection.TO_SINGLE_PHASE, firstSwitch.get(0).getPhaseSwitchDirection());
		// During cooldown: 3p charger keeps charging at 3p-min, no phase switch.
		assertEquals(4140, second.get(0).getSetPointInWatt());
		assertNull(second.get(0).getPhaseSwitchDirection());
	}

	@Test
	void testAutomaticPhaseSwitchFullUseCase() {
		var history = new History();
		var ct = CalculateTester.generateControllers(1) //
				.sum(s -> s //
						.withGridActivePower(-8000)) //
				.set(0, c -> c //
						.setMode(SURPLUS) //
						.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
						.setHistory(history) //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 7360)) //
								.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true) //
								.setSinglePhaseLimitInWatt(1380, 7360) //
								.setThreePhaseLimitInWatt(4140, 11040)));

		executeAutomaticPhaseSwitchWarmup(ct, history, 0);
		var firstSwitch = ct.execute(DistributionStrategy.EQUAL_POWER);
		appendHistoryEntry(ct, history, firstSwitch, 0);
		assertEquals(PhaseSwitchDirection.TO_THREE_PHASE, firstSwitch.get(0).getPhaseSwitchDirection());

		ct.set(0, c -> c //
				.setMode(SURPLUS) //
				.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
				.setHistory(history) //
				.setChargePointAbilities(cp -> cp //
						.setApplySetPoint(new ApplySetPoint.Ability.Watt(THREE_PHASE, 4140, 11040)) //
						.setPhaseSwitchManual(PhaseSwitchDirection.TO_SINGLE_PHASE)) //
				.setElectricVehicleAbilities(ev -> ev //
						.setCanInterrupt(true) //
						.setSinglePhaseLimitInWatt(1380, 7360) //
						.setThreePhaseLimitInWatt(4140, 11040)));

		// no switch -> still three phase (8kW)
		var exec = ct.execute(DistributionStrategy.EQUAL_POWER);
		assertTrue(exec.get(0).getSetPointInWatt() >= 4140); // stays within 3p operating range
		assertEquals(8000, exec.get(0).getPvLimitInWatt());
		appendHistoryEntry(ct, history, exec, 0);

		// pv drops to 2000
		ct.sum(s -> s //
				.withGridActivePower(-2000)); //
		exec = ct.execute(DistributionStrategy.EQUAL_POWER);
		// pvLimit(2000) > singlePhaseMin(1380): 3p charger keeps charging at 3p-min.
		assertTrue(exec.get(0).getSetPointInWatt() >= 4140);
		assertEquals(2000, exec.get(0).getPvLimitInWatt());
		appendHistoryEntry(ct, history, exec, 0);

		// Keep sampling below threshold. During cooldown no switch is allowed.
		for (int i = 0; i < 299; i++) {
			exec = ct.execute(DistributionStrategy.EQUAL_POWER);
			assertNull(exec.get(0).getPhaseSwitchDirection());
			appendHistoryEntry(ct, history, exec, 0);
			ct.clock.leap(1, ChronoUnit.SECONDS);
		}

		// Cooldown elapsed and enough samples are present -> switch back to single
		// phase.
		ct.clock.leap(1, ChronoUnit.SECONDS);
		exec = ct.execute(DistributionStrategy.EQUAL_POWER);
		assertEquals(PhaseSwitchDirection.TO_SINGLE_PHASE, exec.get(0).getPhaseSwitchDirection());
	}

	private static void executeAutomaticPhaseSwitchWarmup(CalculateTester ct, History history, int index) {
		IntStream.range(0, 59).forEach(i -> {
			var warmup = ct.execute(DistributionStrategy.EQUAL_POWER);
			assertNull(warmup.get(index).getPhaseSwitchDirection());
			appendHistoryEntry(ct, history, warmup, index);
			ct.clock.leap(1, ChronoUnit.SECONDS);
		});
	}

	private static void appendHistoryEntry(CalculateTester ct, History history,
			CalculateTester.PowerDistributionTester result, int index) {
		history.addEntry(Instant.now(ct.clock), 1_000, result.get(index).getSetPointInWatt(),
				result.get(index).getPvLimitInWatt(), true);
	}

	@Test
	void test7() {
		final var history = new History();
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

		assertArrayEquals(new int[] { 6000, 6000, 0, 6000, 16000 }, sut.getApplySetPoints());
	}

	@Test
	void test8() {
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
	void test9() {
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

	@Test
	void testAutomaticPhaseSwitchDistributionUsesOppositePhaseMaximum() {
		final var automaticCtrl = TestUtils.createSingleCtrl() //
				.setMode(SURPLUS) //
				.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
				.setChargePointAbilities(cp -> cp //
						.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 7360)) //
						.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE)) //
				.setElectricVehicleAbilities(ev -> ev //
						.setCanInterrupt(true) //
						.setSinglePhaseLimitInWatt(1380, 7360) //
						.setThreePhaseLimitInWatt(4140, 11040)) //
				.build();
		final var minimumCtrl = TestUtils.createSingleCtrl() //
				.setMode(MINIMUM) //
				.setChargePointAbilities(cp -> cp //
						.setApplySetPoint(new ApplySetPoint.Ability.Watt(THREE_PHASE, 4140, 11040))) //
				.setElectricVehicleAbilities(ev -> ev //
						.setSinglePhaseLimitInWatt(1380, 7360) //
						.setThreePhaseLimitInWatt(4140, 11040)) //
				.build();

		var automaticEntry = new PowerDistribution.Entry(null, automaticCtrl, automaticCtrl.getParams());
		var minimumEntry = new PowerDistribution.Entry(null, minimumCtrl, minimumCtrl.getParams());
		minimumEntry.setPointInWatt = 4140;

		var powerDistribution = new PowerDistribution(ImmutableList.of(automaticEntry, minimumEntry));
		RunUtils.distributeSurplusRemainingPower(powerDistribution, DistributionStrategy.EQUAL_POWER, 20_000);

		assertEquals(11040,
				automaticCtrl.getParams().combinedAbilities().phaseSwitch().oppositePhaseApplySetPoint().max());
		assertEquals(11040, automaticEntry.setPointInWatt);
		assertEquals(11040, minimumEntry.setPointInWatt);
	}

	@Test
	void testAutomaticPhaseSwitchDistributionSkipsGapBetweenPhaseRanges() {
		final var automaticCtrl = TestUtils.createSingleCtrl() //
				.setMode(SURPLUS) //
				.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
				.setChargePointAbilities(cp -> cp //
						.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 3680)) //
						.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE)) //
				.setElectricVehicleAbilities(ev -> ev //
						.setCanInterrupt(true) //
						.setSinglePhaseLimitInWatt(1380, 3680) //
						.setThreePhaseLimitInWatt(4140, 11040)) //
				.build();

		var automaticEntry = new PowerDistribution.Entry(null, automaticCtrl, automaticCtrl.getParams());

		RunUtils.distributePowerEqual(List.of(automaticEntry), 3900);

		assertEquals(3680, automaticEntry.setPointInWatt);
	}

	@Test
	void testAutomaticPhaseSwitchDistributionWithTwoAutomaticEntriesAndOppositePhaseAbility() {
		final var automaticCtrl0 = TestUtils.createSingleCtrl() //
				.setMode(SURPLUS) //
				.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
				.setChargePointAbilities(cp -> cp //
						.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 3680)) //
						.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE)) //
				.setElectricVehicleAbilities(ev -> ev //
						.setCanInterrupt(true) //
						.setSinglePhaseLimitInWatt(1380, 3680) //
						.setThreePhaseLimitInWatt(4140, 11040)) //
				.build();

		final var automaticCtrl1 = TestUtils.createSingleCtrl() //
				.setMode(SURPLUS) //
				.setPhaseSwitching(PhaseSwitching.AUTOMATIC) //
				.setChargePointAbilities(cp -> cp //
						.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1380, 3680)) //
						.setPhaseSwitchManual(PhaseSwitchDirection.TO_THREE_PHASE)) //
				.setElectricVehicleAbilities(ev -> ev //
						.setCanInterrupt(true) //
						.setSinglePhaseLimitInWatt(1380, 3680) //
						.setThreePhaseLimitInWatt(4140, 11040)) //
				.build();

		var automaticEntry0 = new PowerDistribution.Entry(null, automaticCtrl0, automaticCtrl0.getParams());
		var automaticEntry1 = new PowerDistribution.Entry(null, automaticCtrl1, automaticCtrl1.getParams());

		assertNotNull(automaticCtrl0.getParams().combinedAbilities().phaseSwitch().oppositePhaseApplySetPoint());
		assertNotNull(automaticCtrl1.getParams().combinedAbilities().phaseSwitch().oppositePhaseApplySetPoint());

		RunUtils.distributePowerEqual(List.of(automaticEntry0, automaticEntry1), 7800);

		// Equal share is 3900 W, but this lies in the 1p->3p gap (3680..4139), so both
		// stay on current phase maximum.
		assertEquals(3680, automaticEntry0.setPointInWatt);
		assertEquals(3680, automaticEntry1.setPointInWatt);
	}

	@Test
	void testApplyChangeLimitWithHistory() {
		final var clock = createDummyClock();
		final var history = new History();
		var setPointInWatt = 10_000; // 10kW
		history.addEntry(Instant.now(clock), null, setPointInWatt, null, true);

		clock.leap(1, ChronoUnit.SECONDS);

		final var ctrl = TestUtils.createSingleCtrl() //
				.setMode(SURPLUS) //
				.setHistory(history) //
				.setChargePointAbilities(cp -> cp //
						.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1_000, 50_000))) //
				.build();

		final var entry = new PowerDistribution.Entry(null, ctrl, ctrl.getParams());
		entry.setPointInWatt = 20_000;

		RunUtils.applyChangeLimit(clock, new PowerDistribution(ImmutableList.of(entry)));
		var expectedSetPointInWatt = 10_300; // 10kW + 3% = 10.3kW
		assertEquals(expectedSetPointInWatt, entry.setPointInWatt);
	}

	@Test
	void testApplyChangeLimitWithoutHistory() {
		final var clock = createDummyClock();
		final var noHistory = new History();
		clock.leap(1, ChronoUnit.SECONDS);
		var minSetPoint = 6 * SINGLE_PHASE.count * 230; // 6A * 1 Phase * 230 V = 1380W
		final var ctrl = TestUtils.createSingleCtrl() //
				.setMode(SURPLUS) //
				.setHistory(noHistory) //
				.setChargePointAbilities(cp -> cp //
						.setApplySetPoint(new ApplySetPoint.Ability.Watt(SINGLE_PHASE, 1_000, 50_000))) //
				.build();

		final var entry = new PowerDistribution.Entry(null, ctrl, ctrl.getParams());
		entry.setPointInWatt = 20_000;

		RunUtils.applyChangeLimit(clock, new PowerDistribution(ImmutableList.of(entry)));

		assertEquals(minSetPoint, entry.setPointInWatt);
	}

	@Test
	void testResolveAutomaticPhaseSwitchTargetPhaseMinPowerInWattFallsBackWhenPhaseSwitchAbilityIsNull()
			throws Exception {
		var method = RunUtils.class.getDeclaredMethod("resolveAutomaticPhaseSwitchTargetPhaseMinPowerInWatt",
				ApplyPhaseSwitch.class, PhaseSwitchDirection.class);
		method.setAccessible(true);

		assertEquals(ApplySetPoint.MIN_POWER_SINGLE_PHASE,
				method.invoke(null, null, PhaseSwitchDirection.TO_SINGLE_PHASE));
		assertEquals(ApplySetPoint.MIN_POWER_THREE_PHASE,
				method.invoke(null, null, PhaseSwitchDirection.TO_THREE_PHASE));
	}

	/**
	 * test verifies that the first entry with same set point as the last one is
	 * returned.
	 */
	@Test
	void testFindFirstEntryWithSameSetPoint_1() {
		final var clock = createDummyClock();
		final var activePower = 123;
		History history = new History();
		var firstNow = Instant.now(clock);
		history.addEntry(firstNow, activePower, 6, null, true);
		addEntriesToTheHistory(clock, history, 10, 6, activePower);
		clock.leap(1, ChronoUnit.SECONDS);
		var lastNow = Instant.now(clock);
		history.addEntry(lastNow, activePower, 6, null, true);
		// WHEN
		var firstEntryWithSameSetPoint = findFirstEntryWithSameSetPoint(history);
		// THEN
		assertEquals(firstNow, firstEntryWithSameSetPoint.getKey());
	}

	/**
	 * test verifies that the last entry is returned when no previous entry with
	 * same set point exists.
	 */
	@Test
	void testFindFirstEntryWithSameSetPoint_2() {
		final var activePower = 123;
		final var clock = createDummyClock();
		History history = new History();
		var firstNow = Instant.now(clock);
		history.addEntry(firstNow, activePower, 6, null, true);
		addEntriesToTheHistory(clock, history, 10, 6, activePower);
		clock.leap(1, ChronoUnit.SECONDS);
		var lastNow = Instant.now(clock);
		history.addEntry(lastNow, activePower, 7, null, true);
		// WHEN
		var firstEntryWithSameSetPoint = findFirstEntryWithSameSetPoint(history);
		// THEN
		assertEquals(lastNow, firstEntryWithSameSetPoint.getKey());
	}

	/**
	 * test verifies that entries with set point different from the last one are
	 * skipped and ignore intermediate entries with same set point.
	 */
	@Test
	void testFindFirstEntryWithSameSetPoint_3() {
		final var activePower = 123;
		final var clock = createDummyClock();
		History history = new History();
		var firstNow = this.addEntryToTheHistoryAfterOneSecond(clock, history, 6, activePower);
		history.addEntry(firstNow, null, 6, null, true);
		addEntriesToTheHistory(clock, history, 10, 6, activePower);
		addEntriesToTheHistory(clock, history, 10, 7, activePower);
		var now = this.addEntryToTheHistoryAfterOneSecond(clock, history, 6, activePower);
		// WHEN
		var firstEntryWithSameSetPoint = findFirstEntryWithSameSetPoint(history);
		// THEN
		assertEquals(now, firstEntryWithSameSetPoint.getKey());
	}

	/**
	 * test verifies that entries with activePower == 0 are skipped.
	 */
	@Test
	void testFindFirstEntryWithSameSetPoint_4() {
		// GIVEN
		final var activePower = 123;
		final var zeroActivePower = 0;
		final var clock = createDummyClock();
		History history = new History();
		addEntriesToTheHistory(clock, history, 300, 6, zeroActivePower);
		var now = this.addEntryToTheHistoryAfterOneSecond(clock, history, 6, activePower);
		// WHEN
		var firstEntryWithSameSetPoint = findFirstEntryWithSameSetPoint(history);
		// THEN
		assertEquals(now, firstEntryWithSameSetPoint.getKey());
	}

	/**
	 * test verifies that entries with activePower == null are skipped.
	 */
	@Test
	void testFindFirstEntryWithSameSetPoint_5() {
		// GIVEN
		final var activePower = 123;
		final Integer nullActivePower = null;
		final var clock = createDummyClock();
		History history = new History();
		addEntriesToTheHistory(clock, history, 300, 6, nullActivePower);
		var now = this.addEntryToTheHistoryAfterOneSecond(clock, history, 6, activePower);
		// WHEN
		var firstEntryWithSameSetPoint = findFirstEntryWithSameSetPoint(history);
		// THEN
		assertEquals(now, firstEntryWithSameSetPoint.getKey());
	}

	/**
	 * test verifies that entries with isReadyForCharging == false are skipped.
	 */
	@Test
	void testFindFirstEntryWithSameSetPoint_6() {
		// GIVEN
		final var isReadyForCharging = false;
		final var activePower = 123;
		final Integer nullActivePower = null;
		final var clock = createDummyClock();
		History history = new History();
		addEntriesToTheHistory(clock, history, 300, 6, nullActivePower);
		clock.leap(1, ChronoUnit.SECONDS);
		history.addEntry(Instant.now(clock), activePower, 6, null, isReadyForCharging);
		var now = this.addEntryToTheHistoryAfterOneSecond(clock, history, 6, activePower);
		// WHEN
		var firstEntryWithSameSetPoint = findFirstEntryWithSameSetPoint(history);
		// THEN
		assertEquals(now, firstEntryWithSameSetPoint.getKey());
	}

	private Instant addEntryToTheHistoryAfterOneSecond(TimeLeapClock clock, History history, int setPoint,
			Integer activePower) {
		clock.leap(1, ChronoUnit.SECONDS);
		var now = Instant.now(clock);
		history.addEntry(now, activePower, setPoint, null, true);
		return now;
	}

	private static void addEntriesToTheHistory(TimeLeapClock clock, History history, int amountOfEntries,
			int setPointInWatt, Integer activePower) {
		for (int cycle = 0; cycle < amountOfEntries; cycle++) {
			clock.leap(1, ChronoUnit.SECONDS);
			history.addEntry(Instant.now(clock), activePower, setPointInWatt, null, true);
		}
	}
}
