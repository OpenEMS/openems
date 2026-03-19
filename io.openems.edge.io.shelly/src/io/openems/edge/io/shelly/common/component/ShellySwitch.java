package io.openems.edge.io.shelly.common.component;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.io.api.DigitalOutput;

public interface ShellySwitch extends DigitalOutput {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Holds writes to Relay Output for debugging.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlug
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY(Doc.of(OpenemsType.BOOLEAN)), //
		/**
		 * Relay Output.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlug
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY(new BooleanDoc()//
				.accessMode(AccessMode.READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY));

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
	 * Gets the Channel for {@link ChannelId#RELAY}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelayChannel() {
		return this.channel(ChannelId.RELAY);
	}

	/**
	 * Gets the Relay Output 1. See {@link ChannelId#RELAY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getRelay() {
		return this.getRelayChannel().value();
	}

	/**
	 * Sets the Relay Output. See {@link ChannelId#RELAY}.
	 *
	 * @param value the next write value
	 * @throws OpenemsError.OpenemsNamedException on error
	 */
	public default void setRelay(boolean value) throws OpenemsError.OpenemsNamedException {
		this.getRelayChannel().setNextWriteValue(value);
	}
}
