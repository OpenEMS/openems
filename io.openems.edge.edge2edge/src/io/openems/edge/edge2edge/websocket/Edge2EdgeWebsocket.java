package io.openems.edge.edge2edge.websocket;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface Edge2EdgeWebsocket extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		REMOTE_NO_CONNECTION(Doc.of(Level.FAULT)), //
		REMOTE_NO_COMPONENT(Doc.of(Level.FAULT)), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_NO_CONNECTION}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getRemoteNoConnectionChannel() {
		return this.channel(ChannelId.REMOTE_NO_CONNECTION);
	}

	/**
	 * Gets the boolean if the state channel is active. See
	 * {@link ChannelId#REMOTE_NO_CONNECTION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getRemoteNoConnection() {
		return this.getRemoteNoConnectionChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#REMOTE_NO_CONNECTION} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRemoteNoConnection(Boolean value) {
		this.getRemoteNoConnectionChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_NO_COMPONENT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getRemoteNoComponentChannel() {
		return this.channel(ChannelId.REMOTE_NO_COMPONENT);
	}

	/**
	 * Gets the boolean if the state channel is active. See
	 * {@link ChannelId#REMOTE_NO_COMPONENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getRemoteNoComponentFault() {
		return this.getRemoteNoComponentChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#REMOTE_NO_COMPONENT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRemoteNoComponentFault(Boolean value) {
		this.getRemoteNoComponentChannel().setNextValue(value);
	}
}
