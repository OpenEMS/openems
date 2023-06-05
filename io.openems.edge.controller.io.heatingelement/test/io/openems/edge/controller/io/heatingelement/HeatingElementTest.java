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
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class HeatingElementTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String IO_ID = "io0";

	private static final ChannelAddress SUM_GRID_ACTIVE_POWER = new ChannelAddress("_sum", "GridActivePower");

	private static final ChannelAddress IO_OUTPUT1 = new ChannelAddress(IO_ID, "InputOutput1");
	private static final ChannelAddress IO_OUTPUT2 = new ChannelAddress(IO_ID, "InputOutput2");
	private static final ChannelAddress IO_OUTPUT3 = new ChannelAddress(IO_ID, "InputOutput3");

	private static final ChannelAddress CTRL_PHASE1TIME = new ChannelAddress(CTRL_ID, "Phase1Time");
	private static final ChannelAddress CTRL_PHASE2TIME = new ChannelAddress(CTRL_ID, "Phase2Time");
	private static final ChannelAddress CTRL_PHASE3TIME = new ChannelAddress(CTRL_ID, "Phase3Time");

	@Test
	public void test() throws Exception {
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
						// Grid active power : 0, Excess power : 0,
						// from -> UNDEFINED --to--> LEVEL_0, no of relais = 0
						.input(SUM_GRID_ACTIVE_POWER, 0) //
						.output(IO_OUTPUT1, false) //
						.output(IO_OUTPUT2, false) //
						.output(IO_OUTPUT3, false) //
						.output(CTRL_PHASE1TIME, 0) //
						.output(CTRL_PHASE1TIME, 0) //
						.output(CTRL_PHASE2TIME, 0)) //
				.next(new TestCase() //
						// Grid active power : 0, Excess power : 0,
						// from -> LEVEL_0 --to--> LEVEL_0, no of relais = 0
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(SUM_GRID_ACTIVE_POWER, 0) //
						.output(IO_OUTPUT1, false) //
						.output(IO_OUTPUT2, false) //
						.output(IO_OUTPUT3, false) //
						.output(CTRL_PHASE1TIME, 0) //
						.output(CTRL_PHASE2TIME, 0) //
						.output(CTRL_PHASE3TIME, 0)) //
				.next(new TestCase() //
						// Grid active power : -2000, Excess power : 2000,
						// from -> LEVEL_0 --to--> LEVEL_1, no of relais = 1
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(SUM_GRID_ACTIVE_POWER, -2000) //
						.output(IO_OUTPUT1, true) //
						.output(IO_OUTPUT2, false) //
						.output(IO_OUTPUT3, false) //
						.output(CTRL_PHASE1TIME, 0) //
						.output(CTRL_PHASE2TIME, 0) //
						.output(CTRL_PHASE3TIME, 0)) //
				.next(new TestCase() //
						// Grid active power : -4000, Excess power : 6000,
						// from -> LEVEL_1 --to--> LEVEL_3, no of relais = 3
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(SUM_GRID_ACTIVE_POWER, -4000) //
						.output(IO_OUTPUT1, true) //
						.output(IO_OUTPUT2, true) //
						.output(IO_OUTPUT3, true) //
						.output(CTRL_PHASE1TIME, 15 * 60) //
						.output(CTRL_PHASE2TIME, 0) //
						.output(CTRL_PHASE3TIME, 0)) //
				.next(new TestCase() //
						// Grid active power : -6000, Excess power : 12000,
						// from -> LEVEL_3 --to--> LEVEL_3, no of relais = 3
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(SUM_GRID_ACTIVE_POWER, -6000) //
						.output(IO_OUTPUT1, true) //
						.output(IO_OUTPUT2, true) //
						.output(IO_OUTPUT3, true) //
						.output(CTRL_PHASE1TIME, 30 * 60) //
						.output(CTRL_PHASE2TIME, 15 * 60) //
						.output(CTRL_PHASE3TIME, 15 * 60)) //
				.next(new TestCase() //
						// Grid active power : -7000, Excess power : 13000,
						// from -> LEVEL_3 --to--> LEVEL_3, no of relais = 3
						.input(SUM_GRID_ACTIVE_POWER, -7000) //
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.output(IO_OUTPUT1, true) //
						.output(IO_OUTPUT2, true) //
						.output(IO_OUTPUT3, true) //
						.output(CTRL_PHASE1TIME, 45 * 60) //
						.output(CTRL_PHASE2TIME, 30 * 60) //
						.output(CTRL_PHASE3TIME, 30 * 60)) //
				.next(new TestCase() //
						// Grid active power : 0, Excess power : 6000,
						// from -> LEVEL_3 --to--> LEVEL_3, no of relais = 3
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(SUM_GRID_ACTIVE_POWER, 0) //
						.output(IO_OUTPUT1, true) //
						.output(IO_OUTPUT2, true) //
						.output(IO_OUTPUT3, true) //
						.output(CTRL_PHASE1TIME, 60 * 60) //
						.output(CTRL_PHASE2TIME, 45 * 60) //
						.output(CTRL_PHASE3TIME, 45 * 60)) //
				.next(new TestCase() //
						// Grid active power : 1, Excess power : 0,
						// from -> LEVEL_3 --to--> LEVEL_0, no of relais = 0
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(SUM_GRID_ACTIVE_POWER, 1) //
						.output(IO_OUTPUT1, false) //
						.output(IO_OUTPUT2, false) //
						.output(IO_OUTPUT3, false) //
						.output(CTRL_PHASE1TIME, 75 * 60) //
						.output(CTRL_PHASE2TIME, 60 * 60) //
						.output(CTRL_PHASE3TIME, 60 * 60)) //
				.next(new TestCase() //
						// Grid active power : 20000, Excess power : 0,
						// from -> LEVEL_0 --to--> LEVEL_0, no of relais = 0
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(SUM_GRID_ACTIVE_POWER, 20000) //
						.output(IO_OUTPUT1, false) //
						.output(IO_OUTPUT2, false) //
						.output(IO_OUTPUT3, false) //
						.output(CTRL_PHASE1TIME, 75 * 60) //
						.output(CTRL_PHASE2TIME, 60 * 60) //
						.output(CTRL_PHASE3TIME, 60 * 60)) //
				.next(new TestCase() //
						// Grid active power : -4000, Excess power : 10000,
						// from -> LEVEL_0 --to--> LEVEL_2, no of relais = 2
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(SUM_GRID_ACTIVE_POWER, -4000) //
						.output(IO_OUTPUT1, true) //
						.output(IO_OUTPUT2, true) //
						.output(IO_OUTPUT3, false) //
						.output(CTRL_PHASE1TIME, 75 * 60) //
						.output(CTRL_PHASE2TIME, 60 * 60) //
						.output(CTRL_PHASE3TIME, 60 * 60)) //
				.next(new TestCase() //
						// Grid active power : 0, Excess power : 4000,
						// from -> LEVEL_2 --to--> LEVEL_2, no of relais = 2
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(SUM_GRID_ACTIVE_POWER, 0) //
						.output(IO_OUTPUT1, true) //
						.output(IO_OUTPUT2, true) //
						.output(IO_OUTPUT3, false) //
						.output(CTRL_PHASE1TIME, 90 * 60) //
						.output(CTRL_PHASE2TIME, 75 * 60) //
						.output(CTRL_PHASE3TIME, 60 * 60)) //
				.next(new TestCase() //
						// Switch to next day
						.timeleap(clock, 22, ChronoUnit.HOURS)//
						.input(SUM_GRID_ACTIVE_POWER, 0) //
						.output(IO_OUTPUT1, true) //
						.output(IO_OUTPUT2, true) //
						.output(IO_OUTPUT3, false) //
						.output(CTRL_PHASE1TIME, 30 * 60) //
						.output(CTRL_PHASE2TIME, 30 * 60) //
						.output(CTRL_PHASE3TIME, 0)); //
	}

}
