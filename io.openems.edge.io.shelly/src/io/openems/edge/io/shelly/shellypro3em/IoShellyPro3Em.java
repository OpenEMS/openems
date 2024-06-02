package io.openems.edge.io.shelly.shellypro3em;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.channel.value.Value;

public interface IoShellyPro3Em extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Phase Sequence Error.
		 *
		 * <p>
		 * Represents an error indicating if the sequence of zero-crossing events is
		 * Phase A followed by Phase C followed by Phase B. The regular succession of
		 * these zero-crossing events is Phase A followed by Phase B followed by Phase
		 * C.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlug
		 * <li>Type: State
		 * </ul>
		 */
		PHASE_SEQUENCE_ERROR(Doc.of(Level.FAULT) //
				.text("Incorrect phase sequence. Expected A-B-C but found A-C-B.")),

		/**
		 * Power Meter Failure.
		 *
		 * <p>
		 * Represents a failure in the power meter, potentially leading to inaccurate or
		 * missing measurements.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlug
		 * <li>Type: State
		 * </ul>
		 */
		POWER_METER_FAILURE(Doc.of(Level.FAULT) //
				.text("Power meter failure; unable to record or measure power accurately.")),

		/**
		 * No Load Error.
		 *
		 * <p>
		 * Indicates that the power meter is in a no-load condition and is not
		 * accumulating the registered energies, therefore, the measured values can be
		 * discarded.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlug
		 * <li>Type: State
		 * </ul>
		 */
		NO_LOAD(Doc.of(Level.FAULT) //
				.text("No load condition detected; the power meter is not accumulating energy.")),

		/**
		 * Slave Communication Failed Fault.
		 *
		 * <p>
		 * Indicates a failure in communication with a slave device, which might affect
		 * system operations.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlug
		 * <li>Type: State
		 * </ul>
		 */
		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
				.text("Communication with slave device failed."));

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
	 * Gets the Channel for {@link ChannelId#SLAVE_COMMUNICATION_FAILED}.
	 *
	 * @return the StateChannel representing communication failure with a slave
	 *         device.
	 */
	public default StateChannel getSlaveCommunicationFailedChannel() {
		return this.channel(ChannelId.SLAVE_COMMUNICATION_FAILED);
	}

	/**
	 * Gets the current state of the Slave Communication Failed channel.
	 *
	 * @return the Channel {@link Value} indicating whether communication has
	 *         failed.
	 */
	public default Value<Boolean> getSlaveCommunicationFailed() {
		return this.getSlaveCommunicationFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SLAVE_COMMUNICATION_FAILED} Channel.
	 *
	 * @param value the next value indicating communication failure state.
	 */
	public default void _setSlaveCommunicationFailed(boolean value) {
		this.getSlaveCommunicationFailedChannel().setNextValue(value);
	}

}
