package io.openems.edge.io.shelly.shelly3em;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.meter.api.ElectricityMeter;

public interface IoShelly3Em extends DigitalOutput, ElectricityMeter, OpenemsComponent, EventHandler {

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
		RELAY(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY)),
		/**
		 * Indicates if an update is available.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlug
		 * <li>Type: Boolean
		 * <li>Level: INFO
		 * </ul>
		 */
		HAS_UPDATE(Doc.of(Level.INFO) //
				.text("A new Firmware Update is available.")),
		/**
		 * Indicates whether the associated meter is functioning properly.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlug
		 * <li>Type: Boolean
		 * <li>Level: WARN
		 * </ul>
		 */
		EMETER1_EXCEPTION(Doc.of(Level.WARNING) //
				.text("E-Meter Phase 1 is not valid.")),
		/**
		 * Indicates whether the associated meter is functioning properly.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlug
		 * <li>Type: Boolean
		 * <li>Level: WARN
		 * </ul>
		 */
		EMETER2_EXCEPTION(Doc.of(Level.WARNING) //
				.text("E-Meter Phase 2 is not valid.")),
		/**
		 * Indicates whether the associated meter is functioning properly.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlug
		 * <li>Type: Boolean
		 * <li>Level: WARN
		 * </ul>
		 */
		EMETER3_EXCEPTION(Doc.of(Level.WARNING) //
				.text("E-Meter Phase 3 is not valid.")),
		/**
		 * Indicates whether the associated meter is functioning properly.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlug
		 * <li>Type: Boolean
		 * <li>Level: WARN
		 * </ul>
		 */
		EMETERN_EXCEPTION(Doc.of(Level.WARNING) //
				.text("E-Meter Phase N is not valid.")),
		/**
		 * Indicates whether the Relay is in an Overpower Condition.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlug
		 * <li>Type: Boolean
		 * <li>Level: WARN
		 * </ul>
		 */
		RELAY_OVERPOWER_EXCEPTION(Doc.of(Level.WARNING) //
				.text("Relay is in overpower condition.")),
		/**
		 * Slave Communication Failed Fault.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlug
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
	 * Internal method to set the 'nextValue' on {@link ChannelId#RELAY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRelay(Boolean value) {
		this.getRelayChannel().setNextValue(value);
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

	/**
	 * Gets the Channel for {@link ChannelId#SLAVE_COMMUNICATION_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getSlaveCommunicationFailedChannel() {
		return this.channel(ChannelId.SLAVE_COMMUNICATION_FAILED);
	}

	/**
	 * Gets the Slave Communication Failed State. See
	 * {@link ChannelId#SLAVE_COMMUNICATION_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getSlaveCommunicationFailed() {
		return this.getSlaveCommunicationFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SLAVE_COMMUNICATION_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSlaveCommunicationFailed(boolean value) {
		this.getSlaveCommunicationFailedChannel().setNextValue(value);
	}

}
