package io.openems.edge.common.component;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;

/**
 * A Service that provides access to OpenEMS-Components.
 */
public interface ComponentManager {

	/**
	 * Gets a OpenEMS-Component by its Component-ID.
	 * 
	 * @param componentId the Component-ID (e.g. "_sum")
	 * @return the OpenEMS-Component
	 * @throws IllegalArgumentException if the Component was not found
	 */
	public <T extends OpenemsComponent> T getComponent(String componentId) throws IllegalArgumentException;

	/**
	 * Gets a Channel by its Channel-Address.
	 * 
	 * @param channelAddress the Channel-Address
	 * @throws IllegalArgumentException if the Channel is not available
	 * @return the Channel
	 */
	public <T extends Channel<?>> T getChannel(ChannelAddress channelAddress) throws IllegalArgumentException;

}
