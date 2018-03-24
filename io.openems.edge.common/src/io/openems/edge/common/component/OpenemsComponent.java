package io.openems.edge.common.component;

import java.util.Collection;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelDoc;

public interface OpenemsComponent {

	/**
	 * Returns a unique ID for this Thing (i.e. the OSGi service.pid)
	 * 
	 * @return
	 */
	String id();

	/**
	 * Returns whether this component is active (i.e. the OSGi components'
	 * activator() was executed, but deactivate() was not)
	 * 
	 * @return
	 */
	boolean isActive();

	/**
	 * Returns whether this component is enabled
	 * 
	 * @return
	 */
	boolean isEnabled();

	/**
	 * Enables or disables this component
	 */
	void setEnabled(boolean isEnabled);

	/**
	 * Returns a Channel defined by its ChannelId Enum
	 * 
	 * @param channelId
	 * @return
	 */
	Channel getChannel(ChannelDoc channelId);

	/**
	 * Returns all Channels
	 * 
	 * @return
	 */
	Collection<Channel> getChannels();
}
