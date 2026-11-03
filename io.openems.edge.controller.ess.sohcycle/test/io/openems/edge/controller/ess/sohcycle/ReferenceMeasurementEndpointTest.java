package io.openems.edge.controller.ess.sohcycle;

import static io.openems.edge.common.sum.Sum.ChannelId.PRODUCTION_DC_ACTUAL_POWER;
import static io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycle.ChannelId.IS_MEASURED;
import static io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycle.ChannelId.MEASURED_CAPACITY;
import static io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycle.ChannelId.SOH_PERCENT;
import static io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycle.ChannelId.STATE_MACHINE;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.MAX_CELL_VOLTAGE;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.MIN_CELL_VOLTAGE;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.SOC;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.ess.sohcycle.statemachine.StateMachine.State;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;

class ReferenceMeasurementEndpointTest {

	private final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2026-01-01T00:00:00Z"));
	private ControllerEssSohCycleImpl controller;

	private static Stream<Arguments> phases() {
		return Stream.of(
				Arguments.of(State.REFERENCE_CYCLE_CHARGING, State.REFERENCE_CYCLE_CHARGING_WAIT, true),
				Arguments.of(State.REFERENCE_CYCLE_DISCHARGING, State.REFERENCE_CYCLE_DISCHARGING_WAIT, false),
				Arguments.of(State.MEASUREMENT_CYCLE_CHARGING, State.MEASUREMENT_CYCLE_CHARGING_WAIT, true),
				Arguments.of(State.MEASUREMENT_CYCLE_DISCHARGING, State.MEASUREMENT_CYCLE_DISCHARGING_WAIT, false));
	}

	private static Stream<Arguments> representativeTimerResetPhases() {
		return Stream.of(
				Arguments.of(State.REFERENCE_CYCLE_CHARGING, State.REFERENCE_CYCLE_CHARGING_WAIT, true),
				Arguments.of(State.MEASUREMENT_CYCLE_DISCHARGING, State.MEASUREMENT_CYCLE_DISCHARGING_WAIT, false));
	}

