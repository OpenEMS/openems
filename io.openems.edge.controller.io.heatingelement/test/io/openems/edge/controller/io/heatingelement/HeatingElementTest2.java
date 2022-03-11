package io.openems.edge.controller.io.heatingelement;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class HeatingElementTest2 {

	private final static String CTRL_ID = "ctrl0";
	private final static String IO_ID = "io0";

	private final static ChannelAddress SUM_GRID_ACTIVE_POWER = new ChannelAddress("_sum", "GridActivePower");

	private final static ChannelAddress IO_OUTPUT1 = new ChannelAddress(IO_ID, "InputOutput1");
	private final static ChannelAddress IO_OUTPUT2 = new ChannelAddress(IO_ID, "InputOutput2");
	private final static ChannelAddress IO_OUTPUT3 = new ChannelAddress(IO_ID, "InputOutput3");

	private final static ChannelAddress CTRL_FORCE_START_AT_SECONDS_OF_DAY = new ChannelAddress(CTRL_ID,
			"ForceStartAtSecondsOfDay");
	private final static ChannelAddress CTRL_TOTAL_PHASE_TIME = new ChannelAddress(CTRL_ID, "TotalPhaseTime");

	@Test
	public void test() throws Exception {
		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);
		new ControllerTest(new ControllerHeatingElementImpl()) //
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
}
