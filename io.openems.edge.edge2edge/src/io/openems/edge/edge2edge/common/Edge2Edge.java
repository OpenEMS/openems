package io.openems.edge.edge2edge.common;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface Edge2Edge extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		REMOTE_NO_OPENEMS(Doc.of(Level.FAULT)), //
		MAPPING_REMOTE_PROTOCOL_FAULT(Doc.of(Level.FAULT)), //
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
	 * Gets the Channel for {@link ChannelId#REMOTE_NO_OPENEMS}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getRemoteNoOpenemsChannel() {
		return this.channel(ChannelId.REMOTE_NO_OPENEMS);
	}

	/**
	 * Gets the boolean if the state channel is active. See
	 * {@link ChannelId#REMOTE_NO_OPENEMS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getRemoteNoOpenems() {
		return this.getMappingRemoteProtocolFaultChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#REMOTE_NO_OPENEMS}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRemoteNoOpenems(Boolean value) {
		this.getRemoteNoOpenemsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAPPING_REMOTE_PROTOCOL_FAULT}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMappingRemoteProtocolFaultChannel() {
		return this.channel(ChannelId.MAPPING_REMOTE_PROTOCOL_FAULT);
	}

	/**
	 * Gets the boolean if the state channel is active. See
	 * {@link ChannelId#MAPPING_REMOTE_PROTOCOL_FAULT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMappingRemoteProtocolFault() {
		return this.getMappingRemoteProtocolFaultChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAPPING_REMOTE_PROTOCOL_FAULT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMappingRemoteProtocolFault(Boolean value) {
		this.getMappingRemoteProtocolFaultChannel().setNextValue(value);
	}
}
