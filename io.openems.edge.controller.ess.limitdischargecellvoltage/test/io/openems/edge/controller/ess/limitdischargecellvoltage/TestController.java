package io.openems.edge.controller.ess.limitdischargecellvoltage;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.ess.limitdischargecellvoltage.helper.CreateTestConfig;
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
		LimitDischargeCellVoltageController controller = new LimitDischargeCellVoltageController();
		// Add referenced services
		DummyComponentManager componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;
		new CreateTestConfig();
		// Activate (twice, so that reference target is set)
		Config config = CreateTestConfig.create();
		try {
			controller.activate(null, config);
			controller.activate(null, config);
			// Prepare Channels
			ChannelAddress ctrl0MinCellVoltage = new ChannelAddress(CreateTestConfig.ESS_ID, "MinCellVoltage");
			ChannelAddress ctrl0State = new ChannelAddress(CreateTestConfig.ID, "StateMachine");
			// Build and run test
			ManagedSymmetricEss ess = new DummyManagedSymmetricEss(CreateTestConfig.ESS_ID);
			new ControllerTest(controller, componentManager, ess, controller) //
					.next(new TestCase() //
							.input(ctrl0MinCellVoltage, config.warningCellVoltage() - 1) //
							.output(ctrl0State, State.WARNING.getValue())) //
					.next(new TestCase() //
							.input(ctrl0MinCellVoltage, config.criticalCellVoltage() - 1) //
							.output(ctrl0State, State.CRITICAL.getValue())) //
					.run();
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
}
