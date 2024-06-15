package io.openems.edge.batteryinverter.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.sum.GridMode;

/**
 * Represents a Symmetric Battery-Inverter.
 */
@ProviderType
public interface SymmetricBatteryInverter extends OpenemsComponent {

	public static final String POWER_DOC_TEXT = "Negative values for Charge; positive for Discharge";

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Grid-Mode.
		 *
		 * <ul>
		 * <li>Interface: SymmetricBatteryInverter
		 * <li>Type: Integer/Enum
		 * <li>Range: 0=Undefined, 1=On-Grid, 2=Off-Grid
		 * </ul>
		 */
		GRID_MODE(Doc.of(GridMode.values()) //
				.persistencePriority(PersistencePriority.HIGH) //
		),
		/**
		 * Active Power.
		 *
		 * <ul>
		 * <li>Interface: SymmetricBatteryInverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(POWER_DOC_TEXT) //
				.persistencePriority(PersistencePriority.HIGH) //
		),
		/**
		 * Reactive Power.
		 *
		 * <ul>
		 * <li>Interface: SymmetricBatteryInverter
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.text(POWER_DOC_TEXT) //
				.persistencePriority(PersistencePriority.HIGH) //
		),
		/**
		 * Holds the currently maximum possible apparent power. This value is commonly
		 * defined by the inverter limitations.
		 *
		 * <ul>
		 * <li>Interface: SymmetricBatteryInverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: zero or positive value
		 * </ul>
		 */
		MAX_APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE) //
				.persistencePriority(PersistencePriority.HIGH) //
		),
		/**
		 * Active Charge Energy.
		 *
		 * <ul>
		 * <li>Interface: SymmetricBatteryInverter
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		ACTIVE_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH) //
		),
		/**
		 * Active Discharge Energy.
		 *
		 * <ul>
		 * <li>Interface: SymmetricBatteryInverter
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		ACTIVE_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)//
		), //

		/**
		 * Inverter DC Minimum Voltage.
		 *
		 * <ul>
		 * <li>Interface: SymmetricBatteryInverter
		 * <li>Type: Integer
		 * <li>Unit: V
		 * </ul>
		 */
		DC_MIN_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)//
		), //

		/**
		 * Inverter DC Max Voltage.
		 *
		 * <ul>
		 * <li>Interface: SymmetricBatteryInverter
		 * <li>Type: Integer
		 * <li>Unit: V
		 * </ul>
		 */
		DC_MAX_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH) //
		);

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
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(SymmetricBatteryInverter.class, accessMode, 100) //
				.channel(0, ChannelId.GRID_MODE, ModbusType.UINT16) //
				.channel(1, ChannelId.ACTIVE_POWER, ModbusType.FLOAT32) //
				.build();
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_MODE}.
	 *
	 * @return the Channel
	 */
	public default Channel<GridMode> getGridModeChannel() {
		return this.channel(ChannelId.GRID_MODE);
	}

	/**
	 * Is the Battery-Inverter On-Grid? See {@link ChannelId#GRID_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default GridMode getGridMode() {
		return this.getGridModeChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#GRID_MODE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridMode(GridMode value) {
		this.getGridModeChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getActivePowerChannel() {
		return this.channel(ChannelId.ACTIVE_POWER);
	}

	/**
	 * Gets the Active Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActivePower() {
		return this.getActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActivePower(Integer value) {
		this.getActivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActivePower(int value) {
		this.getActivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#REACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getReactivePowerChannel() {
		return this.channel(ChannelId.REACTIVE_POWER);
	}

	/**
	 * Gets the Reactive Power in [var]. See {@link ChannelId#REACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getReactivePower() {
		return this.getReactivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#REACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setReactivePower(Integer value) {
		this.getReactivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#REACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setReactivePower(int value) {
		this.getReactivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_APPARENT_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxApparentPowerChannel() {
		return this.channel(ChannelId.MAX_APPARENT_POWER);
	}

	/**
	 * Gets the Maximum Apparent Power in [VA], range "&gt;= 0". See
	 * {@link ChannelId#MAX_APPARENT_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxApparentPower() {
		return this.getMaxApparentPowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_APPARENT_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxApparentPower(Integer value) {
		this.getMaxApparentPowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_APPARENT_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxApparentPower(int value) {
		this.getMaxApparentPowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_CHARGE_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getActiveChargeEnergyChannel() {
		return this.channel(ChannelId.ACTIVE_CHARGE_ENERGY);
	}

	/**
	 * Gets the Active Charge Energy in [Wh_Σ]. See
	 * {@link ChannelId#ACTIVE_CHARGE_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getActiveChargeEnergy() {
		return this.getActiveChargeEnergyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_CHARGE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActiveChargeEnergy(Long value) {
		this.getActiveChargeEnergyChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_CHARGE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActiveChargeEnergy(long value) {
		this.getActiveChargeEnergyChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_DISCHARGE_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getActiveDischargeEnergyChannel() {
		return this.channel(ChannelId.ACTIVE_DISCHARGE_ENERGY);
	}

	/**
	 * Gets the Active Discharge Energy in [Wh_Σ]. See
	 * {@link ChannelId#ACTIVE_DISCHARGE_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getActiveDischargeEnergy() {
		return this.getActiveDischargeEnergyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_DISCHARGE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActiveDischargeEnergy(Long value) {
		this.getActiveDischargeEnergyChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_DISCHARGE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActiveDischargeEnergy(long value) {
		this.getActiveDischargeEnergyChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DC_MIN_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcMinVoltageChannel() {
		return this.channel(ChannelId.DC_MIN_VOLTAGE);
	}

	/**
	 * Gets the Minimum Inverter DC Voltage in [V]. See
	 * {@link ChannelId#DC_MIN_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcMinVoltage() {
		return this.getDcMinVoltageChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#DC_MIN_VOLTAGE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDcMinVoltage(Integer value) {
		this.getDcMinVoltageChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#DC_MIN_VOLTAGE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDcMinVoltage(int value) {
		this.getDcMinVoltageChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DC_MAX_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcMaxVoltageChannel() {
		return this.channel(ChannelId.DC_MAX_VOLTAGE);
	}

	/**
	 * Gets the Maximum Inverter DC Voltage in [V]. See
	 * {@link ChannelId#DC_MAX_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcMaxVoltage() {
		return this.getDcMaxVoltageChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#DC_MAX_VOLTAGE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDcMaxVoltage(Integer value) {
		this.getDcMaxVoltageChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#DC_MAX_VOLTAGE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDcMaxVoltage(int value) {
		this.getDcMaxVoltageChannel().setNextValue(value);
	}
}
