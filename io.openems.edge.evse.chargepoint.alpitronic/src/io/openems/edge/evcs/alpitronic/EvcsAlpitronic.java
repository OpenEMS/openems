package io.openems.edge.evcs.alpitronic;

import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.DOUBLE;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.common.types.OpenemsType.STRING;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
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

		/**
		 * Unix time from charging station.
		 */
		UNIX_TIME(Doc.of(LONG)//
				.unit(Unit.SECONDS)),

		/**
		 * Number of physical connectors.
		 */
		NUM_CONNECTORS(Doc.of(INTEGER)),

		/**
		 * State of the charging station (0=Available, 8=Unavailable, 10=Faulted).
		 */
		STATION_STATE(Doc.of(INTEGER)),

		/**
		 * Total power drained from the grid by all connectors.
		 */
		TOTAL_STATION_POWER(Doc.of(INTEGER)//
				.unit(Unit.WATT)),

		/**
		 * Charging station serial number.
		 */
		SERIAL_NUMBER(Doc.of(STRING)),

		/**
		 * OCPP ChargepointId (32 character null-terminated string).
		 */
		CHARGEPOINT_ID(Doc.of(STRING)),

		/**
		 * Vehicle ID (8 bytes).
		 */
		VID(Doc.of(STRING)),

		/**
		 * OCPP idTag (20 character null-terminated string).
		 */
		ID_TAG(Doc.of(STRING)),

		/**
		 * Whether external load management controller has control.
		 */
		LOAD_MANAGEMENT_ENABLED(Doc.of(BOOLEAN)),

		/**
		 * Software version major.
		 */
		SOFTWARE_VERSION_MAJOR(Doc.of(INTEGER)),

		/**
		 * Software version minor.
		 */
		SOFTWARE_VERSION_MINOR(Doc.of(INTEGER)),

		/**
		 * Software version patch.
		 */
		SOFTWARE_VERSION_PATCH(Doc.of(INTEGER)),

		/**
		 * General status message applied to the entire charger.
		 */
		RAW_STATUS(Doc.of(AvailableState.values())),

		/**
		 * Apply charge power limit.
		 * 
		 * <p>
		 * WriteChannel for the modbus register to apply the charge power given by the
		 * applyChargePowerLimit method
		 */
		APPLY_CHARGE_POWER_LIMIT(Doc.of(INTEGER)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_WRITE)//
				.persistencePriority(PersistencePriority.HIGH)),

		CHARGING_VOLTAGE(Doc.of(DOUBLE)//
				.unit(Unit.VOLT)),

		CHARGING_CURRENT(Doc.of(DOUBLE)//
				.unit(Unit.AMPERE)),

		RAW_CHARGE_POWER(Doc.of(INTEGER)//
				.unit(Unit.WATT)),

		CHARGED_TIME(Doc.of(INTEGER)//
				.unit(Unit.SECONDS)),

		CHARGED_ENERGY(Doc.of(DOUBLE)//
				.unit(Unit.KILOWATT_HOURS)),

		EV_SOC(Doc.of(INTEGER)//
				.unit(Unit.PERCENT)),

		CONNECTOR_TYPE(Doc.of(SelectedConnector.values())),

		EV_MAX_CHARGING_POWER(Doc.of(INTEGER)//
				.unit(Unit.WATT)),

		EV_MIN_CHARGING_POWER(Doc.of(INTEGER)//
				.unit(Unit.WATT)),

		/**
		 * Maximum possible inductive VAR, e. g. 1500 VAR
		 */
		VAR_REACTIVE_MAX(Doc.of(INTEGER)//
				.unit(Unit.VOLT_AMPERE_REACTIVE)),

		/**
		 * Maximum possible capacitive VAR, e. g. -1500 VAR
		 */
		VAR_REACTIVE_MIN(Doc.of(INTEGER)//
				.unit(Unit.VOLT_AMPERE_REACTIVE)),

		SETPOINT_REACTIVE_POWER(Doc.of(INTEGER)//
				.unit(Unit.VOLT_AMPERE_REACTIVE)//
				.accessMode(AccessMode.WRITE_ONLY)),

		RAW_CHARGE_POWER_SET(Doc.of(INTEGER)//
				.unit(Unit.WATT)),

		/**
		 * Total charged energy counter.
		 */
		TOTAL_CHARGED_ENERGY(Doc.of(LONG)//
				.unit(Unit.WATT_HOURS)),

		/**
		 * Maximum AC charging power per connector.
		 */
		MAX_CHARGING_POWER_AC(Doc.of(INTEGER)//
				.unit(Unit.WATT));

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