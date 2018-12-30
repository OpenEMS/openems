package io.openems.edge.core.componentmanager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;

@Component( //
		name = "Core.ComponentManager", //
		immediate = true)
public class ComponentManagerImpl implements ComponentManager {

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(!(service.factoryPid=Core.ComponentManager)))")
	private volatile List<OpenemsComponent> components = new CopyOnWriteArrayList<>();

	public ComponentManagerImpl() {
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

}
