package io.openems.edge.common.component;

import java.util.List;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;
import io.openems.edge.common.channel.Channel;

/**
 * A Service that provides access to OpenEMS-Components.
 */
public interface ComponentManager {

	/**
	 * Gets all enabled OpenEMS-Components.
	 * 
	 * @return a List of OpenEMS-Components
	 * @throws IllegalArgumentException if the Component was not found
	 */
	public List<OpenemsComponent> getComponents();

	/**
	 * Gets a OpenEMS-Component by its Component-ID.
	 * 
	 * @param componentId the Component-ID (e.g. "_sum")
	 * @return the OpenEMS-Component
	 * @throws IllegalArgumentException if the Component was not found
	 */
	@SuppressWarnings("unchecked")
	public default <T extends OpenemsComponent> T getComponent(String componentId) {
		List<OpenemsComponent> components = this.getComponents();
		for (OpenemsComponent component : components) {
			if (component.id().equals(componentId)) {
				return (T) component;
			}
		}
		throw new IllegalArgumentException("Component [" + componentId + "] is not available.");
	}
	// TODO should throw OpenemsNamedException

	/**
	 * Gets a Channel by its Channel-Address.
	 * 
	 * @param channelAddress the Channel-Address
	 * @throws IllegalArgumentException if the Channel is not available
	 * @return the Channel
	 */
	public default <T extends Channel<?>> T getChannel(ChannelAddress channelAddress) throws IllegalArgumentException {
		OpenemsComponent component = this.getComponent(channelAddress.getComponentId());
		return component.channel(channelAddress.getChannelId());
	} // TODO should throw OpenemsNamedException

	/**
	 * Gets the complete configuration of this OpenEMS Edge.
	 * 
	 * @return the EdgeConfig object
	 */
	public EdgeConfig getEdgeConfig();

}
