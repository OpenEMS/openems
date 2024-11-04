package io.openems.edge.controller.io.heatpump.sgready;

import static io.openems.edge.common.sum.Sum.ChannelId.ESS_DISCHARGE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.ESS_SOC;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.common.test.TestUtils.createDummyClock;
import static io.openems.edge.controller.io.heatpump.sgready.ControllerIoHeatPumpSgReady.ChannelId.AWAITING_HYSTERESIS;
import static io.openems.edge.controller.io.heatpump.sgready.ControllerIoHeatPumpSgReady.ChannelId.STATUS;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT0;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT1;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class ControllerIoHeatPumpSgReadyImplTest {

	@Test
	public void manual_undefined_test() throws Exception {
		new ControllerTest(new ControllerIoHeatPumpSgReadyImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrHeatPump0") //
						.setMode(Mode.MANUAL) //
						.setManualState(Status.UNDEFINED) //
						.setOutputChannel1("io0/InputOutput0") //
						.setOutputChannel2("io0/InputOutput1") //
						.build())
				.next(new TestCase() //
						.output(STATUS, Status.REGULAR) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, false)) //
				.deactivate();
	}

	@Test
	public void manual_regular_test() throws Exception {
		new ControllerTest(new ControllerIoHeatPumpSgReadyImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrHeatPump0") //
						.setMode(Mode.MANUAL) //
						.setManualState(Status.REGULAR) //
						.setOutputChannel1("io0/InputOutput0") //
						.setOutputChannel2("io0/InputOutput1") //
						.build())
				.next(new TestCase() //
						.output(STATUS, Status.REGULAR) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, false)) //
				.deactivate();
	}

	@Test
	public void manual_recommendation_test() throws Exception {
		new ControllerTest(new ControllerIoHeatPumpSgReadyImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrHeatPump0") //
						.setMode(Mode.MANUAL) //
						.setManualState(Status.RECOMMENDATION) //
						.setOutputChannel1("io0/InputOutput0") //
						.setOutputChannel2("io0/InputOutput1") //
						.build())
				.next(new TestCase() //
						.output(STATUS, Status.RECOMMENDATION) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, true)) //
				.deactivate();
	}

	@Test
	public void manual_force_on_test() throws Exception {
		new ControllerTest(new ControllerIoHeatPumpSgReadyImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrHeatPump0") //
						.setMode(Mode.MANUAL) //
						.setManualState(Status.FORCE_ON) //
						.setOutputChannel1("io0/InputOutput0") //
						.setOutputChannel2("io0/InputOutput1") //
						.build())
				.next(new TestCase() //
						.output(STATUS, Status.FORCE_ON) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true)) //
				.deactivate();
	}

	@Test
	public void manual_lock_test() throws Exception {
		new ControllerTest(new ControllerIoHeatPumpSgReadyImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrHeatPump0") //
						.setMode(Mode.MANUAL) //
						.setManualState(Status.LOCK) //
						.setOutputChannel1("io0/InputOutput0") //
						.setOutputChannel2("io0/InputOutput1") //
						.build())
				.next(new TestCase() //
						.output(STATUS, Status.LOCK) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, false)) //
				.deactivate();
	}

	@Test
	public void automatic_regular_test() throws Exception {
		new ControllerTest(new ControllerIoHeatPumpSgReadyImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrHeatPump0") //
						.setMode(Mode.AUTOMATIC) //
						.setAutomaticForceOnCtrlEnabled(false) //
						.setAutomaticRecommendationCtrlEnabled(false) //
						.setAutomaticLockCtrlEnabled(false) //
						.setOutputChannel1("io0/InputOutput0") //
						.setOutputChannel2("io0/InputOutput1") //
						.build())
				.next(new TestCase() //
						.output(STATUS, Status.REGULAR) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, false)) //
				.deactivate();
	}

	@Test
	public void automatic_normal_config_test() throws Exception {
		new ControllerTest(new ControllerIoHeatPumpSgReadyImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
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
	public void automatic_switching_time_test() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerIoHeatPumpSgReadyImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
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
	public void automatic_switching2_time_test() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerIoHeatPumpSgReadyImpl())//
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
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
}