	@ParameterizedTest
	@CsvSource({ "true, false", "false, false", "true, true", "false, true" })
	void oppositeDirectionRampsFromZeroAfterWait(boolean charging, boolean earlyEndpoint) throws Exception {
		var active = charging ? State.REFERENCE_CYCLE_CHARGING : State.REFERENCE_CYCLE_DISCHARGING;
		var wait = charging ? State.REFERENCE_CYCLE_CHARGING_WAIT : State.REFERENCE_CYCLE_DISCHARGING_WAIT;
		var next = charging ? State.REFERENCE_CYCLE_DISCHARGING : State.MEASUREMENT_CYCLE_CHARGING;
		int sign = charging ? -1 : 1;
		int initialSoc = charging ? 97 : 7;
		int endpointSoc = charging ? 100 : 0;
		var test = this.createTest(active);
		test.next(new TestCase("Establish ramp in the original direction") //
				.input("ess0", SOC, initialSoc) //
				.input("ess0", charging ? ALLOWED_CHARGE_POWER : ALLOWED_DISCHARGE_POWER,
						earlyEndpoint ? 0 : sign * 1000) //
				.output("ess0", SET_ACTIVE_POWER_EQUALS, sign * 100)) //
				.next(new TestCase("Continue ramp before endpoint") //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, sign * 200)) //
				.next(new TestCase("Accept nominal or early endpoint") //
						.timeleap(this.clock, 5, ChronoUnit.MINUTES) //
						.input("ess0", SOC, earlyEndpoint ? initialSoc : endpointSoc) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0)) //
				.next(new TestCase("WAIT holds neutral battery power") //
						.output(STATE_MACHINE, wait) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0)) //
				.next(new TestCase("Finish WAIT with neutral battery power") //
						.timeleap(this.clock, 30, ChronoUnit.MINUTES) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0)) //
				.next(new TestCase("First opposite-direction ramp step starts at zero") //
						.output(STATE_MACHINE, next) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -sign * 100)) //
				.next(new TestCase("Normal ramp continues in the new direction") //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -sign * 200)) //
				.deactivate();
	}

	@ParameterizedTest
	@MethodSource("phases")
	void nominalEndpointHasPriorityOverBlocking(State active, State wait, boolean charging) throws Exception {
		var test = this.createTest(active);
		test.next(new TestCase("Start blocked below nominal endpoint") //
				.input("ess0", SOC, charging ? 97 : 7) //
				.input("ess0", charging ? ALLOWED_CHARGE_POWER : ALLOWED_DISCHARGE_POWER, 0) //
				.output(STATE_MACHINE, active)) //
				.next(new TestCase("Nominal endpoint completes without waiting five minutes") //
						.timeleap(this.clock, 1, ChronoUnit.MINUTES) //
						.input("ess0", SOC, charging ? 100 : 0)) //
				.next(new TestCase("Normal WAIT state") //
						.output(STATE_MACHINE, wait) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0)) //
				.deactivate();
	}

	@ParameterizedTest
	@MethodSource("phases")
	void continuousBlockingCompletesAtExactlyFiveMinutes(State active, State wait, boolean charging) throws Exception {
		var test = this.createTest(active);
		test.next(new TestCase("Start blocked without initial onEntry") //
				.input("_sum", PRODUCTION_DC_ACTUAL_POWER, 1000) //
				.input("ess0", SOC, charging ? 97 : 7) //
				.input("ess0", charging ? ALLOWED_CHARGE_POWER : ALLOWED_DISCHARGE_POWER, 0) //
				.input("ess0", MIN_CELL_VOLTAGE, 3200) //
				.input("ess0", MAX_CELL_VOLTAGE, 3300) //
				.input("ess0", ACTIVE_DISCHARGE_ENERGY, 12_000L) //
				.output(STATE_MACHINE, active) //
				.output("ess0", SET_ACTIVE_POWER_EQUALS, charging ? 900 : 1100)) //
				.next(new TestCase("Still active just before five minutes") //
						.timeleap(this.clock, 299, ChronoUnit.SECONDS) //
						.output(STATE_MACHINE, active) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, charging ? 800 : 1200)) //
				.next(new TestCase("Accepted endpoint requests zero battery power with DC production") //
						.timeleap(this.clock, 1, ChronoUnit.SECONDS) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 1000) //
						.onAfterControllersCallbacks(() -> {
							if (active == State.MEASUREMENT_CYCLE_CHARGING) {
								assertEquals(3200, this.controller.getMeasurementChargingMinVoltage());
								assertEquals(3300, this.controller.getMeasurementChargingMaxVoltage());
							}
							if (active == State.MEASUREMENT_CYCLE_DISCHARGING) {
								assertEquals(12_000L, this.controller.getMeasurementStartEnergyWh());
							}
						})) //
				.next(new TestCase("Same successful WAIT path") //
						.output(STATE_MACHINE, wait) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 1000)) //
				.deactivate();
	}

	@ParameterizedTest
	@MethodSource("representativeTimerResetPhases")
	void capabilityRecoveryResetsBlocking(State active, State wait, boolean charging) throws Exception {
		this.runInterruptedBlocking(active, wait, charging, false);
	}

	@ParameterizedTest
	@MethodSource("representativeTimerResetPhases")
	void undefinedCapabilityResetsBlockingWithoutConfirmingEndpoint(State active, State wait, boolean charging)
			throws Exception {
		this.runInterruptedBlocking(active, wait, charging, true);
	}

	private void runInterruptedBlocking(State active, State wait, boolean charging, boolean undefined) throws Exception {
		var test = this.createTest(active);
		var allowedPower = charging ? ALLOWED_CHARGE_POWER : ALLOWED_DISCHARGE_POWER;
		test.next(new TestCase("Start blocked") //
				.input("ess0", SOC, charging ? 97 : 7) //
				.input("ess0", allowedPower, 0)) //
				.next(new TestCase("Interruption clears timer and preserves normal power request") //
						.timeleap(this.clock, 299, ChronoUnit.SECONDS) //
						.input("ess0", allowedPower, undefined ? null : charging ? -1000 : 1000) //
						.output(STATE_MACHINE, active) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, charging ? -200 : 200)) //
				.next(new TestCase("Undefined or recovered capability cannot confirm endpoint") //
						.timeleap(this.clock, 5, ChronoUnit.MINUTES) //
						.output(STATE_MACHINE, active)) //
				.next(new TestCase("New explicit block starts a fresh interval") //
						.input("ess0", allowedPower, 0) //
						.output(STATE_MACHINE, active)) //
				.next(new TestCase("Still blocked for less than five new minutes") //
						.timeleap(this.clock, 299, ChronoUnit.SECONDS) //
						.output(STATE_MACHINE, active)) //
				.next(new TestCase("Five new minutes completes with zero power") //
						.timeleap(this.clock, 1, ChronoUnit.SECONDS) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0)) //
				.next(new TestCase("Successful WAIT") //
						.output(STATE_MACHINE, wait)) //
				.deactivate();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void measurementDischargePreservesEnergyThroughEvaluation(boolean earlyEndpoint) throws Exception {
		var test = this.createTest(State.MEASUREMENT_CYCLE_DISCHARGING);
		test.next(new TestCase("Capture discharge baseline") //
				.input("ess0", SOC, 7) //
				.input("ess0", ACTIVE_DISCHARGE_ENERGY, 12_000L) //
				.input("ess0", ALLOWED_DISCHARGE_POWER, earlyEndpoint ? 0 : 1000)) //
				.next(new TestCase("Complete at nominal or physical endpoint") //
						.timeleap(this.clock, 5, ChronoUnit.MINUTES) //
						.input("ess0", SOC, earlyEndpoint ? 7 : 0) //
						.input("ess0", ACTIVE_DISCHARGE_ENERGY, 17_000L)) //
				.next(new TestCase("Same discharge WAIT") //
						.output(STATE_MACHINE, State.MEASUREMENT_CYCLE_DISCHARGING_WAIT) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0) //
						.onAfterControllersCallbacks(() ->
								assertEquals(12_000L, this.controller.getMeasurementStartEnergyWh()))) //
				.next(new TestCase("Complete rest") //
						.timeleap(this.clock, 30, ChronoUnit.MINUTES)) //
				.next(new TestCase("Same downstream evaluation and unchanged baseline") //
						.output(STATE_MACHINE, State.EVALUATE_RESULT) //
						.output(MEASURED_CAPACITY, 5_000L) //
						.output(SOH_PERCENT, 50) //
						.output(IS_MEASURED, true) //
						.onAfterControllersCallbacks(() ->
								assertEquals(12_000L, this.controller.getMeasurementStartEnergyWh()))) //
				.next(new TestCase("Successful completion") //
						.output(STATE_MACHINE, State.DONE) //
						.output(MEASURED_CAPACITY, 5_000L)) //
				.deactivate();
	}

	private ControllerTest createTest(State initialState) throws Exception {
		var ess = new DummyManagedSymmetricEss("ess0").withCapacity(10_000).withMaxApparentPower(10_000);
		new DummyPower(10_000).addEss(ess);
		this.controller = ControllerEssSohCycleImpl.startIn(initialState);
		return new ControllerTest(this.controller) //
				.addReference("componentManager", new DummyComponentManager(this.clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ess) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create().setId("ctrl0").setEssId("ess0").setRunning(true).build());
	}
}
