package io.openems.edge.common.test;

import java.util.ArrayList;
import java.util.List;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Simulates a ComponentManager for the OpenEMS Component test framework.
 */
public class DummyComponentManager implements ComponentManager {

	private final List<OpenemsComponent> components = new ArrayList<>();

	public DummyComponentManager() {
	}

	@SuppressWarnings("unchecked")
	public <T extends OpenemsComponent> T getComponent(String componentId) {
		List<OpenemsComponent> components = this.components;
		for (OpenemsComponent component : components) {
			if (component.id().equals(componentId)) {
				return (T) component;
			}
		}
		throw new IllegalArgumentException("Component [" + componentId + "] is not available.");
	}

	@Override
	public <T extends Channel<?>> T getChannel(ChannelAddress channelAddress) throws IllegalArgumentException {
		OpenemsComponent component = this.getComponent(channelAddress.getComponentId());
		return component.channel(channelAddress.getChannelId());
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

}