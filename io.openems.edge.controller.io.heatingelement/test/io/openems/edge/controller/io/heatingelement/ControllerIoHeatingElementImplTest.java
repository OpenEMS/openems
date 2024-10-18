package io.openems.edge.controller.io.heatingelement;

import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.common.test.TestUtils.createDummyClock;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.PHASE1_TIME;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.PHASE2_TIME;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.PHASE3_TIME;
import static io.openems.edge.controller.io.heatingelement.enums.Level.LEVEL_3;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT0;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT1;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT2;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class ControllerIoHeatingElementImplTest {

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerIoHeatingElementImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setOutputChannelPhaseL1("io0/InputOutput0") //
						.setOutputChannelPhaseL2("io0/InputOutput1") //
						.setOutputChannelPhaseL3("io0/InputOutput2") //
						.setEndTime("15:45:00") //
						.setPowerOfPhase(2000) //
						.setMode(Mode.AUTOMATIC) //
						.setDefaultLevel(LEVEL_3) //
						.setWorkMode(WorkMode.TIME) //
						.setMinTime(1) //
						.setMinimumSwitchingTime(60) //
						.build()) //
				.next(new TestCase() //
						// Grid active power : 0, Excess power : 0,
						// from -> UNDEFINED --to--> LEVEL_0, no of relais = 0
						.input(GRID_ACTIVE_POWER, 0) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false) //
						.output(PHASE1_TIME, 0) //
						.output(PHASE1_TIME, 0) //
						.output(PHASE2_TIME, 0)) //
				.next(new TestCase() //
						// Grid active power : 0, Excess power : 0,
						// from -> LEVEL_0 --to--> LEVEL_0, no of relais = 0
						.timeleap(clock, 15, MINUTES)//
						.input(GRID_ACTIVE_POWER, 0) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false) //
						.output(PHASE1_TIME, 0) //
						.output(PHASE2_TIME, 0) //
						.output(PHASE3_TIME, 0)) //
				.next(new TestCase() //
						// Grid active power : -2000, Excess power : 2000,
						// from -> LEVEL_0 --to--> LEVEL_1, no of relais = 1
						.timeleap(clock, 15, MINUTES)//
						.input(GRID_ACTIVE_POWER, -2000) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false) //
						.output(PHASE1_TIME, 0) //
						.output(PHASE2_TIME, 0) //
						.output(PHASE3_TIME, 0)) //
				.next(new TestCase() //
						// Grid active power : -4000, Excess power : 6000,
						// from -> LEVEL_1 --to--> LEVEL_3, no of relais = 3
						.timeleap(clock, 15, MINUTES)//
						.input(GRID_ACTIVE_POWER, -4000) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT2, true) //
						.output(PHASE1_TIME, 15 * 60) //
						.output(PHASE2_TIME, 0) //
						.output(PHASE3_TIME, 0)) //
				.next(new TestCase() //
						// Grid active power : -6000, Excess power : 12000,
						// from -> LEVEL_3 --to--> LEVEL_3, no of relais = 3
						.timeleap(clock, 15, MINUTES)//
						.input(GRID_ACTIVE_POWER, -6000) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT2, true) //
						.output(PHASE1_TIME, 30 * 60) //
						.output(PHASE2_TIME, 15 * 60) //
						.output(PHASE3_TIME, 15 * 60)) //
				.next(new TestCase() //
						// Grid active power : -7000, Excess power : 13000,
						// from -> LEVEL_3 --to--> LEVEL_3, no of relais = 3
						.input(GRID_ACTIVE_POWER, -7000) //
						.timeleap(clock, 15, MINUTES)//
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT2, true) //
						.output(PHASE1_TIME, 45 * 60) //
						.output(PHASE2_TIME, 30 * 60) //
						.output(PHASE3_TIME, 30 * 60)) //
				.next(new TestCase() //
						// Grid active power : 0, Excess power : 6000,
						// from -> LEVEL_3 --to--> LEVEL_3, no of relais = 3
						.timeleap(clock, 15, MINUTES)//
						.input(GRID_ACTIVE_POWER, 0) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT2, true) //
						.output(PHASE1_TIME, 60 * 60) //
						.output(PHASE2_TIME, 45 * 60) //
						.output(PHASE3_TIME, 45 * 60)) //
				.next(new TestCase() //
						// Grid active power : 1, Excess power : 0,
						// from -> LEVEL_3 --to--> LEVEL_0, no of relais = 0
						.timeleap(clock, 15, MINUTES)//
						.input(GRID_ACTIVE_POWER, 1) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false) //
						.output(PHASE1_TIME, 75 * 60) //
						.output(PHASE2_TIME, 60 * 60) //
						.output(PHASE3_TIME, 60 * 60)) //
				.next(new TestCase() //
						// Grid active power : 20000, Excess power : 0,
						// from -> LEVEL_0 --to--> LEVEL_0, no of relais = 0
						.timeleap(clock, 15, MINUTES)//
						.input(GRID_ACTIVE_POWER, 20000) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false) //
						.output(PHASE1_TIME, 75 * 60) //
						.output(PHASE2_TIME, 60 * 60) //
						.output(PHASE3_TIME, 60 * 60)) //
				.next(new TestCase() //
						// Grid active power : -4000, Excess power : 10000,
						// from -> LEVEL_0 --to--> LEVEL_2, no of relais = 2
						.timeleap(clock, 15, MINUTES)//
						.input(GRID_ACTIVE_POWER, -4000) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT2, false) //
						.output(PHASE1_TIME, 75 * 60) //
						.output(PHASE2_TIME, 60 * 60) //
						.output(PHASE3_TIME, 60 * 60)) //
				.next(new TestCase() //
						// Grid active power : 0, Excess power : 4000,
						// from -> LEVEL_2 --to--> LEVEL_2, no of relais = 2
						.timeleap(clock, 15, MINUTES)//
						.input(GRID_ACTIVE_POWER, 0) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT2, false) //
						.output(PHASE1_TIME, 90 * 60) //
						.output(PHASE2_TIME, 75 * 60) //
						.output(PHASE3_TIME, 60 * 60)) //
				.next(new TestCase() //
						// Switch to next day
						.timeleap(clock, 22, HOURS)//
						.input(GRID_ACTIVE_POWER, 0) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT2, false) //
						.output(PHASE1_TIME, 30 * 60) //
						.output(PHASE2_TIME, 30 * 60) //
						.output(PHASE3_TIME, 0)) //
				.deactivate();
	}

}
