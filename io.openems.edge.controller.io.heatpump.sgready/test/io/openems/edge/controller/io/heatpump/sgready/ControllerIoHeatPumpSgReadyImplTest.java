package io.openems.edge.controller.io.heatpump.sgready;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.sum.Sum.ChannelId.ESS_DISCHARGE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.ESS_SOC;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.controller.io.heatpump.sgready.ControllerIoHeatPumpSgReady.ChannelId.AWAITING_HYSTERESIS;
import static io.openems.edge.controller.io.heatpump.sgready.ControllerIoHeatPumpSgReady.ChannelId.STATUS;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT0;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT1;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ControllerIoHeatPumpSgReadyImplTest {

	@Test
	void manual_undefined_test() throws Exception {
		this.createDefaultControllerTest(null) //
				.activate(MyConfig.create() //
						.setId("ctrHeatPump0") //
						.setMode(Mode.MANUAL) //
						.setManualState(Status.UNDEFINED) //
						.setOutputChannel1("io0/InputOutput0") //
						.setOutputChannel2("io0/InputOutput1") //
						.setMeterId("") //
						.build())
				.next(new TestCase() //
						.output(STATUS, Status.REGULAR) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, false)) //
				.deactivate();
	}

	@Test
	void manual_regular_test() throws Exception {
		this.createDefaultControllerTest(null) //
				.activate(MyConfig.create() //
						.setId("ctrHeatPump0") //
						.setMode(Mode.MANUAL) //
						.setManualState(Status.REGULAR) //
						.setOutputChannel1("io0/InputOutput0") //
						.setOutputChannel2("io0/InputOutput1") //
						.setMeterId("") //
						.build())
				.next(new TestCase() //
						.output(STATUS, Status.REGULAR) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, false)) //
				.deactivate();
	}

	@Test
	void manual_recommendation_test() throws Exception {
		this.createDefaultControllerTest(null) //
				.activate(MyConfig.create() //
						.setId("ctrHeatPump0") //
						.setMode(Mode.MANUAL) //
						.setManualState(Status.RECOMMENDATION) //
						.setOutputChannel1("io0/InputOutput0") //
						.setOutputChannel2("io0/InputOutput1") //
						.setMeterId("") //
						.build())
				.next(new TestCase() //
						.output(STATUS, Status.RECOMMENDATION) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, true)) //
				.deactivate();
	}

	@Test
	void manual_force_on_test() throws Exception {
		this.createDefaultControllerTest(null) //
				.activate(MyConfig.create() //
						.setId("ctrHeatPump0") //
						.setMode(Mode.MANUAL) //
						.setManualState(Status.FORCE_ON) //
						.setOutputChannel1("io0/InputOutput0") //
						.setOutputChannel2("io0/InputOutput1") //
						.setMeterId("") //
						.build())
				.next(new TestCase() //
						.output(STATUS, Status.FORCE_ON) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true)) //
				.deactivate();
	}

	@Test
	void manual_lock_test() throws Exception {
		this.createDefaultControllerTest(null) //
				.activate(MyConfig.create() //
						.setId("ctrHeatPump0") //
						.setMode(Mode.MANUAL) //
						.setManualState(Status.LOCK) //
						.setOutputChannel1("io0/InputOutput0") //
						.setOutputChannel2("io0/InputOutput1") //
						.setMeterId("") //
						.build())
				.next(new TestCase() //
						.output(STATUS, Status.LOCK) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, false)) //
				.deactivate();
	}

	@Test
	void automatic_regular_test() throws Exception {
		this.createDefaultControllerTest(null) //
				.activate(MyConfig.create() //
						.setId("ctrHeatPump0") //
						.setMode(Mode.AUTOMATIC) //
						.setAutomaticForceOnCtrlEnabled(false) //
						.setAutomaticRecommendationCtrlEnabled(false) //
						.setAutomaticLockCtrlEnabled(false) //
						.setOutputChannel1("io0/InputOutput0") //
						.setOutputChannel2("io0/InputOutput1") //
						.setMeterId("") //
						.build())
				.next(new TestCase() //
						.output(STATUS, Status.REGULAR) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, false)) //
				.deactivate();
	}

	@Test
	void automatic_normal_config_test() throws Exception {
		this.createDefaultControllerTest(null) //
				.activate(MyConfig.create() //
						.setId("ctrHeatPump0") //
						.setMode(Mode.AUTOMATIC) //
						.setAutomaticRecommendationCtrlEnabled(true) //
						.setAutomaticRecommendationSurplusPower(3000) //
						.setAutomaticForceOnCtrlEnabled(true) //
						.setAutomaticForceOnSurplusPower(5000) //
						.setAutomaticForceOnSoc(90) //
						.setAutomaticLockCtrlEnabled(true) //
						.setAutomaticLockGridBuyPower(5000) //
						.setAutomaticLockSoc(20) //
						.setOutputChannel1("io0/InputOutput0") //
						.setOutputChannel2("io0/InputOutput1") //
						.setMeterId("") //
						.setMinimumSwitchingTime(0) //
						.build())
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, -4000) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 95) //
						.output(STATUS, Status.RECOMMENDATION)) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, -3000) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 95) //
						.output(STATUS, Status.FORCE_ON)) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 500) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 95) //
						.output(STATUS, Status.RECOMMENDATION)) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, -2700) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 95) //
						.output(STATUS, Status.FORCE_ON)) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, -150) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 88) //
						.output(STATUS, Status.RECOMMENDATION)) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 500) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 88) //
						.output(STATUS, Status.REGULAR))
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 5500) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 19) //
						.output(STATUS, Status.LOCK)) //
				.deactivate();
	}

	@Test
	void automatic_switching_time_test() throws Exception {
		final var clock = createDummyClock();
		this.createDefaultControllerTest(clock) //
				.activate(MyConfig.create() //
						.setId("ctrHeatPump0") //
						.setMode(Mode.AUTOMATIC) //
						.setAutomaticRecommendationCtrlEnabled(true) //
						.setAutomaticRecommendationSurplusPower(3000) //
						.setAutomaticForceOnCtrlEnabled(true) //
						.setAutomaticForceOnSurplusPower(5000) //
						.setAutomaticForceOnSoc(90) //
						.setAutomaticLockCtrlEnabled(true) //
						.setAutomaticLockGridBuyPower(5000) //
						.setAutomaticLockSoc(20) //
						.setOutputChannel1("io0/InputOutput0") //
						.setOutputChannel2("io0/InputOutput1") //
						.setMeterId("") //
						.setMinimumSwitchingTime(60) //
						.build())
				.next(new TestCase("Test 1") //
						.input(GRID_ACTIVE_POWER, -4000) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 95) //
						.output(STATUS, Status.RECOMMENDATION)) //
				.next(new TestCase("Test 1") //
						.timeleap(clock, 18, SECONDS) //
						.output(AWAITING_HYSTERESIS, true)) //
				.next(new TestCase("Test 2") //
						.timeleap(clock, 18, SECONDS) //
						.output(AWAITING_HYSTERESIS, true)) //
				.next(new TestCase("Test 3") //
						.timeleap(clock, 18, SECONDS) //
						.output(AWAITING_HYSTERESIS, true)) //
				.next(new TestCase("Test 4") //
						.timeleap(clock, 18, SECONDS) //
						.output(AWAITING_HYSTERESIS, false))
				.next(new TestCase("Test 5") //
						.input(GRID_ACTIVE_POWER, -4000) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 95) //
						.output(STATUS, Status.FORCE_ON)) //
				.next(new TestCase("Test 6") //
						.timeleap(clock, 30, SECONDS) //
						.output(STATUS, Status.FORCE_ON) //
						.output(AWAITING_HYSTERESIS, true)) //
				.next(new TestCase("Test 7") //
						.timeleap(clock, 10, SECONDS) //
						.input(GRID_ACTIVE_POWER, -500) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 95) //
						.output(STATUS, Status.FORCE_ON) //
						.output(AWAITING_HYSTERESIS, true)) //
				.next(new TestCase("Test 8") //
						.timeleap(clock, 30, SECONDS) //
						.input(GRID_ACTIVE_POWER, 500) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 95) //
						.output(AWAITING_HYSTERESIS, false) //
						.output(STATUS, Status.RECOMMENDATION)) //
				.deactivate();
	}

	@Test
	void automatic_switching2_time_test() throws Exception {
		final var clock = createDummyClock();
		this.createDefaultControllerTest(clock) //
				.activate(MyConfig.create() //
						.setId("ctrHeatPump0") //
						.setMode(Mode.AUTOMATIC) //
						.setAutomaticRecommendationCtrlEnabled(true) //
						.setAutomaticRecommendationSurplusPower(3000) //
						.setAutomaticForceOnCtrlEnabled(true) //
						.setAutomaticForceOnSurplusPower(5000) //
						.setAutomaticForceOnSoc(90) //
						.setAutomaticLockCtrlEnabled(true) //
						.setAutomaticLockGridBuyPower(5000) //
						.setAutomaticLockSoc(20) //
						.setOutputChannel1("io0/InputOutput0") //
						.setOutputChannel2("io0/InputOutput1") //
						.setMeterId("") //
						.setMinimumSwitchingTime(60) //
						.build())
				.next(new TestCase("Test 1") //
						.input(GRID_ACTIVE_POWER, -4000) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 95) //
						.output(STATUS, Status.RECOMMENDATION)) //
				.next(new TestCase("Test 2") //
						.timeleap(clock, 50, SECONDS)//
						.input(GRID_ACTIVE_POWER, -3000) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 95) //
						.output(STATUS, Status.RECOMMENDATION) //
						.output(AWAITING_HYSTERESIS, true)) //
				.next(new TestCase("Test 3") //
						.timeleap(clock, 15, SECONDS)//
						.input(GRID_ACTIVE_POWER, -3000) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 95) //
						.output(AWAITING_HYSTERESIS, false)) //
				.next(new TestCase("Test 3 - Results") //
						.output(AWAITING_HYSTERESIS, true) //
						.output(STATUS, Status.FORCE_ON)) //
				.next(new TestCase("Test 4") //
						.timeleap(clock, 65, SECONDS)//
						.input(GRID_ACTIVE_POWER, -3000) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 95) //
						.output(AWAITING_HYSTERESIS, false) //
						.output(STATUS, Status.FORCE_ON)) //
				.next(new TestCase("Test 5") //
						.timeleap(clock, 15, SECONDS)//
						.input(GRID_ACTIVE_POWER, 500) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 95) //
						.output(AWAITING_HYSTERESIS, false) //
						.output(STATUS, Status.RECOMMENDATION)) //
				.next(new TestCase("Test 6") //
						.timeleap(clock, 65, SECONDS)//
						.input(GRID_ACTIVE_POWER, 15000) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 15) //
						.output(AWAITING_HYSTERESIS, false) //
						.output(STATUS, Status.LOCK)) //
				.next(new TestCase("Test 7") //
						.timeleap(clock, 15, ChronoUnit.MINUTES) //
						.input(GRID_ACTIVE_POWER, -2700) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 95) //
						.output(STATUS, Status.REGULAR)) //
				.next(new TestCase("Test 8") //
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(GRID_ACTIVE_POWER, -15000) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 88) //
						.output(STATUS, Status.RECOMMENDATION)) //
				.deactivate();
	}

	@Test
	void automaticTestWithMeterPowerForceOnEnabled() throws Exception {
		final var clock = createDummyClock();
		this.createDefaultControllerTest(clock) //
				.addReference("meter", new DummyElectricityMeter("meter3")) //
				.activate(MyConfig.create() //
						.setId("ctrHeatPump0") //
						.setMode(Mode.AUTOMATIC) //
						.setAutomaticRecommendationCtrlEnabled(false) //
						.setAutomaticRecommendationSurplusPower(3000) //
						.setAutomaticForceOnCtrlEnabled(true) //
						.setAutomaticForceOnSurplusPower(5000) //
						.setAutomaticForceOnSoc(40) //
						.setAutomaticLockCtrlEnabled(false) //
						.setAutomaticLockGridBuyPower(5000) //
						.setAutomaticLockSoc(20) //
						.setOutputChannel1("io0/InputOutput0") //
						.setOutputChannel2("io0/InputOutput1") //
						.setMeterId("meter3") //
						.setMinimumSwitchingTime(60) //
						.build())
				.next(new TestCase("Regular") //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input(GRID_ACTIVE_POWER, -4000) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 50) //
						.output(STATUS, Status.REGULAR)) //
				.next(new TestCase("Force on") //
						.timeleap(clock, 1, MINUTES)//
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 2000) //
						.input(GRID_ACTIVE_POWER, -4000) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 50) //
						.output(STATUS, Status.FORCE_ON)) //
				.next(new TestCase("Regular") //
						.timeleap(clock, 1, MINUTES)//
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 2000) //
						.input(GRID_ACTIVE_POWER, -4000) //
						.input(ESS_DISCHARGE_POWER, 2000) //
						.input(ESS_SOC, 50) //
						.output(STATUS, Status.REGULAR)) //
				.deactivate();
	}

	@Test
	void automaticTestWithMeterPowerRecommendationEnabled() throws Exception {
		final var clock = createDummyClock();
		this.createDefaultControllerTest(clock) //
				.addReference("meter", new DummyElectricityMeter("meter3")) //
				.activate(MyConfig.create() //
						.setId("ctrHeatPump0") //
						.setMode(Mode.AUTOMATIC) //
						.setAutomaticRecommendationCtrlEnabled(true) //
						.setAutomaticRecommendationSurplusPower(3000) //
						.setAutomaticForceOnCtrlEnabled(true) //
						.setAutomaticForceOnSurplusPower(5000) //
						.setAutomaticForceOnSoc(40) //
						.setAutomaticLockCtrlEnabled(false) //
						.setAutomaticLockGridBuyPower(7000) //
						.setAutomaticLockSoc(20) //
						.setOutputChannel1("io0/InputOutput0") //
						.setOutputChannel2("io0/InputOutput1") //
						.setMeterId("meter3") //
						.setMinimumSwitchingTime(60) //
						.build())
				.next(new TestCase("Regular") //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input(GRID_ACTIVE_POWER, -3000) //
						.input(ESS_DISCHARGE_POWER, 1000) //
						.input(ESS_SOC, 50) //
						.output(STATUS, Status.REGULAR)) //
				.next(new TestCase("Force on") //
						.timeleap(clock, 1, MINUTES)//
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 2000) //
						.input(GRID_ACTIVE_POWER, -6000) //
						.input(ESS_DISCHARGE_POWER, 2000) //
						.input(ESS_SOC, 40) //
						.output(STATUS, Status.FORCE_ON)) //
				.next(new TestCase("Recommendation") //
						.timeleap(clock, 1, MINUTES)//
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 2000) //
						.input(GRID_ACTIVE_POWER, -2000) //
						.input(ESS_DISCHARGE_POWER, -1000) //
						.input(ESS_SOC, 35) //
						.output(STATUS, Status.RECOMMENDATION)) //
				.next(new TestCase("Regular") //
						.timeleap(clock, 1, MINUTES)//
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 40) //
						.output(STATUS, Status.REGULAR)) //
				.deactivate();
	}

	@Test
	void forceOnTestWithJsCalender() throws Exception {
		final var clock = createDummyClock();
		this.createDefaultControllerTest(clock) //
				.activate(MyConfig.create() //
						.setId("ctrHeatPump0") //
						.setManualState(Status.REGULAR) //
						.setMode(Mode.MANUAL) //
						.setAutomaticRecommendationCtrlEnabled(true) //
						.setAutomaticRecommendationSurplusPower(3000) //
						.setAutomaticForceOnCtrlEnabled(true) //
						.setAutomaticForceOnSurplusPower(5000) //
						.setAutomaticForceOnSoc(40) //
						.setAutomaticLockCtrlEnabled(false) //
						.setAutomaticLockGridBuyPower(7000) //
						.setAutomaticLockSoc(20) //
						.setOutputChannel1("io0/InputOutput0") //
						.setOutputChannel2("io0/InputOutput1") //
						.setMeterId("") //
						.setMinimumSwitchingTime(60) //
						.setJsCalender(this.dailyJsCalendarTask(LocalTime.of(1, 0), Duration.of(2, ChronoUnit.HOURS),
								BaseMode.FORCE_ON)) //
						.build())
				.next(new TestCase("Task not active yet. Config value is used") //
						.output(STATUS, Status.REGULAR)) //
				.next(new TestCase("Task is going active") //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.output(STATUS, Status.FORCE_ON)) //
				.next(new TestCase("Task is still active") //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.output(STATUS, Status.FORCE_ON)) //
				.next(new TestCase("Task is not active anymore. Config value is used again") //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.output(STATUS, Status.REGULAR)) //
				.deactivate();
	}

	@Test
	void automaticTestWithJsCalenderAndMeter() throws Exception {
		final var clock = createDummyClock();
		this.createDefaultControllerTest(clock) //
				.addReference("meter", new DummyElectricityMeter("meter3")) //
				.activate(MyConfig.create() //
						.setId("ctrHeatPump0") //
						.setManualState(Status.LOCK) //
						.setMode(Mode.MANUAL) //
						.setAutomaticRecommendationCtrlEnabled(true) //
						.setAutomaticRecommendationSurplusPower(3000) //
						.setAutomaticForceOnCtrlEnabled(true) //
						.setAutomaticForceOnSurplusPower(5000) //
						.setAutomaticForceOnSoc(40) //
						.setAutomaticLockCtrlEnabled(false) //
						.setAutomaticLockGridBuyPower(7000) //
						.setAutomaticLockSoc(20) //
						.setOutputChannel1("io0/InputOutput0") //
						.setOutputChannel2("io0/InputOutput1") //
						.setMeterId("meter3") //
						.setMinimumSwitchingTime(60) //
						.setJsCalender(this.dailyJsCalendarTask(LocalTime.of(8, 0), Duration.of(3, ChronoUnit.HOURS),
								BaseMode.AUTOMATIC)) //
						.build())
				.next(new TestCase("Task not active yet. Config value is used") //
						.output(STATUS, Status.LOCK)) //
				.next(new TestCase("Task is going active at 8 o'clock with grid feed-in") //
						.timeleap(clock, 8, ChronoUnit.HOURS).input(GRID_ACTIVE_POWER, -6000) //
						.input(ESS_SOC, 80).output(STATUS, Status.FORCE_ON)) //
				.next(new TestCase("Task is still active now with the battery discharging") //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 2000) //
						.input(GRID_ACTIVE_POWER, -4000) //
						.input(ESS_DISCHARGE_POWER, 2000).input(ESS_SOC, 80) //
						.output(STATUS, Status.RECOMMENDATION)) //
				.next(new TestCase("Task is still active, but the grid feed-in is not sufficient anymore") //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 1000) //
						.input(GRID_ACTIVE_POWER, -1000) //
						.input(ESS_DISCHARGE_POWER, 2000).input(ESS_SOC, 60) //
						.output(STATUS, Status.REGULAR)) //
				.next(new TestCase("Task is not active anymore, config value is used again") //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.output(STATUS, Status.LOCK)) //
				.deactivate();
	}

	private ControllerTest createDefaultControllerTest(TimeLeapClock clock) throws Exception {
		return new ControllerTest(new ControllerIoHeatPumpSgReadyImpl())//
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", clock == null //
						? new DummyComponentManager() //
						: new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0"));
	}

	private String dailyJsCalendarTask(LocalTime start, Duration duration, BaseMode baseMode) {
		return """
				[
					{
						"@type":"Task",
						"start":"%s",
						"duration":"%s",
						"recurrenceRules":[{"frequency":"daily"}],
						"openems.io:payload":{
							"baseMode":"%s"
						}
					}
				]
				""".formatted(start.toString(), duration.toString(), baseMode);
	}
}
