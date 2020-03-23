package io.openems.edge.battery.soltaro.controller;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.battery.soltaro.controller.helper.Creator;
import io.openems.edge.battery.soltaro.controller.helper.DummyComponentManager;
import io.openems.edge.battery.soltaro.controller.state.StateController;
import io.openems.edge.common.component.ComponentManager;

public class TestStateController {

	@Before
	public void setUp() throws Exception {
		Config c = Creator.createConfig();
		ComponentManager componentManager = new DummyComponentManager();
		StateController.init(componentManager, c);
	}

	@Test
	public final void test() {

		// after calling init for each state there should be a state object

		assertNotNull(StateController.getStateObject(State.CHECK));
		assertNotNull(StateController.getStateObject(State.FORCE_CHARGE));
		assertNotNull(StateController.getStateObject(State.FULL_CHARGE));
		assertNotNull(StateController.getStateObject(State.LIMIT));
		assertNotNull(StateController.getStateObject(State.NORMAL));
		assertNotNull(StateController.getStateObject(State.UNDEFINED));

	}

}
