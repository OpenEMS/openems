package io.openems.edge.io.shelly.shelly25;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.api.DigitalOutput;

public interface IoShelly25 extends DigitalOutput, OpenemsComponent, EventHandler {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Holds writes to Relay Output 1 for debugging.
		 *
		 * <ul>
		 * <li>Interface: Shelly25
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_1(Doc.of(OpenemsType.BOOLEAN)), //
		/**
		 * Relay Output 1.
		 *
		 * <ul>
		 * <li>Interface: Shelly25
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY_1(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_1)),

		/**
		 * Indicates whether the associated meter is functioning properly.
		 *
		 * <ul>
		 * <li>Interface: Shelly25
		 * <li>Type: Boolean
		 * <li>Level: WARN
		 * </ul>
		 */
		RELAY_1_OVERTEMP(Doc.of(Level.WARNING) //
				.text("Relay 1 has been switched off due to Overtemperature.")),
		/**
		 * Indicates whether the associated Relay is in Overpower-State.
		 *
		 * <ul>
		 * <li>Interface: Shelly25
		 * <li>Type: Boolean
		 * <li>Level: WARN
		 * </ul>
		 */
		RELAY_1_OVERPOWER(Doc.of(Level.WARNING) //
				.text("Relay 2 has been switched off due to Overpower.")),
		/**
		 * Holds writes to Relay Output 2 for debugging.
		 *
		 * <ul>
		 * <li>Interface: Shelly25
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_2(Doc.of(OpenemsType.BOOLEAN)), //
		/**
		 * Relay Output 2.
		 *
		 * <ul>
		 * <li>Interface: Shelly25
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY_2(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_2)),
		/**
		 * Indicates whether the associated Relay is in Overtemp-State.
		 *
		 * <ul>
		 * <li>Interface: Shelly25
		 * <li>Type: Boolean
		 * <li>Level: WARN
		 * </ul>
		 */
		RELAY_2_OVERTEMP(Doc.of(Level.WARNING) //
				.text("Relay 2 has been switched off due to Overtemperature.")),
		/**
		 * Indicates whether the associated Relay is in Overpower-State.
		 *
		 * <ul>
		 * <li>Interface: Shelly25
		 * <li>Type: Boolean
		 * <li>Level: WARN
		 * </ul>
		 */
		RELAY_2_OVERPOWER(Doc.of(Level.WARNING) //
				.text("Relay 2 has been switched off due to Overpower.")),
		/**
		 * Slave Communication Failed Fault.
		 *
		 * <ul>
		 * <li>Interface: Shelly25
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
	 * Gets the Channel for {@link ChannelId#RELAY_1}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelay1Channel() {
		return this.channel(ChannelId.RELAY_1);
	}

	/**
	 * Gets the Relay Output 1. See {@link ChannelId#RELAY_1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getRelay1() {
		return this.getRelay1Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#RELAY_1} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRelay1(Boolean value) {
		this.getRelay1Channel().setNextValue(value);
	}

	/**
	 * Sets the Relay Output 1. See {@link ChannelId#RELAY_1}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setRelay1(boolean value) throws OpenemsNamedException {
		this.getRelay1Channel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#RELAY_1_OVERTEMP}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelay1OvertempChannel() {
		return this.channel(ChannelId.RELAY_1_OVERTEMP);
	}

	/**
	 * Gets the Overtemperature State for Relay 1. See
	 * {@link ChannelId#RELAY_1_OVERTEMP}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getRelay1Overtemp() {
		return this.getRelay1OvertempChannel().value();
	}

	/**
	 * Sets the Overtemperature State for Relay 1. See
	 * {@link ChannelId#RELAY_1_OVERTEMP}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setRelay1Overtemp(boolean value) throws OpenemsNamedException {
		this.getRelay1OvertempChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#RELAY_2_OVERTEMP}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelay2OvertempChannel() {
		return this.channel(ChannelId.RELAY_2_OVERTEMP);
	}

	/**
	 * Gets the Overtemperature State for Relay 2. See
	 * {@link ChannelId#RELAY_2_OVERTEMP}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getRelay2Overtemp() {
		return this.getRelay2OvertempChannel().value();
	}

	/**
	 * Sets the Overtemperature State for Relay 2. See
	 * {@link ChannelId#RELAY_2_OVERTEMP}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setRelay2Overtemp(boolean value) throws OpenemsNamedException {
		this.getRelay2OvertempChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#RELAY_1_OVERPOWER}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelay1OverpowerChannel() {
		return this.channel(ChannelId.RELAY_1_OVERPOWER);
	}

	/**
	 * Gets the Overpower State for Relay 1. See
	 * {@link ChannelId#RELAY_1_OVERPOWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getRelay1Overpower() {
		return this.getRelay1OverpowerChannel().value();
	}

	/**
	 * Sets the Overpower State for Relay 1. See
	 * {@link ChannelId#RELAY_1_OVERPOWER}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setRelay1Overpower(boolean value) throws OpenemsNamedException {
		this.getRelay1OverpowerChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#RELAY_2_OVERPOWER}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelay2OverpowerChannel() {
		return this.channel(ChannelId.RELAY_2_OVERPOWER);
	}

	/**
	 * Gets the Overpower State for Relay 2. See
	 * {@link ChannelId#RELAY_2_OVERPOWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getRelay2Overpower() {
		return this.getRelay2OverpowerChannel().value();
	}

	/**
	 * Sets the Overpower State for Relay 2. See
	 * {@link ChannelId#RELAY_2_OVERPOWER}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setRelay2Overpower(boolean value) throws OpenemsNamedException {
		this.getRelay2OverpowerChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#RELAY_2}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelay2Channel() {
		return this.channel(ChannelId.RELAY_2);
	}

	/**
	 * Gets the Relay Output 2. See {@link ChannelId#RELAY_2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getRelay2() {
		return this.getRelay2Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#RELAY_2} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRelay2(Boolean value) {
		this.getRelay2Channel().setNextValue(value);
	}

	/**
	 * Sets the Relay Output 2. See {@link ChannelId#RELAY_2}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setRelay2(boolean value) throws OpenemsNamedException {
		this.getRelay2Channel().setNextWriteValue(value);
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
