package io.openems.edge.controller.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.controller.api.Controller;

/**
 * Framework for testing a Controller.
 */
public class ControllerTest extends AbstractComponentTest {

	private final Controller controller;

	public ControllerTest(Controller controller, OpenemsComponent... components) {
		super(components);
		this.controller = controller;
	}

	@Override
	protected void executeLogic() {
		this.controller.run();
	}

}