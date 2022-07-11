package io.openems.edge.common.test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Provides a generic test framework for OpenEMS Components.
 * 
 * <p>
 * See {@link AbstractComponentTest} for implementation details
 */
public class ComponentTest extends AbstractComponentTest<ComponentTest, OpenemsComponent> {

	public ComponentTest(OpenemsComponent sut) throws OpenemsException {
		super(sut);
	}

	@Override
	protected ComponentTest self() {
		return this;
	}

}
