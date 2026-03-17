package io.openems.edge.controller.ess.sohcycle;

import static io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycle.ChannelId.STATE_MACHINE;
import static io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycleImpl.ZERO_WATT_POWER;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.MAX_CELL_VOLTAGE;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.MIN_CELL_VOLTAGE;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.SOC;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.channel.ChannelUtils;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.ess.sohcycle.statemachine.StateMachine;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;

public class ControllerEssSohCycleTest {

	private final DummyConfigurationAdmin cm = new DummyConfigurationAdmin();

	private static final String ESS_ID = "ess0";
	private	static final String CONTROLLER_ID = "ctrl0";
	private static final int DEFAULT_MAX_APPARENT_POWER = 10_000;
	private static final int DEFAULT_CAPACITY = 10_000;
	private static final String DEFAULT_CLOCK_INSTANT = "2000-01-01T00:00:00.00Z";
	private static final int WAIT_TIME_MINUTES = 30;

	@Test
	public void testCompleteSohCycleFromIdleToDone() throws Exception {
		final var clock = this.createDefaultClock();
		final var power = new DummyPower(DEFAULT_MAX_APPARENT_POWER);
		final var ess = this.createDefaultEss();
		final var controller = new ControllerEssSohCycleImpl();
		final var test = this.createControllerTest(clock, ess, controller, true);
		power.addEss(ess);
		test.next(new TestCase() //
				.input(STATE_MACHINE, StateMachine.State.IDLE) //
				.input(ESS_ID, SOC, 50))
				.next(new TestCase("Preparing Reference") //
						.output(STATE_MACHINE, StateMachine.State.PREPARE))
				.next(new TestCase() //
						.input(ESS_ID, SOC, 0))
				.next(new TestCase("Reference: start charging") //
						.output(STATE_MACHINE, StateMachine.State.REFERENCE_CYCLE_CHARGING))
				.next(new TestCase() //
						.input(ESS_ID, SOC, 100))
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.REFERENCE_CYCLE_CHARGING_WAIT))
				.next(new TestCase("Waiting 30 minutes")//
						.timeleap(clock, WAIT_TIME_MINUTES, ChronoUnit.MINUTES))//
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.REFERENCE_CYCLE_DISCHARGING))
				.next(new TestCase() //
						.input(ESS_ID, SOC, 0))
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.REFERENCE_CYCLE_DISCHARGING_WAIT))
				.next(new TestCase("Waiting 30 minutes")//
						.timeleap(clock, WAIT_TIME_MINUTES, ChronoUnit.MINUTES))//
				.next(new TestCase() //
						.input(ESS_ID, ACTIVE_DISCHARGE_ENERGY, 50_000L) //
						.output(STATE_MACHINE, StateMachine.State.MEASUREMENT_CYCLE_CHARGING))
				.next(new TestCase() //
						.input(ESS_ID, SOC, 100))
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.MEASUREMENT_CYCLE_CHARGING_WAIT))
				.next(new TestCase("Waiting 30 minutes")//
						.timeleap(clock, WAIT_TIME_MINUTES, ChronoUnit.MINUTES))//
				.next(new TestCase() //
						.input(ESS_ID, MIN_CELL_VOLTAGE, 3200) //
						.input(ESS_ID, MAX_CELL_VOLTAGE, 3250) //
						.output(STATE_MACHINE, StateMachine.State.CHECK_BALANCING))
				.next(new TestCase("Measurement discharge ongoing") //
						.output(STATE_MACHINE, StateMachine.State.MEASUREMENT_CYCLE_DISCHARGING))
				.next(new TestCase() //
						.input(ESS_ID, SOC, 0))
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.MEASUREMENT_CYCLE_DISCHARGING_WAIT))
				.next(new TestCase("Waiting 30 minutes")//
						.timeleap(clock, WAIT_TIME_MINUTES, ChronoUnit.MINUTES))//
				.next(new TestCase()//
						.input(ESS_ID, ACTIVE_DISCHARGE_ENERGY, 65_000L)//
						.output(STATE_MACHINE, StateMachine.State.EVALUATE_RESULT))
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.DONE)//
						.onAfterControllersCallbacks(() -> {
							this.assertMeasuredCapacity(controller, 15_000L);
						}))
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.IDLE)//
						.onAfterControllersCallbacks(() -> {
							var config = this.cm.getOrCreateEmptyConfiguration(controller.servicePid());
							assertEquals("Running should switch to false after DONE->IDLE",
									Boolean.FALSE, config.getProperties().get("isRunning"));
							this.assertMeasuredCapacity(controller, 15_000L);
						}))
				.deactivate();


	}

	@Test
	public void testPrepareForReferenceCycleWithHighPowerLowCapacity() throws Exception {
		this.runPrepareForReferenceCycle(10_000, 5_000);
	}

	@Test
	public void testPrepareForReferenceCycleWithLowPowerHighCapacity() throws Exception {
		this.runPrepareForReferenceCycle(1_000, 10_000);
	}


	@Test
	public void testReferenceCycleChargingWithPowerRampUp() throws Exception {
		this.runCycleRampUntilWait(StateMachine.State.REFERENCE_CYCLE_CHARGING,
				StateMachine.State.REFERENCE_CYCLE_CHARGING_WAIT,
				new int[] { 0, 10, 20, 30, 40, 80, 100 },
				-1);
	}

	@Test
	public void testReferenceCycleDischargingWithPowerRampUp() throws Exception {
		this.runCycleRampUntilWait(StateMachine.State.REFERENCE_CYCLE_DISCHARGING,
				StateMachine.State.REFERENCE_CYCLE_DISCHARGING_WAIT,
				new int[] { 100, 90, 80, 70, 60, 20, 0 },
				1);
	}

	@Test
	public void testMeasurementCycleChargingWithPowerRampUp() throws Exception {
		this.runCycleRampUntilWait(StateMachine.State.MEASUREMENT_CYCLE_CHARGING,
				StateMachine.State.MEASUREMENT_CYCLE_CHARGING_WAIT,
				new int[] { 0, 10, 20, 30, 40, 80, 100 },
				-1);
	}

	@Test
	public void testMeasurementCycleDischargingWithPowerRampUp() throws Exception {
		this.runCycleRampUntilWait(StateMachine.State.MEASUREMENT_CYCLE_DISCHARGING,
				StateMachine.State.MEASUREMENT_CYCLE_DISCHARGING_WAIT,
				new int[] { 100, 90, 80, 70, 60, 20, 0 },
				1);
	}

	@Test
	public void testMeasurementBaselineCapturedAndAvailableAtEvaluate() throws Exception {
		final var clock = this.createDefaultClock();
		final var controller = new ControllerEssSohCycleImpl();
		final var test = this.setupTestAndRunToMeasurementCycleDischarging(clock, controller);
		final long baselineWh = 12_345L;
		test.next(new TestCase()//
						.input(ESS_ID, MIN_CELL_VOLTAGE, 3200)//
						.input(ESS_ID, MAX_CELL_VOLTAGE, 3250)//
						.output(STATE_MACHINE, StateMachine.State.CHECK_BALANCING))
				.next(new TestCase()//
						.input(ESS_ID, ACTIVE_DISCHARGE_ENERGY, baselineWh)//
						.output(STATE_MACHINE, StateMachine.State.MEASUREMENT_CYCLE_DISCHARGING))
				.next(new TestCase()//
						.input(ESS_ID, SOC, 0))
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.MEASUREMENT_CYCLE_DISCHARGING_WAIT))
				.next(new TestCase().timeleap(clock, WAIT_TIME_MINUTES, ChronoUnit.MINUTES))
				// TEST: Baseline still available at EvaluateResult
				.next(new TestCase("Baseline persists at EvaluateResult")//
						.input(ESS_ID, ACTIVE_DISCHARGE_ENERGY, baselineWh + 5_000L)//
						.output(STATE_MACHINE, StateMachine.State.EVALUATE_RESULT)//
						.onAfterControllersCallbacks(() -> this.assertBaseline(controller, baselineWh)))
				.next(new TestCase("Baseline persists at DONE")//
						.output(STATE_MACHINE, StateMachine.State.DONE)//
						.onAfterControllersCallbacks(() -> this.assertBaseline(controller, baselineWh)))
				.next(new TestCase("Baseline persists in IDLE")//
						.output(STATE_MACHINE, StateMachine.State.IDLE))
				.deactivate();
	}

	@Test
	public void testMeasuredCapacityClearedWhenStartingNewCycle() throws Exception {
		final var clock = this.createDefaultClock();
		final var power = new DummyPower(DEFAULT_MAX_APPARENT_POWER);
		final var ess = this.createDefaultEss();
		final var controller = new ControllerEssSohCycleImpl();
		final var test = this.createControllerTest(clock, ess, controller);
		power.addEss(ess);

		// Manually set a measured capacity value to simulate a completed cycle
		ChannelUtils.setValue(controller, ControllerEssSohCycle.ChannelId.MEASURED_CAPACITY, 15_000L);

		test.next(new TestCase("IDLE state with previous measured capacity")//
				.input(STATE_MACHINE, StateMachine.State.IDLE)//
				.input(ESS_ID, SOC, 50)//
				.onAfterControllersCallbacks(() -> {
					// Verify the capacity is set before starting new cycle
					this.assertMeasuredCapacity(controller, 15_000L);
				}))
				.next(new TestCase("Transition to PREPARE_REFERENCE should clear MEASURED_CAPACITY")//
						.output(STATE_MACHINE, StateMachine.State.PREPARE)//
						.onAfterControllersCallbacks(() -> {
							// Verify the capacity is cleared when starting new cycle
							this.assertMeasuredCapacityCleared(controller);
						}))
				.deactivate();
	}

	@Test
	public void testPrepareReferenceSkipsReferenceCycleWhenDisabled() throws Exception {
		final var clock = this.createDefaultClock();
		final var power = new DummyPower(DEFAULT_MAX_APPARENT_POWER);
		final var ess = this.createDefaultEss();
		final var controller = new ControllerEssSohCycleImpl();
		final var test = this.createControllerTest(clock, ess, controller, false);
		power.addEss(ess);

		test.next(new TestCase()//
				.input(STATE_MACHINE, StateMachine.State.IDLE)//
				.input(ESS_ID, SOC, 50))//
				.next(new TestCase("Preparing Reference")
						.output(STATE_MACHINE, StateMachine.State.PREPARE))
				.next(new TestCase()//
						.input(ESS_ID, SOC, 0))
				.next(new TestCase("Should skip Reference Cycle and go directly to Measurement Cycle")//
						.output(STATE_MACHINE, StateMachine.State.MEASUREMENT_CYCLE_CHARGING))
				.deactivate();
	}


	// Helper methods for baseline tests
	/**
	 * Sets up test controller and runs through IDLE -> PREPARE_REFERENCE ->
	 * REFERENCE cycles to reach MEASUREMENT_CYCLE_DICHARGING state.
	 * @param clock      the clock to use for time leaps
	 * @param controller the controller instance to test
	 * @return the controller test instance atMEASUREMENT_CYCLE_CHARGING state
	 */
	private ControllerTest setupTestAndRunToMeasurementCycleDischarging(
			TimeLeapClock clock, ControllerEssSohCycleImpl controller) throws Exception {
		final var power = new DummyPower(DEFAULT_MAX_APPARENT_POWER);
		final var ess = this.createDefaultEss();
		final var test = this.createControllerTest(clock, ess, controller, true);
		power.addEss(ess);
		test.next(new TestCase()//
						.input(STATE_MACHINE, StateMachine.State.IDLE).input(ESS_ID, SOC, 50))
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.PREPARE))
				.next(new TestCase()//
						.input(ESS_ID, SOC, 0))
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.REFERENCE_CYCLE_CHARGING))
				.next(new TestCase()//
						.input(ESS_ID, SOC, 100))
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.REFERENCE_CYCLE_CHARGING_WAIT))
				.next(new TestCase().timeleap(clock, WAIT_TIME_MINUTES, ChronoUnit.MINUTES))
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.REFERENCE_CYCLE_DISCHARGING))
				.next(new TestCase()//
						.input(ESS_ID, SOC, 0))
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.REFERENCE_CYCLE_DISCHARGING_WAIT))
				.next(new TestCase().timeleap(clock, WAIT_TIME_MINUTES, ChronoUnit.MINUTES))
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.MEASUREMENT_CYCLE_CHARGING))
				// Complete measurement charging
				.next(new TestCase()//
						.input(ESS_ID, SOC, 100))
				.next(new TestCase()//
						.output(STATE_MACHINE, StateMachine.State.MEASUREMENT_CYCLE_CHARGING_WAIT))
				.next(new TestCase().timeleap(clock, WAIT_TIME_MINUTES, ChronoUnit.MINUTES));
		return test;
	}


	/**
	 * Asserts that the baseline matches the expected value.
	 * @param controller the controller to check
	 * @param expected   the expected baseline value
	 */
	private void assertBaseline(ControllerEssSohCycleImpl controller, long expected) {
		final var captured = controller.getMeasurementStartEnergyWh();
		if (captured == null || captured != expected) {
			throw new AssertionError("Expected baseline=" + expected + ", but was " + captured);
		}
	}

	/**
	 * Asserts that the measured capacity channel matches the expected value.
	 * @param controller the controller to check
	 * @param expectedWh the expected measured capacity value in Wh
	 */
	private void assertMeasuredCapacity(ControllerEssSohCycleImpl controller, long expectedWh) {
		final var channelValue = controller.getMeasuredCapacityChannel().value().get();
		if (channelValue == null || channelValue != expectedWh) {
			throw new AssertionError("Expected MEASURED_CAPACITY=" + expectedWh + " Wh, but was " + channelValue);
		}
	}

	/**
	 * Asserts that the measured capacity channel has been cleared (is null).
	 * @param controller the controller to check
	 */
	private void assertMeasuredCapacityCleared(ControllerEssSohCycleImpl controller) {
		final var channelValue = controller.getMeasuredCapacityChannel().value().get();
		if (channelValue != null) {
			throw new AssertionError("Expected MEASURED_CAPACITY to be null, but was " + channelValue);
		}
	}

	private void runPrepareForReferenceCycle(int maxApparentPower, int capacity)
			throws Exception {
		final var clock = this.createDefaultClock();
		final var power = new DummyPower(maxApparentPower);
		final var ess = this.createEss(maxApparentPower, capacity);
		final var test = this.createControllerTest(clock, ess, new ControllerEssSohCycleImpl());

		power.addEss(ess);

		test.next(new TestCase()
					.input(STATE_MACHINE, StateMachine.State.IDLE)
					.input(ESS_ID, SOC, 50))
				.next(new TestCase("Preparing Reference")
						.output(STATE_MACHINE, StateMachine.State.PREPARE))
				.next(new TestCase("Verify power is being set while discharging")
						.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, 1000))
				.next(new TestCase().input(ESS_ID, SOC, 0))
				.next(new TestCase().output(STATE_MACHINE, StateMachine.State.MEASUREMENT_CYCLE_CHARGING))
				.deactivate();
	}

	private void runCycleRampUntilWait(StateMachine.State activeState, StateMachine.State waitState, int[] socSamples,
			int powerSign) throws Exception {
		final var clock = this.createDefaultClock();
		final var rampStep = (int) (DEFAULT_MAX_APPARENT_POWER * 0.01);
		final var power = new DummyPower(DEFAULT_MAX_APPARENT_POWER);
		final var ess = this.createDefaultEss();

		final var test = new ControllerTest(
				ControllerEssSohCycleImpl.startIn(activeState))
				.addReference("componentManager", new DummyComponentManager(clock))
				.addReference("cm", this.cm)
				.addReference("ess", ess)
				.addReference("sum", new DummySum())
				.activate(MyConfig.create()
					.setId(CONTROLLER_ID)
					.setEssId(ESS_ID)
					.setRunning(true)
					.build());

		power.addEss(ess);

		var controller = test;
		for (int i = 0; i < socSamples.length; i++) {
			final var description = i < 5
					? "Cycle " + (i + 1) + ": Ramp step"
					: "Continue at SoC";
			final var testCase = new TestCase(description)
					.input(ESS_ID, SOC, socSamples[i]);

			if (i < socSamples.length - 1) {
				testCase.output(STATE_MACHINE, activeState);
			}

			if (i < 5) {
				testCase.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, powerSign * rampStep * (i + 1));
			}

			controller = controller.next(testCase);
		}

		controller.next(new TestCase("Switch to wait once fully charged")
				.output(STATE_MACHINE, waitState)
				.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, ZERO_WATT_POWER))
				.deactivate();
	}

	private TimeLeapClock createDefaultClock() {
		return new TimeLeapClock(Instant.parse(DEFAULT_CLOCK_INSTANT), ZoneOffset.UTC);
	}

	private DummyManagedSymmetricEss createDefaultEss() {
		return this.createEss(DEFAULT_MAX_APPARENT_POWER, DEFAULT_CAPACITY);
	}

	private DummyManagedSymmetricEss createEss(int maxApparentPower, int capacity) {
		return new DummyManagedSymmetricEss(ESS_ID)
				.withMaxApparentPower(maxApparentPower)
				.withCapacity(capacity);
	}

	private ControllerTest createControllerTest(TimeLeapClock clock, DummyManagedSymmetricEss ess,
			ControllerEssSohCycleImpl controller) throws Exception {
		return this.createControllerTest(clock, ess, controller, false);
	}

	private ControllerTest createControllerTest(TimeLeapClock clock, DummyManagedSymmetricEss ess,
			ControllerEssSohCycleImpl controller, boolean referenceCycleEnabled) throws Exception {
		return new ControllerTest(controller)
				.addReference("componentManager", new DummyComponentManager(clock))
				.addReference("cm", this.cm)
				.addReference("ess", ess)
				.addReference("sum", new DummySum())
				.activate(MyConfig.create()
						.setId(CONTROLLER_ID)
						.setEssId(ESS_ID)
						.setRunning(true)
						.setReferenceCycleEnabled(referenceCycleEnabled)
						.build());
	}
}
