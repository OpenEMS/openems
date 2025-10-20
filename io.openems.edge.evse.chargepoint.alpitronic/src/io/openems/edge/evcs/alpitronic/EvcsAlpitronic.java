package io.openems.edge.evcs.alpitronic;

import static io.openems.common.channel.Unit.AMPERE;
import static io.openems.common.channel.Unit.KILOWATT_HOURS;
import static io.openems.common.channel.Unit.PERCENT;
import static io.openems.common.channel.Unit.SECONDS;
import static io.openems.common.channel.Unit.VOLT;
import static io.openems.common.channel.Unit.VOLT_AMPERE_REACTIVE;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.channel.Unit.WATT_HOURS;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.DOUBLE;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.common.types.OpenemsType.STRING;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.chargepoint.alpitronic.enums.AvailableState;
import io.openems.edge.evse.chargepoint.alpitronic.enums.SelectedConnector;

/**
 * Hypercharger EV charging protocol interface.
 * 
 * <p>
 * Defines the interface for Alpitronic Hypercharger
 */
public interface EvcsAlpitronic extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		UNIX_TIME(Doc.of(LONG)//
				.unit(SECONDS)//
				.text("Unix time from charging station")),

		NUM_CONNECTORS(Doc.of(INTEGER)//
				.text("Number of physical connectors")),

		STATION_STATE(Doc.of(INTEGER)//
				.text("State of the charging station (0=Available, 8=Unavailable, 10=Faulted)")),

		TOTAL_STATION_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.text("Total power drained from the grid by all connectors")),

		SERIAL_NUMBER(Doc.of(STRING)//
				.text("Charging station serial number")),

		CHARGEPOINT_ID(Doc.of(STRING)//
				.text("OCPP ChargepointId (32 character null-terminated string)")),

		VID(Doc.of(STRING)//
				.text("Vehicle ID (8 bytes)")),

		ID_TAG(Doc.of(STRING)//
				.text("OCPP idTag (20 character null-terminated string)")),

		LOAD_MANAGEMENT_ENABLED(Doc.of(BOOLEAN)//
				.text("Whether external load management controller has control")),

		SOFTWARE_VERSION_MAJOR(Doc.of(INTEGER)//
				.text("Software version major")),

		SOFTWARE_VERSION_MINOR(Doc.of(INTEGER)//
				.text("Software version minor")),

		SOFTWARE_VERSION_PATCH(Doc.of(INTEGER)//
				.text("Software version patch")),

		RAW_STATUS(Doc.of(AvailableState.values())//
				.text("General status message applied to the entire charger")),

		APPLY_CHARGE_POWER_LIMIT(Doc.of(INTEGER)//
				.unit(WATT)//
				.text("Apply charge power limit")//
				.accessMode(AccessMode.READ_WRITE)//
				.persistencePriority(PersistencePriority.HIGH)),

		CHARGING_VOLTAGE(Doc.of(DOUBLE)//
				.unit(VOLT)//
				.text("Charging voltage")),

		CHARGING_CURRENT(Doc.of(DOUBLE)//
				.unit(AMPERE)//
				.text("Charging current")),

		RAW_CHARGE_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.text("Raw charge power")),

		CHARGED_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)//
				.text("Charged time")),

		CHARGED_ENERGY(Doc.of(DOUBLE)//
				.unit(KILOWATT_HOURS)//
				.text("Charged energy")),

		EV_SOC(Doc.of(INTEGER)//
				.unit(PERCENT)//
				.text("Electric vehicle state of charge")),

		CONNECTOR_TYPE(Doc.of(SelectedConnector.values())//
				.text("Connector type")),

		EV_MAX_CHARGING_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.text("Electric vehicle maximum charging power")),

		EV_MIN_CHARGING_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.text("Electric vehicle minimum charging power")),

		VAR_REACTIVE_MAX(Doc.of(INTEGER)//
				.unit(VOLT_AMPERE_REACTIVE)//
				.text("Maximum possible inductive VAR")),

		VAR_REACTIVE_MIN(Doc.of(INTEGER)//
				.unit(VOLT_AMPERE_REACTIVE)//
				.text("Maximum possible capacitive VAR")),

		SETPOINT_REACTIVE_POWER(Doc.of(INTEGER)//
				.unit(VOLT_AMPERE_REACTIVE)//
				.text("Setpoint reactive power")//
				.accessMode(AccessMode.WRITE_ONLY)),

		RAW_CHARGE_POWER_SET(Doc.of(INTEGER)//
				.unit(WATT)//
				.text("Raw charge power set")),

		TOTAL_CHARGED_ENERGY(Doc.of(LONG)//
				.unit(WATT_HOURS)//
				.text("Total charged energy counter")),

		MAX_CHARGING_POWER_AC(Doc.of(INTEGER)//
				.unit(WATT)//
				.text("Maximum AC charging power per connector"));

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