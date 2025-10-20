package io.openems.edge.pvinverter.kostal.piko;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public interface PvInverterKostalPiko extends ManagedSymmetricPvInverter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// Status
		STATUS(Doc.of(OpenemsType.STRING)//
				.text("Inverter Status")), //

		// Total Energy values
		TOTAL_YIELD(Doc.of(OpenemsType.LONG)//
				.unit(Unit.KILOWATT_HOURS)//
				.persistencePriority(PersistencePriority.HIGH)), //
		DAY_YIELD(Doc.of(OpenemsType.LONG)//
				.unit(Unit.KILOWATT_HOURS)//
				.persistencePriority(PersistencePriority.HIGH)), //

		// DC String 1
		DC_STRING1_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)//
				.persistencePriority(PersistencePriority.HIGH)), //
		DC_STRING1_CURRENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE)//
				.persistencePriority(PersistencePriority.HIGH)), //
		DC_STRING1_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)), //

		// DC String 2
		DC_STRING2_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)//
				.persistencePriority(PersistencePriority.HIGH)), //
		DC_STRING2_CURRENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE)//
				.persistencePriority(PersistencePriority.HIGH)), //
		DC_STRING2_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)), //

		// DC String 3
		DC_STRING3_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)//
				.persistencePriority(PersistencePriority.HIGH)), //
		DC_STRING3_CURRENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE)//
				.persistencePriority(PersistencePriority.HIGH)), //
		DC_STRING3_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)), //

		// Debug channel for HTML content
		DEBUG_HTML(Doc.of(OpenemsType.STRING)//
				.accessMode(AccessMode.READ_ONLY)//
				.persistencePriority(PersistencePriority.LOW)//
				.text("Raw HTML response for debugging")),

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