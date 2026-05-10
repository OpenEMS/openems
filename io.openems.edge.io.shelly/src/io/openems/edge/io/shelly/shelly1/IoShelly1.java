package io.openems.edge.io.shelly.shelly1;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.types.OpenemsType.BOOLEAN;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.api.DigitalOutput;

public interface IoShelly1 extends DigitalOutput, OpenemsComponent, EventHandler {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Holds writes to Relay Output for debugging.
		 *
		 * <ul>
		 * <li>Interface: Shelly1
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY(Doc.of(BOOLEAN)), //
		/**
		 * Relay Output.
		 *
		 * <ul>
		 * <li>Interface: Shelly1
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY(new BooleanDoc()//
				.accessMode(READ_WRITE)//
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY)),
		/**
		 * Slave Communication Failed Fault.
		 *
		 * <ul>
		 * <li>Interface: Shelly1
		 * <li>Type: State
		 * </ul>
		 */
		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT)); //

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
	 * @throws OpenemsNamedException on error
	 */
	public default void setRelay(boolean value) throws OpenemsNamedException {
		this.getRelayChannel().setNextWriteValue(value);
	}
}
