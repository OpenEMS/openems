package io.openems.edge.controller.ess.sohcycle;

import static io.openems.edge.common.sum.Sum.ChannelId.PRODUCTION_DC_ACTUAL_POWER;
import static io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycle.ChannelId.STATE_MACHINE;
import static io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycleImpl.ZERO_WATT_POWER;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.MAX_CELL_VOLTAGE;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.MIN_CELL_VOLTAGE;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.SOC;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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

class ControllerEssSohCycleTest {

	private final DummyConfigurationAdmin cm = new DummyConfigurationAdmin();

	private static final String ESS_ID = "ess0";
	private	static final String CONTROLLER_ID = "ctrl0";
	private static final int DEFAULT_MAX_APPARENT_POWER = 10_000;
	private static final int DEFAULT_CAPACITY = 10_000;
	private static final String DEFAULT_CLOCK_INSTANT = "2000-01-01T00:00:00.00Z";
	private static final int WAIT_TIME_MINUTES = 30;

	@Test
	void testCompleteSohCycleFromIdleToDone() throws Exception {
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
							assertEquals(Boolean.FALSE, config.getProperties().get("isRunning"),
									"Running should switch to false after DONE->IDLE");
							this.assertMeasuredCapacity(controller, 15_000L);
						}))
				.deactivate();


	}

	@Test
	void testPrepareForReferenceCycleWithHighPowerLowCapacity() throws Exception {
		this.runPrepareForReferenceCycle(10_000, 5_000);
	}

	@Test
	void testPrepareForReferenceCycleWithLowPowerHighCapacity() throws Exception {
		this.runPrepareForReferenceCycle(1_000, 10_000);
	}


	@Test
	void testWaitResetsRampAndPreservesDcCompensation() throws Exception {
		final var clock = this.createDefaultClock();
		final var power = new DummyPower(DEFAULT_MAX_APPARENT_POWER);
		final var ess = this.createDefaultEss();
		final var controller = new ControllerEssSohCycleImpl(StateMachine.State.REFERENCE_CYCLE_CHARGING, null);
		final var test = this.createControllerTest(clock, ess, controller);
		power.addEss(ess);

		test.next(new TestCase("Charging before WAIT") //
				.input("_sum", PRODUCTION_DC_ACTUAL_POWER, 1000) //
				.input(ESS_ID, SOC, 50) //
				.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, -1000)) //
				.next(new TestCase("Reach charging endpoint") //
						.input(ESS_ID, SOC, 100) //
						.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, 1000)) //
				.next(new TestCase("First WAIT cycle requests neutral battery power with DC production") //
						.output(STATE_MACHINE, StateMachine.State.REFERENCE_CYCLE_CHARGING_WAIT) //
						.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, 1000)) //
				.next(new TestCase("WAIT requests zero ESS power without DC production") //
						.input("_sum", PRODUCTION_DC_ACTUAL_POWER, 0) //
						.output(STATE_MACHINE, StateMachine.State.REFERENCE_CYCLE_CHARGING_WAIT) //
						.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, ZERO_WATT_POWER)) //
				.next(new TestCase("Still resting just before 30 minutes") //
						.timeleap(clock, WAIT_TIME_MINUTES * 60 - 1, ChronoUnit.SECONDS) //
						.input("_sum", PRODUCTION_DC_ACTUAL_POWER, 2000) //
						.output(STATE_MACHINE, StateMachine.State.REFERENCE_CYCLE_CHARGING_WAIT) //
						.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, 2000)) //
				.next(new TestCase("Last WAIT cycle at 30 minutes") //
						.timeleap(clock, 1, ChronoUnit.SECONDS) //
						.output(STATE_MACHINE, StateMachine.State.REFERENCE_CYCLE_CHARGING_WAIT) //
						.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, 2000)) //
				.next(new TestCase("Normal transition after WAIT") //
						.input("_sum", PRODUCTION_DC_ACTUAL_POWER, 2000) //
						.output(STATE_MACHINE, StateMachine.State.REFERENCE_CYCLE_DISCHARGING) //
						.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, 2100)) //
				.next(new TestCase("Continue ramping with DC compensation") //
						.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, 2200)) //
				.deactivate();
	}

	@Test
	void testReferenceCycleChargingWithPowerRampUp() throws Exception {
		this.runCycleRampUntilWait(StateMachine.State.REFERENCE_CYCLE_CHARGING,
				StateMachine.State.REFERENCE_CYCLE_CHARGING_WAIT,
				new int[] { 0, 10, 20, 30, 40, 80, 100 },
				-1);
	}

	@Test
	void testReferenceCycleDischargingWithPowerRampUp() throws Exception {
		this.runCycleRampUntilWait(StateMachine.State.REFERENCE_CYCLE_DISCHARGING,
				StateMachine.State.REFERENCE_CYCLE_DISCHARGING_WAIT,
				new int[] { 100, 90, 80, 70, 60, 20, 0 },
				1);
	}

	@Test
	void testMeasurementCycleChargingWithPowerRampUp() throws Exception {
		this.runCycleRampUntilWait(StateMachine.State.MEASUREMENT_CYCLE_CHARGING,
				StateMachine.State.MEASUREMENT_CYCLE_CHARGING_WAIT,
				new int[] { 0, 10, 20, 30, 40, 80, 100 },
				-1);
	}

	@Test
	void testMeasurementCycleDischargingWithPowerRampUp() throws Exception {
		this.runCycleRampUntilWait(StateMachine.State.MEASUREMENT_CYCLE_DISCHARGING,
				StateMachine.State.MEASUREMENT_CYCLE_DISCHARGING_WAIT,
				new int[] { 100, 90, 80, 70, 60, 20, 0 },
				1);
	}

	@Test
	void testMeasurementBaselineCapturedAndAvailableAtEvaluate() throws Exception {
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
						.output(STATE_MACHINE, StateMachine.State.MEASUREMENT_CYCLE_DISCHARGING_WAIT))//
				.next(new TestCase()//
						.timeleap(clock, WAIT_TIME_MINUTES, ChronoUnit.MINUTES))//
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
	void testMeasuredCapacityClearedWhenStartingNewCycle() throws Exception {
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
				.next(new TestCase("Starting a new run clears previous measured capacity")//
						.output(STATE_MACHINE, StateMachine.State.PREPARE)//
						.onAfterControllersCallbacks(() -> {
							// Verify the capacity is cleared when starting new cycle
							this.assertMeasuredCapacityCleared(controller);
						}))
				.deactivate();
	}

	@Test
	void testPrepareReferenceSkipsReferenceCycleWhenDisabled() throws Exception {
		final var clock = this.createDefaultClock();
		final var power = new DummyPower(DEFAULT_MAX_APPARENT_POWER);
		final var ess = this.createDefaultEss();
		final var controller = new ControllerEssSohCycleImpl();
		final var test = this.createControllerTest(clock, ess, controller, false);
		power.addEss(ess);

		test.next(new TestCase()//
				.input(STATE_MACHINE, StateMachine.State.IDLE)//
				.input(ESS_ID, SOC, 50))//
				.next(new TestCase("Preparing Reference")//
						.output(STATE_MACHINE, StateMachine.State.PREPARE))//
				.next(new TestCase()//
						.input(ESS_ID, SOC, 0))//
				.next(new TestCase("Should skip Reference Cycle and go directly to Measurement Cycle")//
						.output(STATE_MACHINE, StateMachine.State.MEASUREMENT_CYCLE_CHARGING))//
				.deactivate();
	}


	@Test
	void testPrepareSkipsPrechargeWhenTargetSocAlreadyReached() throws Exception {
		final var clock = this.createDefaultClock();
		final var power = new DummyPower(DEFAULT_MAX_APPARENT_POWER);
		final var ess = this.createDefaultEss();
		final var test = this.createControllerTest(clock, ess, new ControllerEssSohCycleImpl());
		power.addEss(ess);

		test.next(new TestCase()//
						.input(STATE_MACHINE, StateMachine.State.IDLE)//
						.input(ESS_ID, SOC, 0)//
				// Even with discharge unavailable, target SoC is already reached, so no
				// precharge is required.
				.input(ESS_ID, ALLOWED_DISCHARGE_POWER, 0))//
				.next(new TestCase("Preparing Reference")//
						.output(STATE_MACHINE, StateMachine.State.PREPARE))//
				.next(new TestCase("Target SoC already reached: no precharge, proceed with existing flow")//
						.output(STATE_MACHINE, StateMachine.State.MEASUREMENT_CYCLE_CHARGING))//
				.deactivate();
	}

	@Test
	void testPrepareContinuesNormalDischargeWithoutPrecharge() throws Exception {
		final var clock = this.createDefaultClock();
		final var power = new DummyPower(DEFAULT_MAX_APPARENT_POWER);
		final var ess = this.createDefaultEss();
		final var test = this.createControllerTest(clock, ess, new ControllerEssSohCycleImpl());
		power.addEss(ess);

		var controller = test.next(new TestCase()//
						.input(STATE_MACHINE, StateMachine.State.IDLE)//
						.input(ESS_ID, SOC, 100)//
				.input(ESS_ID, ALLOWED_DISCHARGE_POWER, 1000))//
				.next(new TestCase("Undefined discharge capability: keeps normal PREPARE power request")//
						.input(ESS_ID, SOC, 100)//
						.input(ESS_ID, ALLOWED_DISCHARGE_POWER, null)//
						.output(STATE_MACHINE, StateMachine.State.PREPARE)//
						.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, 2000));

		controller = controller.next(new TestCase("SoC is low but above 0%: stays in PREPARE")//
				.input(ESS_ID, SOC, 50)//
				.input(ESS_ID, ALLOWED_DISCHARGE_POWER, 1000)//
				.output(STATE_MACHINE, StateMachine.State.PREPARE));

		controller.next(new TestCase("SoC reaches 0%: remains in PREPARE for this cycle")//
				.input(ESS_ID, SOC, 0)//
				.output(STATE_MACHINE, StateMachine.State.PREPARE))//
				.next(new TestCase("Reached 0%: proceeds with existing flow")//
						.output(STATE_MACHINE, StateMachine.State.MEASUREMENT_CYCLE_CHARGING))//
				.deactivate();
	}

	@Test
	void testPreparePrechargeResetsDischargeRampBeforeCharging() throws Exception {
		final var clock = this.createDefaultClock();
		final var ess = this.createDefaultEss();
		final var controller = new ControllerEssSohCycleImpl(StateMachine.State.PREPARE, 0f);
		final var test = this.createControllerTest(clock, ess, controller);
		new DummyPower(DEFAULT_MAX_APPARENT_POWER).addEss(ess);

		test.next(new TestCase("PREPARE establishes a positive discharge ramp") //
				.input(ESS_ID, SOC, 50) //
				.input(ESS_ID, ALLOWED_DISCHARGE_POWER, 1000) //
				.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, 100) //
				.output(STATE_MACHINE, StateMachine.State.PREPARE)) //
				.next(new TestCase("Entering PREPARE_PRECHARGE requests neutral battery power") //
						.input(ESS_ID, ALLOWED_DISCHARGE_POWER, 0) //
						.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, ZERO_WATT_POWER) //
						.output(STATE_MACHINE, StateMachine.State.PREPARE)) //
				.next(new TestCase("PREPARE_PRECHARGE ramps into negative charging power") //
						.input(ESS_ID, SOC, 50) //
						.input(ESS_ID, ALLOWED_DISCHARGE_POWER, 0) //
						.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, -100) //
						.output(STATE_MACHINE, StateMachine.State.PREPARE_PRECHARGE)) //
				.next(new TestCase("Charging ramp continues from zero") //
						.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, -200) //
						.output(STATE_MACHINE, StateMachine.State.PREPARE_PRECHARGE)) //
				.deactivate();
	}

	@Test
	void testPrepareRecoveryIsSingleUseAndSecondBlockMustBeContinuousForFiveMinutes() throws Exception {
		final var clock = this.createDefaultClock();
		final var power = new DummyPower(DEFAULT_MAX_APPARENT_POWER);
		final var ess = this.createDefaultEss();
		final var test = this.createControllerTest(clock, ess, new ControllerEssSohCycleImpl());
		power.addEss(ess);

		test.next(new TestCase()//
						.input(STATE_MACHINE, StateMachine.State.IDLE)//
						.input(ESS_ID, SOC, 50).input(ESS_ID, ALLOWED_DISCHARGE_POWER, 0))//
				.next(new TestCase("Preparing Reference")//
						.output(STATE_MACHINE, StateMachine.State.PREPARE))//
				.next(new TestCase("First discharge block: enters PREPARE_PRECHARGE")//
						.input(ESS_ID, ALLOWED_DISCHARGE_POWER, 0)//
						.output(STATE_MACHINE, StateMachine.State.PREPARE_PRECHARGE))//
				.next(new TestCase("Discharge becomes available: returns to PREPARE")//
						.input(ESS_ID, ALLOWED_DISCHARGE_POWER, 1000)//
						.output(STATE_MACHINE, StateMachine.State.PREPARE_PRECHARGE))//
				.next(new TestCase("PREPARE resumes normal discharging")//
						.input(ESS_ID, ALLOWED_DISCHARGE_POWER, 1000)//
						.output(STATE_MACHINE, StateMachine.State.PREPARE))//
				.next(new TestCase("Second block starts endpoint timer")//
						.input(ESS_ID, ALLOWED_DISCHARGE_POWER, 0)//
						.output(STATE_MACHINE, StateMachine.State.PREPARE))//
				.next(new TestCase("Recovery within five minutes resets the blocking interval")//
						.timeleap(clock, 299, ChronoUnit.SECONDS)//
						.input(ESS_ID, ALLOWED_DISCHARGE_POWER, 1000)//
						.output(STATE_MACHINE, StateMachine.State.PREPARE))//
				.next(new TestCase("A new block starts a fresh interval")//
						.timeleap(clock, 1, ChronoUnit.SECONDS)//
						.input(ESS_ID, ALLOWED_DISCHARGE_POWER, 0)//
						.output(STATE_MACHINE, StateMachine.State.PREPARE))//
				.next(new TestCase("Undefined capability clears the blocking interval")//
						.timeleap(clock, 299, ChronoUnit.SECONDS)//
						.input(ESS_ID, ALLOWED_DISCHARGE_POWER, null)//
						.output(STATE_MACHINE, StateMachine.State.PREPARE))//
				.next(new TestCase("Undefined capability is not endpoint confirmation")//
						.timeleap(clock, 1, ChronoUnit.SECONDS)//
						.input(ESS_ID, ALLOWED_DISCHARGE_POWER, 0)//
						.output(STATE_MACHINE, StateMachine.State.PREPARE))//
				.next(new TestCase("Still blocked before a continuous five minutes")//
						.timeleap(clock, 299, ChronoUnit.SECONDS)//
						.output(STATE_MACHINE, StateMachine.State.PREPARE))//
				.next(new TestCase("Continuous second block accepts lower endpoint")//
						.timeleap(clock, 1, ChronoUnit.SECONDS)//
						.output(STATE_MACHINE, StateMachine.State.PREPARE)//
						.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, ZERO_WATT_POWER))//
				.next(new TestCase("Continue through PREPARE's normal next state")//
						.output(STATE_MACHINE, StateMachine.State.MEASUREMENT_CYCLE_CHARGING))//
				.deactivate();
	}

	@ParameterizedTest
	@CsvSource({ "0, 1799", "1000, 1799", "0, 1800", "1000, 1800" })
	void testPreparePrechargeReturnsToPrepareWhenDischargeRestoredBeforeOrAtTimeout(int dcProduction,
			int recoveryAfterSeconds)
			throws Exception {
		final var clock = this.createDefaultClock();
		final var power = new DummyPower(DEFAULT_MAX_APPARENT_POWER);
		final var ess = this.createDefaultEss();
		final var test = new ControllerTest(//
				new ControllerEssSohCycleImpl(StateMachine.State.PREPARE_PRECHARGE, null))//
				.addReference("componentManager", new DummyComponentManager(clock))//
				.addReference("cm", this.cm)//
				.addReference("ess", ess)//
				.addReference("sum", new DummySum())//
				.activate(MyConfig.create().setId(CONTROLLER_ID).setEssId(ESS_ID).setRunning(true).build());
		power.addEss(ess);

		test.next(new TestCase("PREPARE_PRECHARGE charges while waiting")//
				.input("_sum", PRODUCTION_DC_ACTUAL_POWER, dcProduction)//
						.input(ESS_ID, SOC, 50)//
						.input(ESS_ID, ALLOWED_DISCHARGE_POWER, 0)//
				.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, dcProduction - 2000)//
						.output(STATE_MACHINE, StateMachine.State.PREPARE_PRECHARGE))//
				.next(new TestCase("Still charging after the former five-minute timeout")//
						.timeleap(clock, 5, ChronoUnit.MINUTES)//
						.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, dcProduction - 2000)//
						.output(STATE_MACHINE, StateMachine.State.PREPARE_PRECHARGE))//
				.next(new TestCase("Discharge recovery wins before or exactly at timeout")//
						.timeleap(clock, recoveryAfterSeconds - 300, ChronoUnit.SECONDS)//
						.input(ESS_ID, ALLOWED_DISCHARGE_POWER, 1000)//
						.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, dcProduction)//
						.output(STATE_MACHINE, StateMachine.State.PREPARE_PRECHARGE))//
				.next(new TestCase("PREPARE discharge ramps from zero")//
						.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, dcProduction + 100)//
						.output(STATE_MACHINE, StateMachine.State.PREPARE))//
				.next(new TestCase("Continue ramping in the discharge direction")//
						.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, dcProduction + 200)//
						.output(STATE_MACHINE, StateMachine.State.PREPARE))//
				.deactivate();
	}

	@Test
	void testPreparePrechargeAbortsAfterThirtyMinutesWithoutRecovery() throws Exception {
		final var clock = this.createDefaultClock();
		final var power = new DummyPower(DEFAULT_MAX_APPARENT_POWER);
		final var ess = this.createDefaultEss();
		final var test = new ControllerTest(//
				new ControllerEssSohCycleImpl(StateMachine.State.PREPARE_PRECHARGE, null))//
				.addReference("componentManager", new DummyComponentManager(clock))//
				.addReference("cm", this.cm)//
				.addReference("ess", ess)//
				.addReference("sum", new DummySum())//
				.activate(MyConfig.create()//
						.setId(CONTROLLER_ID)//
						.setEssId(ESS_ID)//
						.setRunning(true)//
						.build());
		power.addEss(ess);

		test.next(new TestCase("PREPARE_PRECHARGE waits when charge is unavailable")
						.input(ESS_ID, SOC, 50)//
						.input(ESS_ID, ALLOWED_DISCHARGE_POWER, 0)//
						.input(ESS_ID, ALLOWED_CHARGE_POWER, 0)//
						.output(STATE_MACHINE, StateMachine.State.PREPARE_PRECHARGE))//
				.next(new TestCase("Discharge remains blocked before timeout")//
						.timeleap(clock, 1799, ChronoUnit.SECONDS)//
						.output(STATE_MACHINE, StateMachine.State.PREPARE_PRECHARGE))//
				.next(new TestCase("No recovery at timeout aborts the cycle")//
						.timeleap(clock, 1, ChronoUnit.SECONDS)//
						.output(STATE_MACHINE, StateMachine.State.PREPARE_PRECHARGE)//
						.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, ZERO_WATT_POWER))//
				.next(new TestCase("Timeout does not continue into a charging cycle")//
						.output(STATE_MACHINE, StateMachine.State.ERROR_ABORT))//
				.deactivate();
	}

	@Test
	void testPreparePrechargeAbortsWhenRequiredPowerDetailsAreMissing() throws Exception {
		final var clock = this.createDefaultClock();
		final var ess = new DummyManagedSymmetricEss(ESS_ID).withMaxApparentPower(DEFAULT_MAX_APPARENT_POWER);
		new DummyPower(DEFAULT_MAX_APPARENT_POWER).addEss(ess);
		final var controller = new ControllerEssSohCycleImpl(StateMachine.State.PREPARE_PRECHARGE, null);
		final var test = this.createControllerTest(clock, ess, controller);

		test.next(new TestCase("Missing capacity prevents a valid PREPARE_PRECHARGE power request") //
				.input(ESS_ID, SOC, 50) //
				.input(ESS_ID, ALLOWED_DISCHARGE_POWER, 0) //
				.output(STATE_MACHINE, StateMachine.State.PREPARE_PRECHARGE)) //
				.next(new TestCase("Missing required power details abort the run") //
				.output(STATE_MACHINE, StateMachine.State.ERROR_ABORT)) //
				.deactivate();
	}

	@Test
	void testPreparePrechargeDoesNotTreatUndefinedDischargeAsRecovery() throws Exception {
		final var clock = this.createDefaultClock();
		final var power = new DummyPower(DEFAULT_MAX_APPARENT_POWER);
		final var ess = this.createDefaultEss();
		final var test = new ControllerTest(//
				new ControllerEssSohCycleImpl(StateMachine.State.PREPARE_PRECHARGE, null))//
				.addReference("componentManager", new DummyComponentManager(clock))//
				.addReference("cm", this.cm)//
				.addReference("ess", ess)//
				.addReference("sum", new DummySum())//
				.activate(MyConfig.create()//
						.setId(CONTROLLER_ID)//
						.setEssId(ESS_ID)//
						.setRunning(true)//
						.build());
		power.addEss(ess);

		test.next(new TestCase("Undefined discharge capability is not recovery")//
				.input(ESS_ID, SOC, 50)//
				.output(STATE_MACHINE, StateMachine.State.PREPARE_PRECHARGE))//
				.next(new TestCase("Remain in recovery while discharge availability is undefined")//
						.output(STATE_MACHINE, StateMachine.State.PREPARE_PRECHARGE))//
				.deactivate();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testPreparePrechargeTimeoutAtNominalSocAborts(
			boolean referenceCycleEnabled) throws Exception {
		final var clock = this.createDefaultClock();
		final var ess = this.createDefaultEss();
		new DummyPower(DEFAULT_MAX_APPARENT_POWER).addEss(ess);
		final var controller = new ControllerEssSohCycleImpl(StateMachine.State.PREPARE_PRECHARGE, null);
		final var test = this.createControllerTest(clock, ess, controller, referenceCycleEnabled);

		test.next(new TestCase("Start recovery") //
				.input(ESS_ID, SOC, 50) //
				.input(ESS_ID, ALLOWED_DISCHARGE_POWER, 0)) //
				.next(new TestCase("Remain in recovery just before timeout") //
						.timeleap(clock, 1799, ChronoUnit.SECONDS) //
						.output(STATE_MACHINE, StateMachine.State.PREPARE_PRECHARGE)) //
				.next(new TestCase("Abort at timeout even at nominal lower endpoint") //
						.timeleap(clock, 1, ChronoUnit.SECONDS) //
						.input(ESS_ID, SOC, 0) //
						.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, ZERO_WATT_POWER)) //
				.next(new TestCase("Neither reference nor measurement charging follows timeout") //
						.output(STATE_MACHINE, StateMachine.State.ERROR_ABORT))//
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
				ControllerEssSohCycleImpl.startIn(activeState))//
				.addReference("componentManager", new DummyComponentManager(clock))//
				.addReference("cm", this.cm)//
				.addReference("ess", ess)//
				.addReference("sum", new DummySum())//
				.activate(MyConfig.create()//
						.setId(CONTROLLER_ID)//
						.setEssId(ESS_ID)//
						.setRunning(true)//
					.build());

		power.addEss(ess);

		var controller = test;
		for (int i = 0; i < socSamples.length; i++) {
			final var description = i == socSamples.length - 1 ? "Enter WAIT at SoC endpoint"
					: i < 5 ? "Cycle " + (i + 1) + ": Ramp step" : "Continue at SoC";
			final var testCase = new TestCase(description)
					.input(ESS_ID, SOC, socSamples[i]);

			if (i < socSamples.length - 1) {
				testCase.output(STATE_MACHINE, activeState);
			}

			if (i == socSamples.length - 1) {
				testCase.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, ZERO_WATT_POWER);
			} else {
				testCase.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, powerSign * rampStep * (i + 1));
			}

			controller = controller.next(testCase);
		}

		controller.next(new TestCase("WAIT requests zero power without resetting the ramp")//
				.output(STATE_MACHINE, waitState)//
				.output(ESS_ID, SET_ACTIVE_POWER_EQUALS, ZERO_WATT_POWER))//
				.deactivate();
	}

	private TimeLeapClock createDefaultClock() {
		return new TimeLeapClock(Instant.parse(DEFAULT_CLOCK_INSTANT), ZoneOffset.UTC);
	}

	private DummyManagedSymmetricEss createDefaultEss() {
		return this.createEss(DEFAULT_MAX_APPARENT_POWER, DEFAULT_CAPACITY);
	}

	private DummyManagedSymmetricEss createEss(int maxApparentPower, int capacity) {
		return new DummyManagedSymmetricEss(ESS_ID)//
				.withMaxApparentPower(maxApparentPower)//
				.withCapacity(capacity);
	}

	private ControllerTest createControllerTest(TimeLeapClock clock, DummyManagedSymmetricEss ess,
			ControllerEssSohCycleImpl controller) throws Exception {
		return this.createControllerTest(clock, ess, controller, false);
	}

	private ControllerTest createControllerTest(TimeLeapClock clock, DummyManagedSymmetricEss ess,
			ControllerEssSohCycleImpl controller, boolean referenceCycleEnabled) throws Exception {
		return new ControllerTest(controller)//
				.addReference("componentManager", new DummyComponentManager(clock))//
				.addReference("cm", this.cm)//
				.addReference("ess", ess)//
				.addReference("sum", new DummySum())//
				.activate(MyConfig.create()//
						.setId(CONTROLLER_ID)//
						.setEssId(ESS_ID)//
						.setRunning(true)//
						.setReferenceCycleEnabled(referenceCycleEnabled)//
						.build());
	}
}
