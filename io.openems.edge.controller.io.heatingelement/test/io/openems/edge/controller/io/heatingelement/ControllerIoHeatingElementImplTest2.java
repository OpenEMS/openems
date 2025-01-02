package io.openems.edge.controller.io.heatingelement;

import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.FORCE_START_AT_SECONDS_OF_DAY;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.STATUS;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.TOTAL_PHASE_TIME;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT0;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT1;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT2;
import static java.time.temporal.ChronoUnit.MINUTES;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.Status;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class ControllerIoHeatingElementImplTest2 {

	@Test
	public void minimumTime_lowerLevel_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);
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
						.setDefaultLevel(Level.LEVEL_3) //
						.setWorkMode(WorkMode.TIME) //
						.setMinTime(1) //
						.setMinimumSwitchingTime(60) //
						.build()) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.output("io0", INPUT_OUTPUT0, false))//
				.next(new TestCase() //
						.timeleap(clock, 6, MINUTES)//
						.input(GRID_ACTIVE_POWER, -2000) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output(TOTAL_PHASE_TIME, 0) //
						.output(FORCE_START_AT_SECONDS_OF_DAY, 53_100 /* 14:45 */)) //
				.next(new TestCase() //
						.timeleap(clock, 6, MINUTES)//
						.input(GRID_ACTIVE_POWER, 0) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output(TOTAL_PHASE_TIME, 360 /* 6 minutes, one phase */) //
						.output(FORCE_START_AT_SECONDS_OF_DAY, 53_220 /* 14:47 - two minutes later */)) //
				.deactivate();
	}

	@Test
	public void minimumTime_sameLevel_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);
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
						.setDefaultLevel(Level.LEVEL_3) //
						.setWorkMode(WorkMode.TIME) //
						.setMinTime(1) //
						.setMinimumSwitchingTime(60) //
						.build()) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.output("io0", INPUT_OUTPUT0, false))//
				.next(new TestCase() //
						.timeleap(clock, 6, MINUTES)//
						.input(GRID_ACTIVE_POWER, -6000) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT2, true) //
						.output(TOTAL_PHASE_TIME, 0) //
						.output(FORCE_START_AT_SECONDS_OF_DAY, 53_100 /* 14:45 */)) //
				.next(new TestCase() //
						.timeleap(clock, 6, MINUTES)//
						.input(GRID_ACTIVE_POWER, 0) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT2, true) //
						.output(TOTAL_PHASE_TIME, 1080 /* 6 minutes, all three phases */) //
						.output(FORCE_START_AT_SECONDS_OF_DAY, 53_460 /* 14:51 - six minutes later */)) //
				.deactivate();
	}

	@Test
	public void minimumTime_inForceMode_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577890260) /* starts at 1. January 2020 14:51:00 */,
				ZoneOffset.UTC);
		new ControllerTest(new ControllerIoHeatingElementImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setOutputChannelPhaseL1("io0/InputOutput0") //
						.setOutputChannelPhaseL2("io0/InputOutput1") //
						.setOutputChannelPhaseL3("io0/InputOutput2") //
						.setEndTime("16:00:00") //
						.setPowerOfPhase(2000) //
						.setMode(Mode.AUTOMATIC) //
						.setDefaultLevel(Level.LEVEL_3) //
						.setWorkMode(WorkMode.TIME) //
						.setMinTime(1) //
						.setMinimumSwitchingTime(60) //
						.build()) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.output("io0", INPUT_OUTPUT0, false))//
				.next(new TestCase() //
						.timeleap(clock, 6, MINUTES)// /* 14:57 */
						.input(GRID_ACTIVE_POWER, 0) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false) //
						.output(TOTAL_PHASE_TIME, 0) //
						.output(FORCE_START_AT_SECONDS_OF_DAY, 54_000 /* 15:00 */)) //
				.next(new TestCase() //
						.timeleap(clock, 4, MINUTES)//
						.input(GRID_ACTIVE_POWER, 0) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT2, true) //
						.output(FORCE_START_AT_SECONDS_OF_DAY, 54_000 /* current time */))
				.next(new TestCase() //
						.timeleap(clock, 3, MINUTES)// /* 15:03 */
						.input(GRID_ACTIVE_POWER, 0) //
						// Previous duration of each phase cannot be set as input if you want to count
						// already passed active time
						// .input(CTRL_PHASE1TIME, 180) //
						// .input(CTRL_PHASE2TIME, 180) //
						// .input(CTRL_PHASE3TIME, 180) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT2, true) //
						.output(STATUS, Status.ACTIVE_FORCED) //
						.output(FORCE_START_AT_SECONDS_OF_DAY, 54_180 /* current time */)) //
				.deactivate();
	}
}
