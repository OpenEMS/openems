package io.openems.edge.controller.symmetric.peakshaving;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.test.DummySymmetricMeter;

public class PeakshavingTest {

	@Test
	public void test() throws Exception {
		// Initialize Controller
		PeakShaving controller = new PeakShaving();
		// Add referenced services
		DummyComponentManager componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;
		DummyPower power = new DummyPower(0.5, 0.2, 0.1);
		controller.power = power;
		// Activate (twice, so that reference target is set)
		MyConfig config = new MyConfig("ctrl0", "ess0", "meter0", 100000, 50000);
		controller.activate(null, config);
		controller.activate(null, config);
		// Prepare Channels
		ChannelAddress gridMode = new ChannelAddress("ess0", "GridMode");
		ChannelAddress ess = new ChannelAddress("ess0", "ActivePower");
		ChannelAddress grid = new ChannelAddress("meter0", "ActivePower");
		ChannelAddress essSetPower = new ChannelAddress("ess0", "SetActivePowerEquals");
		// Build and run test
		ManagedSymmetricEss essComponent = new DummyManagedSymmetricEss("ess0");
		SymmetricMeter meterComponent = new DummySymmetricMeter("meter0");
//		/*
//		 * Test-Cases simulating constant 120000 kW Grid power with slow measuring at
//		 * Grid + ESS and eventually overshoot of ESS
//		 */
//		new ControllerTest(controller, componentManager, essComponent, meterComponent) //
//				.next(new TestCase() //
//						.input(gridMode, GridMode.ON_GRID) //
//						.input(ess, 0).input(grid, 120000) //
//						.output(essSetPower, 20000)) //
//				.next(new TestCase() //
//						.input(ess, 5000).input(grid, 120000) //
//						.output(essSetPower, 25000)) //
//				.next(new TestCase() //
//						.input(ess, 10000).input(grid, 118000) //
//						.output(essSetPower, 28000)) //
//				.next(new TestCase() //
//						.input(ess, 15000).input(grid, 112000) //
//						.output(essSetPower, 27000)) //
//				.next(new TestCase() //
//						.input(ess, 20000).input(grid, 105000) //
//						.output(essSetPower, 25000)) //
//				.next(new TestCase() //
//						.input(ess, 23000).input(grid, 95000) //
//						.output(essSetPower, 18000)) //
//				.run();
		/*
		 * Test-Cases simulating constant 120000 kW Grid power with slow measuring at
		 * Grid + ESS and eventually overshoot of ESS; filtered with PID.
		 */
		new ControllerTest(controller, componentManager, essComponent, meterComponent) //
				.next(new TestCase() //
						.input(gridMode, GridMode.ON_GRID) //
						.input(ess, 0).input(grid, 120000) //
						.output(essSetPower, 10000 /* instead of 20000 */)) //
				.next(new TestCase() //
						.input(ess, 5000).input(grid, 120000) //
						.output(essSetPower, 13500 /* instead of 25000 */)) //
				.next(new TestCase() //
						.input(ess, 10000).input(grid, 118000) //
						.output(essSetPower, 16500 /* instead of 28000 */)) //
				.next(new TestCase() //
						.input(ess, 15000).input(grid, 112000) //
						.output(essSetPower, 17100 /* instead of 27000 */)) //
				.next(new TestCase() //
						.input(ess, 20000).input(grid, 105000) //
						.output(essSetPower, 16000 /* instead of 25000 */)) //
				.next(new TestCase() //
						.input(ess, 23000).input(grid, 95000) //
						.output(essSetPower, 12200 /* instead of 18000 */)) //
				.run();
	}

}
