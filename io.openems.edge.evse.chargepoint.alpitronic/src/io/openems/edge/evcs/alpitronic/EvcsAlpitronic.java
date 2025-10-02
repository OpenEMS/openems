package io.openems.edge.evcs.alpitronic;

import static io.openems.common.channel.Unit.AMPERE;
import static io.openems.common.channel.Unit.VOLT;
import static io.openems.common.types.OpenemsType.DOUBLE;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.chargepoint.alpitronic.enums.AvailableState;

/**
 * Hypercharger EV charging protocol interface.
 * 
 * <p>
 * Defines the interface for Alpitronic Hypercharger
 */
public interface EvcsAlpitronic extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		RAW_STATUS(Doc.of(AvailableState.values())), //
		CHARGING_VOLTAGE(Doc.of(DOUBLE).unit(VOLT)), //
		CHARGING_CURRENT(Doc.of(DOUBLE).unit(AMPERE));

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
	 * Gets the Channel for {@link ChannelId#CHARGING_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default DoubleReadChannel getChargingCurrentChannel() {
		return this.channel(ChannelId.CHARGING_CURRENT);
	}

	/**
	 * Gets the Charge Current in [A]. See {@link ChannelId#CHARGING_CURRENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Double> getChargingCurrent() {
		return this.getChargingCurrentChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CHARGING_CURRENT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChargingCurrent(Double value) {
		this.getChargingCurrentChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CHARGING_CURRENT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChargingCurrent(double value) {
		this.getChargingCurrentChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGING_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default DoubleReadChannel getChargingVoltageChannel() {
		return this.channel(ChannelId.CHARGING_VOLTAGE);
	}

	/**
	 * Gets the Charge Voltage in [V]. See {@link ChannelId#CHARGING_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Double> getChargingVoltage() {
		return this.getChargingVoltageChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CHARGING_VOLTAGE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChargingVoltage(Double value) {
		this.getChargingVoltageChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CHARGING_VOLTAGE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChargingVoltage(double value) {
		this.getChargingVoltageChannel().setNextValue(value);
	}
}
