package io.openems.edge.controller.test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.controller.api.Controller;

/**
 * Provides a generic test framework for OpenEMS {@link Controller}s.
 */
public class ControllerTest extends AbstractComponentTest<ControllerTest, Controller> {

	public ControllerTest(Controller controller, OpenemsComponent... components) throws OpenemsException {
		super(controller);
		for (OpenemsComponent component : components) {
			this.addComponent(component);
		}
	}

	@Override
	protected void onExecuteControllers() throws OpenemsNamedException {
		this.getSut().run();
	}

	@Override
	protected ControllerTest self() {
		return this;
	}

}