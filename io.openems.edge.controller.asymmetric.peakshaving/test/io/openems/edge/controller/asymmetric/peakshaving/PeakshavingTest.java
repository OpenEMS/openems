package io.openems.edge.controller.asymmetric.peakshaving;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.GridMode;
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
		DummyPower power = new DummyPower(0.5, 0.2, 0.1);
		controller.power = power;

		// Activate (twice, so that reference target is set)
		MyConfig config = new MyConfig("ctrl0", "ess0", "meter0", 33333, 16666);
		controller.activate(null, config);
		controller.activate(null, config);

		// Prepare Channels
		ChannelAddress gridMode = new ChannelAddress("ess0", "GridMode");
		ChannelAddress ess = new ChannelAddress("ess0", "ActivePower");
		ChannelAddress grid = new ChannelAddress("meter0", "ActivePower");
		ChannelAddress essSetPower = new ChannelAddress("ess0", "SetActivePowerEquals");

		/*
		 * Build and run test for symmetric Ess and Meter
		 */
		ManagedSymmetricEss essComponent = new DummyManagedSymmetricEss("ess0");
		SymmetricMeter meterComponent = new DummySymmetricMeter("meter0");

		/*
		 * Test-Cases simulating constant 120000 kW Grid power with slow measuring at
		 * Grid + ESS and eventually overshoot of ESS; filtered with PID.
		 */
		new ControllerTest(controller, componentManager, essComponent, meterComponent) //

				.next(new TestCase() //
						.input(gridMode, GridMode.ON_GRID) //
						.input(ess, 0) //
						.input(grid, 120000) //
						.output(essSetPower, 10000 /* instead of 20000 without PID */ )) //
				.next(new TestCase() //
						.input(ess, 5000) //
						.input(grid, 120000) //
						.output(essSetPower, 13500 /* instead of 25000 */ )) //
				.next(new TestCase() //
						.input(ess, 10000) //
						.input(grid, 118000) //
						.output(essSetPower, 16500 /* instead of 28000 */ )) //
				.next(new TestCase() //
						.input(ess, 15000) //
						.input(grid, 112000) //
						.output(essSetPower, 17101 /* instead of 27000 */ )) //
				.next(new TestCase() //
						.input(ess, 20000) //
						.input(grid, 105000) //
						.output(essSetPower, 16001 /* instead of 25000 */ )) //
				.next(new TestCase() //
						.input(ess, 23000) //
						.input(grid, 95000) //
						.output(essSetPower, 12201 /* instead of 18000 */ )) //
				.next(new TestCase() //
						.input(gridMode, GridMode.ON_GRID) //
						.input(ess, 0) //
						.input(grid, 40000) //
						.output(essSetPower, 11302 /* instead of -10000 */ )) //
				.next(new TestCase() //
						.input(gridMode, GridMode.ON_GRID) //
						.input(ess, 0) //
						.input(grid, 5000) //
						.output(essSetPower, -10497 /* instead of -45000 */ )) //
				.run();
	}

	@Test
	public void asymmetricMeterTest() throws Exception {

		// Initialize Controller
		controller = new PeakShaving();

		// Add referenced services
		componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;
		DummyPower power = new DummyPower(0.5, 0.2, 0.1);
		controller.power = power;

		// Activate (twice, so that reference target is set)
		MyConfig config = new MyConfig("ctrl0", "ess0", "meter0", 7360, 5000);
		controller.activate(null, config);
		controller.activate(null, config);

		// Prepare Channels
		ChannelAddress gridMode = new ChannelAddress("ess0", "GridMode");
		ChannelAddress ess = new ChannelAddress("ess0", "ActivePower");
		ChannelAddress gridL1 = new ChannelAddress("meter0", "ActivePowerL1");
		ChannelAddress gridL2 = new ChannelAddress("meter0", "ActivePowerL2");
		ChannelAddress gridL3 = new ChannelAddress("meter0", "ActivePowerL3");
		ChannelAddress essSetPower = new ChannelAddress("ess0", "SetActivePowerEquals");

		/*
		 * Build and run test for asymmetric Meter and symmetric Ess
		 */
		ManagedSymmetricEss essComponent = new DummyManagedSymmetricEss("ess0");
		AsymmetricMeter asymmetricMeterComponent = new DummyAsymmetricMeter("meter0");

		/*
		 * Test-Cases simulating constant more than 22080 kW Grid With more power on one
		 * Phase than allowed.
		 */
		new ControllerTest(controller, componentManager, essComponent, asymmetricMeterComponent) //
				.next(new TestCase() //
						.input(gridMode, GridMode.ON_GRID) //
						.input(ess, 0) //
						.input(gridL1, 10000) //
						.input(gridL2, 7000) //
						.input(gridL3, 7000) //
						.output(essSetPower, 3960 /* instead of 7920 */ )) //
				.next(new TestCase() //
						.input(gridMode, GridMode.ON_GRID) //
						.input(ess, -2000) //
						.input(gridL1, 10000) //
						.input(gridL2, 7000) //
						.input(gridL3, 7000) //
						.output(essSetPower, 5744 /* instead of 5920 */ )) //
				.next(new TestCase() //
						.input(gridMode, GridMode.ON_GRID) //
						.input(ess, 2000) //
						.input(gridL1, 10000) //
						.input(gridL2, 7000) //
						.input(gridL3, 7000) //
						.output(essSetPower, 6728 /* instead of 9920 */ )) //
				.next(new TestCase() //
						.input(gridMode, GridMode.ON_GRID) //
						.input(ess, 0) //
						.input(gridL1, 15000 /* 7.640 more than allowed on this Phase. */) //
						.input(gridL2, 3000) //
						.input(gridL3, 3000) //
						.output(essSetPower, 16412 /* instead of 22920 */ )) //
				.next(new TestCase() //
						.input(gridMode, GridMode.ON_GRID) //
						.input(ess, 0) //
						.input(gridL1, 3000) //
						.input(gridL2, 3000) //
						.input(gridL3, 3000) //
						.output(essSetPower, 6336 /* instead of -6000 */ )) //
				.next(new TestCase() //
						.input(gridMode, GridMode.ON_GRID) //
						.input(ess, 0) //
						.input(gridL1, 1000) //
						.input(gridL2, 1000) //
						.input(gridL3, 1000) //
						.output(essSetPower, 2136 /* instead of -12000 */ )) //
				.next(new TestCase() //
						.input(gridMode, GridMode.ON_GRID) //
						.input(ess, 0) //
						.input(gridL1, 1000) //
						.input(gridL2, 1000) //
						.input(gridL3, 1000) //
						.output(essSetPower, -264 /* instead of -12000 */ )) //
				.run();
	}
}
