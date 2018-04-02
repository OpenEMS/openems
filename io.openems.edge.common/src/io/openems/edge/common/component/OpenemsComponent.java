package io.openems.edge.common.component;

import java.util.Collection;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.ChannelDoc;
import io.openems.edge.common.channel.doc.Option;
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
	 * Returns a Channel defined by its ChannelId Enum
	 * 
	 * @param channelId
	 * @return
	 */
	Channel channel(ChannelDoc channelId);

	/**
	 * Returns all Channels
	 * 
	 * @return
	 */
	Collection<Channel> channels();

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelDoc {
		// Running State of the component
		STATE(Unit.NONE, State.OK);
		enum State implements Option {
			OK, WARNING, FAULT
		}

		private final Unit unit;
		private final Option options;

		private ChannelId(Unit unit, Option options) {
			this.unit = unit;
			this.options = options;
		}

		@Override
		public Unit getUnit() {
			return this.unit;
		}

		@Override
		public Option getOptions() {
			return this.options;
		}
	}

	default Channel getState() {
		return this.channel(ChannelId.STATE);
	}
}
