package io.openems.edge.controller.asymmetric.peakshaving;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.test.DummyAsymmetricMeter;
import io.openems.edge.meter.test.DummySymmetricMeter;

public class PeakshavingTest {

	private static PeakShaving controller;
	private static DummyComponentManager componentManager;

	@Test
	public void symmetricMeterTest() throws Exception {
		// Initialize Controller
		controller = new PeakShaving();

		// Add referenced services
		componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;
		DummyPower power = new DummyPower(0.3, 0.3, 0.1);
		controller.power = power;

		// Activate (twice, so that reference target is set)
		MyConfig config = new MyConfig("ctrl0", "ess0", "meter0", 33333, 16666);
		controller.activate(null, config);
		controller.activate(null, config);

		// Prepare Channels
		ChannelAddress ess = new ChannelAddress("ess0", "ActivePower");
		ChannelAddress grid = new ChannelAddress("meter0", "ActivePower");
		ChannelAddress essSetPower = new ChannelAddress("ess0", "SetActivePowerEquals");

		// Build and run test
		ManagedSymmetricEss essComponent = new DummyManagedSymmetricEss("ess0", power);
		SymmetricMeter meterComponent = new DummySymmetricMeter("meter0");
		new ControllerTest(controller, componentManager, essComponent, meterComponent) //
				.next(new TestCase() //
						.input(ess, 0).input(grid, 120000) //
						.output(essSetPower, 6000)) //
				.next(new TestCase() //
						.input(ess, 0).input(grid, 120000) //
						.output(essSetPower, 12001)) //
				.next(new TestCase() //
						.input(ess, 3793).input(grid, 120000 - 3793) //
						.output(essSetPower, 16484)) //
				.next(new TestCase() //
						.input(ess, 8981).input(grid, 120000 - 8981) //
						.output(essSetPower, 19650)) //
				.next(new TestCase() //
						.input(ess, 13723).input(grid, 120000 - 13723) //
						.output(essSetPower, 21578)) //
				.next(new TestCase() //
						.input(ess, 17469).input(grid, 120000 - 17469) //
						.output(essSetPower, 22437)) //
				.next(new TestCase() //
						.input(ess, 20066).input(grid, 120000 - 20066) //
						.output(essSetPower, 22533)) //
				.next(new TestCase() //
						.input(ess, 21564).input(grid, 120000 - 21564) //
						.output(essSetPower, 22174)) //
				.next(new TestCase() //
						.input(ess, 22175).input(grid, 120000 - 22175) //
						.output(essSetPower, 21610)) //
				.next(new TestCase() //
						.input(ess, 22173).input(grid, 120000 - 22173) //
						.output(essSetPower, 21020)) //
				.next(new TestCase() //
						.input(ess, 21816).input(grid, 120000 - 21816) //
						.output(essSetPower, 20511)) //
				.next(new TestCase() //
						.input(ess, 21311).input(grid, 120000 - 21311) //
						.output(essSetPower, 20133)) //
				.next(new TestCase() //
						.input(ess, 20803).input(grid, 120000 - 20803) //
						.output(essSetPower, 19893)) //
				.next(new TestCase() //
						.input(ess, 20377).input(grid, 120000 - 20377) //
						.output(essSetPower, 19772)) //
				.run();
	}

	@Test
	public void asymmetricMeterTest() throws Exception {

		// Initialize Controller
		controller = new PeakShaving();

		// Add referenced services
		componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;
		DummyPower power = new DummyPower(0.3, 0.3, 0.1);
		controller.power = power;

		// Activate (twice, so that reference target is set)
		MyConfig config = new MyConfig("ctrl0", "ess0", "meter0", 33333, 16666);
		controller.activate(null, config);
		controller.activate(null, config);

		// Prepare Channels
		ChannelAddress ess = new ChannelAddress("ess0", "ActivePower");
		ChannelAddress gridL1 = new ChannelAddress("meter0", "ActivePowerL1");
		ChannelAddress gridL2 = new ChannelAddress("meter0", "ActivePowerL2");
		ChannelAddress gridL3 = new ChannelAddress("meter0", "ActivePowerL3");
		ChannelAddress essSetPower = new ChannelAddress("ess0", "SetActivePowerEquals");

		// Build and run test
		ManagedSymmetricEss essComponent = new DummyManagedSymmetricEss("ess0", power);
		AsymmetricMeter meterComponent = new DummyAsymmetricMeter("meter0");
		new ControllerTest(controller, componentManager, essComponent, meterComponent) //
				.next(new TestCase() //
						.input(ess, 0) //
						.input(gridL1, 20000).input(gridL2, 40000).input(gridL3, 10000) //
						.output(essSetPower, 6000)) //
				.next(new TestCase() //
						.input(ess, 0) //
						.input(gridL1, 20000).input(gridL2, 40000).input(gridL3, 10000) //
						.output(essSetPower, 12001)) //
				.next(new TestCase() //
						.input(ess, 3793) //
						.input(gridL1, 20000 - 3793 / 3).input(gridL2, 40000 - 3793 / 3).input(gridL3, 10000 - 3793 / 3) //
						.output(essSetPower, 16484)) //
				.next(new TestCase() //
						.input(ess, 8981) //
						.input(gridL1, 20000 - 8981 / 3).input(gridL2, 40000 - 8981 / 3).input(gridL3, 10000 - 8981 / 3) //
						.output(essSetPower, 19651)) //
				.run();
	}
}
