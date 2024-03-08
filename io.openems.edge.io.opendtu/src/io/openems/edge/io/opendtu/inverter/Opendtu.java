package io.openems.edge.io.opendtu.inverter;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;

public interface Opendtu extends SinglePhaseMeter, ElectricityMeter, OpenemsComponent, EventHandler {

	/**
	 * Provides a channel for setting the Power Limit in percent.
	 *
	 * @return A {@link WriteChannel}<{@link Integer}> for the power limit, allowing
	 *         for the power limit to be set relative to the inverter's maximum
	 *         capacity. This limit is expressed as a percentage of the maximum
	 *         power output.
	 */
	public default WriteChannel<Integer> setPowerLimit() {
		return this.channel(ChannelId.RELATIVE_LIMIT);
	}

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Slave Communication Failed Fault.
		 *
		 * <ul>
		 * <li>Interface: Opendtu
		 * <li>Type: State
		 * </ul>
		 */
		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT)), //

		/**
		 * Maximum Ever Actual Power.
		 *
		 * <ul>
		 * <li>Interface: Opendtu
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive or '0'
		 * <li>Implementation Note: value is automatically derived from ACTUAL_POWER
		 * </ul>
		 */
		MAX_ACTUAL_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * The Limit Status as String of the Power Limit Response.
		 *
		 * <ul>
		 * <li>Interface: Opendtu
		 * <li>Type: String
		 * <li>Description: Reflects the outcome of the last power limit setting operation; expected values are "Ok", "Pending", "Failure".
		 * </ul>
		 */
		LIMIT_STATUS(Doc.of(OpenemsType.STRING) //
				.text("Limit Status")), //
		/**
		 * The relative Limit Power set to an Inverter.
		 *
		 * <ul>
		 * <li>Interface: Opendtu
		 * <li>Type: Integer
		 * <li>Unit: %
		 * <li>Description: Allows setting a relative power limit for the inverter in
		 * percentage of its maximum capacity.
		 * </ul>
		 */
		RELATIVE_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)), //
		/**
		 * The absolute Limit Power set to an Inverter.
		 *
		 * <ul>
		 * <li>Interface: Opendtu
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Description: Allows setting an absolute power limit for the inverter in watts.
		 * </ul>
		 */
		ABSOLUTE_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //

		/**
		 * Power Limit Setting Failed Fault.
		 *
		 * <ul>
		 * <li>Interface: Opendtu
		 * <li>Type: State
		 * </ul>
		 */
		POWER_LIMIT_FAULT(Doc.of(Level.FAULT)); //

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
