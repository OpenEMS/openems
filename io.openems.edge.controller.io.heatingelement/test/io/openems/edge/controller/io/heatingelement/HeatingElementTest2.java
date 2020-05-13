package io.openems.edge.controller.io.heatingelement;

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

	@Test
	public void test() throws Exception {
		// initialize the controller
		TimeLeapClock clock = new TimeLeapClock(ZoneOffset.UTC);
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
		ChannelAddress forceStartAtSecondsOfDay = new ChannelAddress("ctrl1", "ForceStartAtSecondsOfDay");
		ChannelAddress totalPhaseTime = new ChannelAddress("ctrl1", "TotalPhaseTime");

		MyConfig myconfig = new MyConfig("ctrl1", grid.toString(), output1.toString(), output2.toString(),
				output3.toString(), "15:45:00", 2000, Mode.AUTOMATIC, Level.LEVEL_3, WorkMode.TIME, 1, 60);
		controller.activate(null, myconfig);
		controller.activate(null, myconfig);

		DummyInputOutput io = new DummyInputOutput("io0");

		// Build and run test
		new ControllerTest(controller, componentManager, controller, sum, io)//
				.next(new TestCase() //
						.input(grid, 0) //
						.output(output1, false))//
				.next(new TestCase() //
						.timeleap(clock, 6, ChronoUnit.MINUTES)//
						.input(grid, -2000) //
						.output(output1, true) //
						.output(totalPhaseTime, 0) //
						.output(forceStartAtSecondsOfDay, 53_100 /* 14:45 */)) //
				.next(new TestCase() //
						.timeleap(clock, 6, ChronoUnit.MINUTES)//
						.input(grid, 0) //
						.output(output1, true) //
						.output(totalPhaseTime, 360 /* 6 minutes, one phase */) //
						.output(forceStartAtSecondsOfDay, 53_220 /* 14:47 - two minutes later */)) //
				.run();
	}
}
