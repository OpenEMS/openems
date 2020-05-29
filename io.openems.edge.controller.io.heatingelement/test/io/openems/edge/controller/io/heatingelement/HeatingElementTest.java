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

public class HeatingElementTest {

	@Test
	public void test() throws Exception {
		// initialize the controller
		TimeLeapClock clock = new TimeLeapClock(
				Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);
		ControllerHeatingElementImpl controller = new ControllerHeatingElementImpl();
		// Add referenced services
		DummyComponentManager componentManager = new DummyComponentManager(clock);
		controller.componentManager = componentManager;
		DummySum sum = new DummySum();
		controller.sum = sum;

		ChannelAddress grid = new ChannelAddress("_sum", "GridActivePower");
		ChannelAddress output1 = new ChannelAddress("io0", "InputOutput1");
		ChannelAddress output2 = new ChannelAddress("io0", "InputOutput2");
		ChannelAddress output3 = new ChannelAddress("io0", "InputOutput3");
		ChannelAddress phase1Time = new ChannelAddress("ctrl1", "Phase1Time");
		ChannelAddress phase2Time = new ChannelAddress("ctrl1", "Phase2Time");
		ChannelAddress phase3Time = new ChannelAddress("ctrl1", "Phase3Time");

		MyConfig myconfig = new MyConfig("ctrl1", grid.toString(), output1.toString(), output2.toString(),
				output3.toString(), "15:45:00", 2000, Mode.AUTOMATIC, Level.LEVEL_3, WorkMode.TIME, 1, 60);
		controller.activate(null, myconfig);
		controller.activate(null, myconfig);

		DummyInputOutput io = new DummyInputOutput("io0");

		// Build and run test
		new ControllerTest(controller, componentManager, sum, io, controller)//
				.next(new TestCase() //
						// Grid active power : 0, Excess power : 0,
						// from -> UNDEFINED --to--> LEVEL_0, no of relais = 0
						.input(grid, 0) //
						.output(output1, false) //
						.output(output2, false) //
						.output(output3, false) //
						.output(phase1Time, 0) //
						.output(phase2Time, 0) //
						.output(phase3Time, 0)) //
				.next(new TestCase() //
						// Grid active power : 0, Excess power : 0,
						// from -> LEVEL_0 --to--> LEVEL_0, no of relais = 0
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(grid, 0) //
						.output(output1, false) //
						.output(output2, false) //
						.output(output3, false) //
						.output(phase1Time, 0) //
						.output(phase2Time, 0) //
						.output(phase3Time, 0)) //
				.next(new TestCase() //
						// Grid active power : -2000, Excess power : 2000,
						// from -> LEVEL_0 --to--> LEVEL_1, no of relais = 1
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(grid, -2000) //
						.output(output1, true) //
						.output(output2, false) //
						.output(output3, false) //
						.output(phase1Time, 0) //
						.output(phase2Time, 0) //
						.output(phase3Time, 0)) //
				.next(new TestCase() //
						// Grid active power : -4000, Excess power : 6000,
						// from -> LEVEL_1 --to--> LEVEL_3, no of relais = 3
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(grid, -4000) //
						.output(output1, true) //
						.output(output2, true) //
						.output(output3, true) //
						.output(phase1Time, 15 * 60) //
						.output(phase2Time, 0) //
						.output(phase3Time, 0)) //
				.next(new TestCase() //
						// Grid active power : -6000, Excess power : 12000,
						// from -> LEVEL_3 --to--> LEVEL_3, no of relais = 3
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(grid, -6000) //
						.output(output1, true) //
						.output(output2, true) //
						.output(output3, true) //
						.output(phase1Time, 30 * 60) //
						.output(phase2Time, 15 * 60) //
						.output(phase3Time, 15 * 60)) //
				.next(new TestCase() //
						// Grid active power : -7000, Excess power : 13000,
						// from -> LEVEL_3 --to--> LEVEL_3, no of relais = 3
						.input(grid, -7000) //
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.output(output1, true) //
						.output(output2, true) //
						.output(output3, true) //
						.output(phase1Time, 45 * 60) //
						.output(phase2Time, 30 * 60) //
						.output(phase3Time, 30 * 60)) //
				.next(new TestCase() //
						// Grid active power : 0, Excess power : 6000,
						// from -> LEVEL_3 --to--> LEVEL_3, no of relais = 3
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(grid, 0) //
						.output(output1, true) //
						.output(output2, true) //
						.output(output3, true) //
						.output(phase1Time, 60 * 60) //
						.output(phase2Time, 45 * 60) //
						.output(phase3Time, 45 * 60)) //
				.next(new TestCase() //
						// Grid active power : 1, Excess power : 0,
						// from -> LEVEL_3 --to--> LEVEL_0, no of relais = 0
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(grid, 1) //
						.output(output1, false) //
						.output(output2, false) //
						.output(output3, false) //
						.output(phase1Time, 75 * 60) //
						.output(phase2Time, 60 * 60) //
						.output(phase3Time, 60 * 60)) //
				.next(new TestCase() //
						// Grid active power : 20000, Excess power : 0,
						// from -> LEVEL_0 --to--> LEVEL_0, no of relais = 0
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(grid, 20000) //
						.output(output1, false) //
						.output(output2, false) //
						.output(output3, false) //
						.output(phase1Time, 75 * 60) //
						.output(phase2Time, 60 * 60) //
						.output(phase3Time, 60 * 60)) //
				.next(new TestCase() //
						// Grid active power : -4000, Excess power : 10000,
						// from -> LEVEL_0 --to--> LEVEL_2, no of relais = 2
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(grid, -4000) //
						.output(output1, true) //
						.output(output2, true) //
						.output(output3, false) //
						.output(phase1Time, 75 * 60) //
						.output(phase2Time, 60 * 60) //
						.output(phase3Time, 60 * 60)) //
				.next(new TestCase() //
						// Grid active power : 0, Excess power : 4000,
						// from -> LEVEL_2 --to--> LEVEL_2, no of relais = 2
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(grid, 0) //
						.output(output1, true) //
						.output(output2, true) //
						.output(output3, false) //
						.output(phase1Time, 90 * 60) //
						.output(phase2Time, 75 * 60) //
						.output(phase3Time, 60 * 60)) //
				.next(new TestCase() //
						// Switch to next day
						.timeleap(clock, 22, ChronoUnit.HOURS)//
						.input(grid, 0) //
						.output(output1, true) //
						.output(output2, true) //
						.output(output3, false) //
						.output(phase1Time, 30 * 60) //
						.output(phase2Time, 30 * 60) //
						.output(phase3Time, 0)) //
				.run();
	}

}
