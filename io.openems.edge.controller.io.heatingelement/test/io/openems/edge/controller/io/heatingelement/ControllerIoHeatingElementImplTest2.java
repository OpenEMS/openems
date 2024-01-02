package io.openems.edge.controller.io.heatingelement;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
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

	private static final String CTRL_ID = "ctrl0";
	private static final String IO_ID = "io0";

	private static final ChannelAddress SUM_GRID_ACTIVE_POWER = new ChannelAddress("_sum", "GridActivePower");

	private static final ChannelAddress IO_OUTPUT1 = new ChannelAddress(IO_ID, "InputOutput1");
	private static final ChannelAddress IO_OUTPUT2 = new ChannelAddress(IO_ID, "InputOutput2");
	private static final ChannelAddress IO_OUTPUT3 = new ChannelAddress(IO_ID, "InputOutput3");

	private static final ChannelAddress CTRL_FORCE_START_AT_SECONDS_OF_DAY = new ChannelAddress(CTRL_ID,
			"ForceStartAtSecondsOfDay");
	private static final ChannelAddress CTRL_TOTAL_PHASE_TIME = new ChannelAddress(CTRL_ID, "TotalPhaseTime");
	private static final ChannelAddress CTRL_STATUS = new ChannelAddress(CTRL_ID, "Status");

	@Test
	public void minimumTime_lowerLevel_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);
		new ControllerTest(new ControllerIoHeatingElementImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setOutputChannelPhaseL1(IO_OUTPUT1.toString()) //
						.setOutputChannelPhaseL2(IO_OUTPUT2.toString()) //
						.setOutputChannelPhaseL3(IO_OUTPUT3.toString()) //
						.setEndTime("15:45:00") //
						.setPowerOfPhase(2000) //
						.setMode(Mode.AUTOMATIC) //
						.setDefaultLevel(Level.LEVEL_3) //
						.setWorkMode(WorkMode.TIME) //
						.setMinTime(1) //
						.setMinimumSwitchingTime(60) //
						.build()) //
				.next(new TestCase() //
						.input(SUM_GRID_ACTIVE_POWER, 0) //
						.output(IO_OUTPUT1, false))//
				.next(new TestCase() //
						.timeleap(clock, 6, ChronoUnit.MINUTES)//
						.input(SUM_GRID_ACTIVE_POWER, -2000) //
						.output(IO_OUTPUT1, true) //
						.output(CTRL_TOTAL_PHASE_TIME, 0) //
						.output(CTRL_FORCE_START_AT_SECONDS_OF_DAY, 53_100 /* 14:45 */)) //
				.next(new TestCase() //
						.timeleap(clock, 6, ChronoUnit.MINUTES)//
						.input(SUM_GRID_ACTIVE_POWER, 0) //
						.output(IO_OUTPUT1, true) //
						.output(CTRL_TOTAL_PHASE_TIME, 360 /* 6 minutes, one phase */) //
						.output(CTRL_FORCE_START_AT_SECONDS_OF_DAY, 53_220 /* 14:47 - two minutes later */)); //
	}

	@Test
	public void minimumTime_sameLevel_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);
		new ControllerTest(new ControllerIoHeatingElementImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setOutputChannelPhaseL1(IO_OUTPUT1.toString()) //
						.setOutputChannelPhaseL2(IO_OUTPUT2.toString()) //
						.setOutputChannelPhaseL3(IO_OUTPUT3.toString()) //
						.setEndTime("15:45:00") //
						.setPowerOfPhase(2000) //
						.setMode(Mode.AUTOMATIC) //
						.setDefaultLevel(Level.LEVEL_3) //
						.setWorkMode(WorkMode.TIME) //
						.setMinTime(1) //
						.setMinimumSwitchingTime(60) //
						.build()) //
				.next(new TestCase() //
						.input(SUM_GRID_ACTIVE_POWER, 0) //
						.output(IO_OUTPUT1, false))//
				.next(new TestCase() //
						.timeleap(clock, 6, ChronoUnit.MINUTES)//
						.input(SUM_GRID_ACTIVE_POWER, -6000) //
						.output(IO_OUTPUT1, true) //
						.output(IO_OUTPUT2, true) //
						.output(IO_OUTPUT3, true) //
						.output(CTRL_TOTAL_PHASE_TIME, 0) //
						.output(CTRL_FORCE_START_AT_SECONDS_OF_DAY, 53_100 /* 14:45 */)) //
				.next(new TestCase() //
						.timeleap(clock, 6, ChronoUnit.MINUTES)//
						.input(SUM_GRID_ACTIVE_POWER, 0) //
						.output(IO_OUTPUT1, true) //
						.output(IO_OUTPUT2, true) //
						.output(IO_OUTPUT3, true) //
						.output(CTRL_TOTAL_PHASE_TIME, 1080 /* 6 minutes, all three phases */) //
						.output(CTRL_FORCE_START_AT_SECONDS_OF_DAY, 53_460 /* 14:51 - six minutes later */)); //
	}

	@Test
	public void minimumTime_inForceMode_test() throws Exception {
		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577890260) /* starts at 1. January 2020 14:51:00 */,
				ZoneOffset.UTC);
		new ControllerTest(new ControllerIoHeatingElementImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setOutputChannelPhaseL1(IO_OUTPUT1.toString()) //
						.setOutputChannelPhaseL2(IO_OUTPUT2.toString()) //
						.setOutputChannelPhaseL3(IO_OUTPUT3.toString()) //
						.setEndTime("16:00:00") //
						.setPowerOfPhase(2000) //
						.setMode(Mode.AUTOMATIC) //
						.setDefaultLevel(Level.LEVEL_3) //
						.setWorkMode(WorkMode.TIME) //
						.setMinTime(1) //
						.setMinimumSwitchingTime(60) //
						.build()) //
				.next(new TestCase() //
						.input(SUM_GRID_ACTIVE_POWER, 0) //
						.output(IO_OUTPUT1, false))//
				.next(new TestCase() //
						.timeleap(clock, 6, ChronoUnit.MINUTES)// /* 14:57 */
						.input(SUM_GRID_ACTIVE_POWER, 0) //
						.output(IO_OUTPUT1, false) //
						.output(IO_OUTPUT2, false) //
						.output(IO_OUTPUT3, false) //
						.output(CTRL_TOTAL_PHASE_TIME, 0) //
						.output(CTRL_FORCE_START_AT_SECONDS_OF_DAY, 54_000 /* 15:00 */)) //
				.next(new TestCase() //
						.timeleap(clock, 4, ChronoUnit.MINUTES)//
						.input(SUM_GRID_ACTIVE_POWER, 0) //
						.output(IO_OUTPUT1, true) //
						.output(IO_OUTPUT2, true) //
						.output(IO_OUTPUT3, true) //
						.output(CTRL_FORCE_START_AT_SECONDS_OF_DAY, 54_000 /* current time */))
				.next(new TestCase() //
						.timeleap(clock, 3, ChronoUnit.MINUTES)// /* 15:03 */
						.input(SUM_GRID_ACTIVE_POWER, 0) //
						// Previous duration of each phase cannot be set as input if you want to count
						// already passed active time
						// .input(CTRL_PHASE1TIME, 180) //
						// .input(CTRL_PHASE2TIME, 180) //
						// .input(CTRL_PHASE3TIME, 180) //
						.output(IO_OUTPUT1, true) //
						.output(IO_OUTPUT2, true) //
						.output(IO_OUTPUT3, true) //
						.output(CTRL_STATUS, Status.ACTIVE_FORCED) //
						.output(CTRL_FORCE_START_AT_SECONDS_OF_DAY, 54_180 /* current time */)); //
	}
}
