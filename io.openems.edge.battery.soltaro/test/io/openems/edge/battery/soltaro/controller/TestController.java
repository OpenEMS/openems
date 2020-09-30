package io.openems.edge.battery.soltaro.controller;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

//import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.soltaro.controller.helper.Creator;
import io.openems.edge.battery.soltaro.controller.helper.DummyComponentManager;
//import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class TestController {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public final void test() {
		// TODO More tests and szenarios needed
		// Initialize Controller
		BatteryHandlingController controller = new BatteryHandlingController();
		// Add referenced services
		DummyComponentManager componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;
		// Activate (twice, so that reference target is set)
		Config config = Creator.createConfig();
		try {
			controller.activate(null, config);
			controller.activate(null, config);
			// Prepare Channels
//			ChannelAddress ctrl0MinCellVoltage = new ChannelAddress(Creator.ESS_ID, "MinCellVoltage");
//			ChannelAddress ctrl0State = new ChannelAddress(Creator.ID, "StateMachine");
			// Build and run test
			ManagedSymmetricEss ess = new DummyManagedSymmetricEss(Creator.ESS_ID);
			new ControllerTest(controller, componentManager, ess, controller) //
//					.next(new TestCase() //
//							.input(ctrl0MinCellVoltage, config.warningLowCellVoltage() - 1) //
//							.output(ctrl0State, State.LIMIT.getValue())) //
//					.next(new TestCase() //
//							.input(ctrl0MinCellVoltage, config.criticalLowCellVoltage() - 1) //
//							.output(ctrl0State, State.FORCE_CHARGE.getValue())) //
			;
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
}
