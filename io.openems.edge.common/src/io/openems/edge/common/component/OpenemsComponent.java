package io.openems.edge.common.component;

import java.util.Collection;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;

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
	 * Returns a Channel defined by its ChannelId.
	 * 
	 * @param channelId
	 * @return
	 */
	Channel channel(io.openems.edge.common.channel.doc.ChannelId channelId);

	/**
	 * Returns all Channels
	 * 
	 * @return
	 */
	Collection<Channel> channels();

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		// Running State of the component
		STATE(new Doc().unit(Unit.NONE) //
				.option(0, "Ok") //
				.option(1, "Warning") //
				.option(2, "Fault"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	default StateChannel getState() {
		Channel channel = this.channel(ChannelId.STATE);
		if (!(channel instanceof StateChannel)) {
			throw new IllegalArgumentException("Channel [State] must be of type 'StateChannel'.");
		}
		return (StateChannel) channel;
	}
}
