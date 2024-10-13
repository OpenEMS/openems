package io.openems.edge.ess.api;

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

@ProviderType
public interface SymmetricEss extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * State of Charge.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: %
		 * <li>Range: 0..100
		 * </ul>
		 */
		SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Capacity.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 *
		 * @since 2019.5.0
		 */
		CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Grid-Mode.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer/Enum
		 * <li>Range: 0=Undefined, 1=On-Grid, 2=Off-Grid
		 * </ul>
		 */
		GRID_MODE(Doc.of(GridMode.values()) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Active Power.
		 *
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Negative values for Charge; positive for Discharge") //
		),
		/**
		 * Reactive Power.
		 *
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * </ul>
		 */
		REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.persistencePriority(PersistencePriority.HIGH) //
		),
		/**
		 * Holds the currently maximum possible apparent power. This value is commonly
		 * defined by the inverter limitations.
		 *
		 * <ul>
		 * <li>Interface: Managed Symmetric Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: zero or positive value
		 * </ul>
		 */
		MAX_APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Active Charge Energy.
		 *
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		ACTIVE_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Active Discharge Energy.
		 *
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		ACTIVE_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Min Cell Voltage.
		 *
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: mV
		 * <li>Range: > 0
		 * </ul>
		 *
		 * @since 2019.12.0
		 */
		MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Max Cell Voltage.
		 *
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: mV
		 * <li>Range: > 0
		 * </ul>
		 *
		 * @since 2019.17.0
		 */
		MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Min Cell Temperature.
		 *
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: °C
		 * <li>Range: -273 to positive infinity
		 * </ul>
		 *
		 * @since 2019.17.0
		 */
		MIN_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Max Cell Temperature.
		 *
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: °C
		 * <li>Range: -273 to positive infinity
		 * </ul>
		 *
		 * @since 2019.17.0
		 */
		MAX_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.HIGH));

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
		return ModbusSlaveNatureTable.of(SymmetricEss.class, accessMode, 100) //
				.channel(0, ChannelId.SOC, ModbusType.UINT16) //
				.channel(1, ChannelId.GRID_MODE, ModbusType.UINT16) //
				.channel(2, ChannelId.ACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(4, ChannelId.REACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(6, ChannelId.MIN_CELL_VOLTAGE, ModbusType.FLOAT32) //
				.channel(8, ChannelId.MAX_CELL_VOLTAGE, ModbusType.FLOAT32) //
				.channel(10, ChannelId.MIN_CELL_TEMPERATURE, ModbusType.FLOAT32) //
				.channel(12, ChannelId.MAX_CELL_TEMPERATURE, ModbusType.FLOAT32) //
				.channel(14, ChannelId.CAPACITY, ModbusType.FLOAT32) //
				.build();
	}

	/**
	 * Gets the Channel for {@link ChannelId#SOC}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getSocChannel() {
		return this.channel(ChannelId.SOC);
	}

	/**
	 * Gets the State of Charge in [%], range 0..100 %. See {@link ChannelId#SOC}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSoc() {
		return this.getSocChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SOC} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSoc(Integer value) {
		this.getSocChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SOC} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSoc(int value) {
		this.getSocChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CAPACITY}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCapacityChannel() {
		return this.channel(ChannelId.CAPACITY);
	}

	/**
	 * Gets the Capacity in [Wh]. See {@link ChannelId#CAPACITY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCapacity() {
		return this.getCapacityChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CAPACITY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCapacity(Integer value) {
		this.getCapacityChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CAPACITY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCapacity(int value) {
		this.getCapacityChannel().setNextValue(value);
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
	 * Is the Energy Storage System On-Grid? See {@link ChannelId#GRID_MODE}.
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
	 * Gets the Channel for {@link ChannelId#MIN_CELL_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMinCellVoltageChannel() {
		return this.channel(ChannelId.MIN_CELL_VOLTAGE);
	}

	/**
	 * Gets the Minimum Cell Voltage in [mV]. See
	 * {@link ChannelId#MIN_CELL_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMinCellVoltage() {
		return this.getMinCellVoltageChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MIN_CELL_VOLTAGE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMinCellVoltage(Integer value) {
		this.getMinCellVoltageChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MIN_CELL_VOLTAGE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMinCellVoltage(int value) {
		this.getMinCellVoltageChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_CELL_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxCellVoltageChannel() {
		return this.channel(ChannelId.MAX_CELL_VOLTAGE);
	}

	/**
	 * Gets the Maximum Cell Voltage in [mV]. See
	 * {@link ChannelId#MAX_CELL_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxCellVoltage() {
		return this.getMaxCellVoltageChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_CELL_VOLTAGE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxCellVoltage(Integer value) {
		this.getMaxCellVoltageChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_CELL_VOLTAGE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxCellVoltage(int value) {
		this.getMaxCellVoltageChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MIN_CELL_TEMPERATURE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMinCellTemperatureChannel() {
		return this.channel(ChannelId.MIN_CELL_TEMPERATURE);
	}

	/**
	 * Gets the Minimal Cell Temperature in [degC]. See
	 * {@link ChannelId#MIN_CELL_TEMPERATURE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMinCellTemperature() {
		return this.getMinCellTemperatureChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MIN_CELL_TEMPERATURE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMinCellTemperature(Integer value) {
		this.getMinCellTemperatureChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MIN_CELL_TEMPERATURE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMinCellTemperature(int value) {
		this.getMinCellTemperatureChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_CELL_TEMPERATURE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxCellTemperatureChannel() {
		return this.channel(ChannelId.MAX_CELL_TEMPERATURE);
	}

	/**
	 * Gets the Maximum Cell Temperature in [degC]. See
	 * {@link ChannelId#MAX_CELL_TEMPERATURE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxCellTemperature() {
		return this.getMaxCellTemperatureChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_CELL_TEMPERATURE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxCellTemperature(Integer value) {
		this.getMaxCellTemperatureChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_CELL_TEMPERATURE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxCellTemperature(int value) {
		this.getMaxCellTemperatureChannel().setNextValue(value);
	}
}
