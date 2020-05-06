package io.openems.edge.common.test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Provides a generic test framework for OpenEMS Components.
 * 
 * @see {@link AbstractComponentTest} for implementation details
 */
public class ComponentTest extends AbstractComponentTest<ComponentTest, OpenemsComponent> {

	public ComponentTest(OpenemsComponent sut) {
		super(sut);
	}

	@Override
	protected void executeLogic() throws OpenemsNamedException {
		// nothing to do here
	}

	@Override
	protected ComponentTest self() {
		return this;
	}

}
