package io.openems.edge.common.component;

import java.util.Collection;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelDoc;
import io.openems.edge.common.channel.Unit;

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

	public enum ChannelId implements io.openems.edge.common.channel.ChannelDoc {
		// Running State of the component
		STATE(Unit.NONE, State.class);
		enum State {
			OK, WARNING, FAULT
		}

		private final Unit unit;
		private final Class<? extends Enum<?>> values;

		private ChannelId(Unit unit, Class<? extends Enum<?>> values) {
			this.unit = unit;
			this.values = values;
			// TODO use values
		}

		@Override
		public Unit getUnit() {
			return this.unit;
		}
	}

	default Channel getState() {
		return this.channel(ChannelId.STATE);
	}
}
