package io.openems.edge.io.shelly.shellypro3em;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface IoShellyPro3Em extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Phase Sequence Error.
		 *
		 * Represents an error indicating if the sequence of zero-crossing events is Phase A
		 * followed by Phase C followed by Phase B. The regular succession of these
		 * zero-crossing events is Phase A followed by Phase B followed by Phase C.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlug
		 * <li>Type: State
		 * </ul>
		 */
		PHASE_SEQUENCE_ERROR(Doc.of(Level.FAULT)
				.text("Incorrect Phase sequence (A-C-B, should be A-B-C)")),
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
