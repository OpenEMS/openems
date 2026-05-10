package io.openems.edge.controller.evse.cluster;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.controller.evse.cluster.RunUtils.calculate;
import static io.openems.edge.controller.evse.cluster.RunUtils.findFirstEntryWithSameSetPoint;
import static io.openems.edge.controller.evse.single.PhaseSwitching.DISABLE;
import static io.openems.edge.evse.api.chargepoint.Mode.FORCE;
import static io.openems.edge.evse.api.chargepoint.Mode.MINIMUM;
import static io.openems.edge.evse.api.chargepoint.Mode.SURPLUS;
import static io.openems.edge.evse.api.chargepoint.Mode.ZERO;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.controller.evse.TestUtils;
import io.openems.edge.controller.evse.TestUtils.CtrlBuilder;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.SingleModes;
import io.openems.edge.controller.evse.cluster.RunUtils.PowerDistribution;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.controller.evse.single.PhaseSwitching;
import io.openems.edge.controller.evse.single.Types.History;
import io.openems.edge.evse.api.chargepoint.Profile.PhaseSwitch;
import io.openems.edge.evse.api.common.ApplySetPoint;

public class RunUtilsTest {

	protected static class CalculateTester {

		public static CalculateTester generateControllers(int count) {
			final var clock = createDummyClock();

			// Add History with high value to tick Utils::applyChangeLimit.
			final var history = new History();
			history.addEntry(Instant.now(clock), null, 22000 /* [W] */, false);
			clock.leap(500, ChronoUnit.MILLIS);

			return new CalculateTester(clock, IntStream.range(0, count) //
					.<CtrlBuilder>mapToObj(i -> TestUtils.createSingleCtrl() //
							.setId("evse" + i) //
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
					new SingleModes(ImmutableMap.of()), //
					LogVerbosity.NONE, log -> doNothing()));
		}

		protected static record PowerDistributionTester(PowerDistribution powerDistribution) {
			protected static record EntryTester(PowerDistribution.Entry entry) {
				protected int getApplySetPointInMilliAmpere() {
					return ((ApplySetPoint.Action.MilliAmpere) this.entry.actions.build().applySetPoint()).value();
				}

				protected int getApplySetPointInAmpere() {
					return ((ApplySetPoint.Action.Ampere) this.entry.actions.build().applySetPoint()).value();
				}

				protected int getApplySetPointInWatt() {
					return ((ApplySetPoint.Action.Watt) this.entry.actions.build().applySetPoint()).value();
				}

