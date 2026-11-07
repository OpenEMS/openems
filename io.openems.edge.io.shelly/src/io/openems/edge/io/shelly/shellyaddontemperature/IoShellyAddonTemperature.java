package io.openems.edge.io.shelly.shellyaddontemperature;

import static io.openems.common.channel.Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.thermometer.api.Thermometer;

public interface IoShellyAddonTemperature extends Thermometer, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Communication with the Shelly failed or the response did not contain the
		 * configured temperature sensor.
		 *
		 * <ul>
		 * <li>Interface: IoShellyAddonTemperature
		 * <li>Type: State
		 * <li>Level: FAULT
		 * </ul>
		 */
		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
				.debounce(5, TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)), //

		/**
		 * Indicates whether the Shelly needs a restart.
		 *
		 * <ul>
		 * <li>Interface: IoShellyAddonTemperature
		 * <li>Type: State
		 * <li>Level: INFO
		 * </ul>
		 */
		NEEDS_RESTART(Doc.of(Level.INFO) //
				.text("Shelly suggests a restart."));

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
	 * Gets the Communication Failed Fault State. See
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
