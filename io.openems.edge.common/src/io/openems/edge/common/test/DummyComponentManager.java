package io.openems.edge.common.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.openems.common.types.EdgeConfig;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Simulates a ComponentManager for the OpenEMS Component test framework.
 */
public class DummyComponentManager implements ComponentManager {

	private final List<OpenemsComponent> components = new ArrayList<>();

	public DummyComponentManager() {
	}

	@Override
	public List<OpenemsComponent> getComponents() {
		return Collections.unmodifiableList(this.components);
	}

	/**
	 * Specific for this Dummy implementation.
	 * 
	 * @param component
	 */
	public void addComponent(OpenemsComponent component) {
		if (component != this) {
			this.components.add(component);
		}
	}

	@Override
	public EdgeConfig getEdgeConfig() {
		return new EdgeConfig();
	}

}