				protected PhaseSwitch getPhaseSwitch() {
					return this.entry.actions.build().phaseSwitch();
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
	public void test1() {
		var ct = CalculateTester.generateControllers(5); //
		final var history = new History();
		history.addEntry(Instant.now(ct.clock), null, 10000 /* [W] */, false);
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

		history.addEntry(Instant.now(ct.clock), null, 10150 /* [W] */, false);
		ct.clock.leap(1000, ChronoUnit.MILLIS);
		sut = ct.execute(DistributionStrategy.EQUAL_POWER);
		assertArrayEquals(new int[] { 0, 15, 15152, 15458, 0 }, sut.getApplySetPoints());

		// #3 apply ramp on [2]

		history.addEntry(Instant.now(ct.clock), null, 10455 /* [W] */, false);
		ct.clock.leap(1000, ChronoUnit.MILLIS);
		sut = ct.execute(DistributionStrategy.EQUAL_POWER);
		assertArrayEquals(new int[] { 0, 15, 15607, 15458, 0 }, sut.getApplySetPoints());

		// #4 finished ramp on [2]

		history.addEntry(Instant.now(ct.clock), null, 10769 /* [W] */, false);
		ct.clock.leap(1000, ChronoUnit.MILLIS);
		sut = ct.execute(DistributionStrategy.EQUAL_POWER);
		assertArrayEquals(new int[] { 0, 15, 15916, 15458, 0 }, sut.getApplySetPoints());
	}

	@Test
	public void test2() {
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
	public void testMinimumWithSurplus() {
		var sut = CalculateTester.generateControllers(3) //
				.set(0, c -> c //
						.setMode(FORCE)) //
				.set(1, 2, c -> 
						c.setMode(MINIMUM)) //
				.sum(s -> s //
						.withGridActivePower(-27000)) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertArrayEquals(new int[] { 16000, 11565, 11565 }, sut.getApplySetPoints());
	}
	
	@Test
	public void testMinimumWithoutSurplus() {
		var sut = CalculateTester.generateControllers(4) //
				.set(0, 3, c -> c //
						.setMode(FORCE)) //
				.set(1, 2, c -> 
						c.setMode(MINIMUM)) //
				.sum(s -> s //
						.withGridActivePower(0)) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertArrayEquals(new int[] { 16000, 6000, 6000, 16000 }, sut.getApplySetPoints());
	}

	@Test
	public void test3() {
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
	public void test4() {
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
	public void test5() {
		var sut = CalculateTester.generateControllers(5) //
				.set(0, c -> c //
						.setChargePointAbilities(cp -> cp //
								.setApplySetPoint(new ApplySetPoint.Ability.Ampere(SINGLE_PHASE, 6, 16)) //
								.setPhaseSwitch(PhaseSwitch.TO_THREE_PHASE)) //
						.setElectricVehicleAbilities(ev -> ev //
								.setCanInterrupt(true))) //
				.setAll(c -> c //
						.setMode(FORCE) //
						.setPhaseSwitching(PhaseSwitching.FORCE_THREE_PHASE)) //
				.execute(DistributionStrategy.EQUAL_POWER);

		assertEquals(PhaseSwitch.TO_THREE_PHASE, sut.get(0).getPhaseSwitch());
		assertNull(sut.get(1).getPhaseSwitch());
	}

	@Test
	public void test6() {
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
	public void test7() {
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
	public void test8() {
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
	public void test9() {
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
	public void testApplyChangeLimitWithHistory() {
		final var clock = createDummyClock();
		final var history = new History();
		var setPointInWatt = 10_000; // 10kW
		history.addEntry(Instant.now(clock), null, setPointInWatt, true);

		clock.leap(1, ChronoUnit.SECONDS);

		final var ctrl = TestUtils.createSingleCtrl() //
				.setId("evse0") //
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
	public void testApplyChangeLimitWithoutHistory() {
		final var clock = createDummyClock();
		final var noHistory = new History();
		clock.leap(1, ChronoUnit.SECONDS);
		var minSetPoint = 6 * SINGLE_PHASE.count * 230; // 6A * 1 Phase * 230 V = 1380W
		final var ctrl = TestUtils.createSingleCtrl() //
				.setId("evse0") //
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

	/**
	 * test verifies that the first entry with same set point as the last one is returned.
	 */
	@Test
	public void testFindFirstEntryWithSameSetPoint_1() {
		//GIVEN
		final var clock = createDummyClock();
		final var activePower = 123;
		History history = new History();
		var firstNow = Instant.now(clock);
		history.addEntry(firstNow, activePower, 6, true);
		addEntriesToTheHistory(clock, history, 10, 6, activePower);
		clock.leap(1, ChronoUnit.SECONDS);
		var lastNow = Instant.now(clock);
		history.addEntry(lastNow, activePower, 6, true);
		//WHEN
		var firstEntryWithSameSetPoint = findFirstEntryWithSameSetPoint(history);
		//THEN
		assertEquals(firstNow, firstEntryWithSameSetPoint.getKey());
	}

	/**
	 * test verifies that the last entry is returned when no previous entry with same set point exists.
	 */
	@Test
	public void testFindFirstEntryWithSameSetPoint_2() {
		//GIVEN
		final var activePower = 123;
		final var clock = createDummyClock();
		History history = new History();
		var firstNow = Instant.now(clock);
		history.addEntry(firstNow, activePower, 6, true);
		addEntriesToTheHistory(clock, history, 10, 6,activePower);
		clock.leap(1, ChronoUnit.SECONDS);
		var lastNow = Instant.now(clock);
		history.addEntry(lastNow, activePower, 7, true);
		//WHEN
		var firstEntryWithSameSetPoint = findFirstEntryWithSameSetPoint(history);
		//THEN
		assertEquals(lastNow, firstEntryWithSameSetPoint.getKey());
	}


	/**
	 * test verifies that entries with set point different from the last one are skipped
	 * and ignore intermediate entries with same set point.
	 */
	@Test
	public void testFindFirstEntryWithSameSetPoint_3() {
		//GIVEN
		final var activePower = 123;
		final var clock = createDummyClock();
		History history = new History();
		var firstNow = this.addEntryToTheHistoryAfterOneSecond(clock, history, 6, activePower);
		history.addEntry(firstNow, null, 6, true);
		addEntriesToTheHistory(clock, history, 10, 6, activePower);
		addEntriesToTheHistory(clock, history, 10, 7, activePower);
		var now = this.addEntryToTheHistoryAfterOneSecond(clock, history, 6, activePower);
		//WHEN
		var firstEntryWithSameSetPoint = findFirstEntryWithSameSetPoint(history);
		//THEN
		assertEquals(now, firstEntryWithSameSetPoint.getKey());
	}

	/**
	 * test verifies that entries with activePower == 0 are skipped.
	 */
	@Test
	public void testFindFirstEntryWithSameSetPoint_4() {
		//GIVEN
		final var activePower = 123;
		final var zeroActivePower = 0;
		final var clock = createDummyClock();
		History history = new History();
		addEntriesToTheHistory(clock, history, 300, 6, zeroActivePower);
		var now = this.addEntryToTheHistoryAfterOneSecond(clock, history, 6, activePower);
		//WHEN
		var firstEntryWithSameSetPoint = findFirstEntryWithSameSetPoint(history);
		//THEN
		assertEquals(now, firstEntryWithSameSetPoint.getKey());
	}

	/**
	 * test verifies that entries with activePower == null are skipped.
	 */
	@Test
	public void testFindFirstEntryWithSameSetPoint_5() {
		//GIVEN
		final var activePower = 123;
		final Integer nullActivePower = null;
		final var clock = createDummyClock();
		History history = new History();
		addEntriesToTheHistory(clock, history, 300, 6, nullActivePower);
		var now = this.addEntryToTheHistoryAfterOneSecond(clock, history, 6, activePower);
		//WHEN
		var firstEntryWithSameSetPoint = findFirstEntryWithSameSetPoint(history);
		//THEN
		assertEquals(now, firstEntryWithSameSetPoint.getKey());
	}

	/**
	 * test verifies that entries with isReadyForCharging == false are skipped.
	 */
	@Test
	public void testFindFirstEntryWithSameSetPoint_6() {
		//GIVEN
		final var isReadyForCharging = false;
		final var activePower = 123;
		final Integer nullActivePower = null;
		final var clock = createDummyClock();
		History history = new History();
		addEntriesToTheHistory(clock, history, 300, 6, nullActivePower);
		clock.leap(1, ChronoUnit.SECONDS);
		history.addEntry(Instant.now(clock), activePower, 6, isReadyForCharging);
		var now = this.addEntryToTheHistoryAfterOneSecond(clock, history, 6, activePower);
		//WHEN
		var firstEntryWithSameSetPoint = findFirstEntryWithSameSetPoint(history);
		//THEN
		assertEquals(now, firstEntryWithSameSetPoint.getKey());
	}

	private Instant addEntryToTheHistoryAfterOneSecond(TimeLeapClock clock, History history, int setPoint, Integer activePower) {
		clock.leap(1, ChronoUnit.SECONDS);
		var now = Instant.now(clock);
		history.addEntry(now, activePower, setPoint, true);
		return now;
	}

	private static void addEntriesToTheHistory(TimeLeapClock clock, History history, int amountOfEntries, int setPointInWatt, Integer activePower) {
		for (int cycle = 0; cycle < amountOfEntries; cycle++) {
			clock.leap(1, ChronoUnit.SECONDS);
			history.addEntry(Instant.now(clock), activePower, setPointInWatt, true);
		}
	}
}
