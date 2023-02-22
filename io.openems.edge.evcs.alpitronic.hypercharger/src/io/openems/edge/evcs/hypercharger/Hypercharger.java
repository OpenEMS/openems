package io.openems.edge.evcs.hypercharger;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Hypercharger EV charging protocol interface.
 * 
 * <p>
 * Defines the interface for Alpitronic Hypercharger
 */
public interface Hypercharger extends OpenemsComponent {

	public enum Connector {
		SLOT_0(100), //
		SLOT_1(200), //
		SLOT_2(300), //
		SLOT_3(400);

		public final int modbusOffset;

		private Connector(int modbusOffset) {
			this.modbusOffset = modbusOffset;
		}
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

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
		APPLY_CHARGE_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH)),

		CHARGING_VOLTAGE(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.VOLT)),

		CHARGING_CURRENT(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.AMPERE)),

		RAW_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),

		CHARGED_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS)),

		CHARGED_ENERGY(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.KILOWATT_HOURS)),

		EV_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)),

		CONNECTOR_TYPE(Doc.of(SelectedConnector.values())),

		EV_MAX_CHARGING_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),

		EV_MIN_CHARGING_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),

		/**
		 * Maximum possible inductive VAR, e. g. 1500 VAR
		 */
		VAR_REACTIVE_MAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)),

		/**
		 * Maximum possible capacitive VAR, e. g. -1500 VAR
		 */
		VAR_REACTIVE_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)),

		SETPOINT_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.WRITE_ONLY)),

		RAW_CHARGE_POWER_SET(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),;

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
	 * Gets the Channel for {@link ChannelId#APPLY_CHARGE_POWER_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getApplyChargePowerLimitChannel() {
		return this.channel(ChannelId.APPLY_CHARGE_POWER_LIMIT);
	}

	/**
	 * Sets the charge power limit of the EVCS in [W] on
	 * {@link ChannelId#APPLY_CHARGE_POWER_LIMIT} Channel.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setApplyChargePowerLimit(Integer value) throws OpenemsNamedException {
		this.getApplyChargePowerLimitChannel().setNextWriteValue(value);
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
