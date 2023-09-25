package io.openems.edge.battery.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.statemachine.AbstractStateMachine;

/**
 * Represents a Battery.
 *
 * <p>
 * To indicate, that the Battery is ready for charging/discharging, the
 * following Channels need to be set:
 *
 * <ul>
 * <li>StartStoppable.ChannelId.START_STOP must be set to 'START'
 * <li>No 'Fault'-StateChannels are set (i.e. 'OpenemsComponent.ChannelId.STATE'
 * is < 3)
 * <li>CHARGE_MAX_VOLTAGE, CHARGE_MAX_CURRENT, DISCHARGE_MIN_VOLTAGE and
 * DISCHARGE_MAX_CURRENT are != null
 * </ul>
 */
@ProviderType
public interface Battery extends StartStoppable, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * State of Charge.
		 *
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: %
		 * <li>Range: 0..100
		 * </ul>
		 */
		SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * State of Health.
		 *
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: %
		 * <li>Range: 0..100
		 * </ul>
		 */
		SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Voltage of battery.
		 *
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: V
		 * </ul>
		 */
		VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Current of battery.
		 *
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: A
		 * </ul>
		 */
		CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Capacity of battery.
		 *
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Maximal voltage for charging.
		 *
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: V
		 * </ul>
		 */
		CHARGE_MAX_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Maximum current for charging.
		 *
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>Usually positive, negative for force discharge mode
		 * </ul>
		 */
		CHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Minimal voltage for discharging.
		 *
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: V
		 * </ul>
		 */
		DISCHARGE_MIN_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Maximum current for discharging.
		 *
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>Usually positive, negative for force charge mode
		 * </ul>
		 */
		DISCHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Minimal Cell Temperature.
		 *
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: Celsius
		 * </ul>
		 */
		MIN_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Maximum Cell Temperature.
		 *
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: Celsius
		 * <li>Range: (-50)..100
		 * </ul>
		 */
		MAX_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Minimal cell voltage.
		 *
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: mV
		 * </ul>
		 */
		MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Maximum cell voltage.
		 *
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: mV
		 * </ul>
		 */
		MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
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
		return ModbusSlaveNatureTable.of(Battery.class, accessMode, 100) //
				.channel(0, ChannelId.SOC, ModbusType.UINT16) //
				.channel(1, ChannelId.SOH, ModbusType.UINT16) //
				.channel(2, ChannelId.VOLTAGE, ModbusType.FLOAT32) //
				.channel(4, ChannelId.CURRENT, ModbusType.FLOAT32) //
				.channel(6, ChannelId.CAPACITY, ModbusType.FLOAT32) //
				.channel(8, ChannelId.CHARGE_MAX_VOLTAGE, ModbusType.FLOAT32) //
				.channel(10, ChannelId.CHARGE_MAX_CURRENT, ModbusType.FLOAT32) //
				.channel(12, ChannelId.DISCHARGE_MIN_VOLTAGE, ModbusType.FLOAT32) //
				.channel(14, ChannelId.DISCHARGE_MAX_CURRENT, ModbusType.FLOAT32) //
				.channel(16, ChannelId.MIN_CELL_TEMPERATURE, ModbusType.FLOAT32) //
				.channel(18, ChannelId.MAX_CELL_TEMPERATURE, ModbusType.FLOAT32) //
				.channel(20, ChannelId.MIN_CELL_VOLTAGE, ModbusType.FLOAT32) //
				.channel(22, ChannelId.MAX_CELL_VOLTAGE, ModbusType.FLOAT32) //
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
	 * Gets the Channel for {@link ChannelId#SOH}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getSohChannel() {
		return this.channel(ChannelId.SOH);
	}

	/**
	 * Gets the State of Health in [%], range 0..100 %. See {@link ChannelId#SOH}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSoh() {
		return this.getSohChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SOH} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSoh(Integer value) {
		this.getSohChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SOH} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSoh(int value) {
		this.getSohChannel().setNextValue(value);
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
	 * Gets the Channel for {@link ChannelId#DISCHARGE_MIN_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDischargeMinVoltageChannel() {
		return this.channel(ChannelId.DISCHARGE_MIN_VOLTAGE);
	}

	/**
	 * Gets the Discharge Min Voltage in [V]. See
	 * {@link ChannelId#DISCHARGE_MIN_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDischargeMinVoltage() {
		return this.getDischargeMinVoltageChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DISCHARGE_MIN_VOLTAGE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDischargeMinVoltage(Integer value) {
		this.getDischargeMinVoltageChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DISCHARGE_MIN_VOLTAGE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDischargeMinVoltage(int value) {
		this.getDischargeMinVoltageChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DISCHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDischargeMaxCurrentChannel() {
		return this.channel(ChannelId.DISCHARGE_MAX_CURRENT);
	}

	/**
	 * Gets the Discharge Max Current in [A]. See
	 * {@link ChannelId#DISCHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDischargeMaxCurrent() {
		return this.getDischargeMaxCurrentChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DISCHARGE_MAX_CURRENT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDischargeMaxCurrent(Integer value) {
		this.getDischargeMaxCurrentChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DISCHARGE_MAX_CURRENT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDischargeMaxCurrent(int value) {
		this.getDischargeMaxCurrentChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGE_MAX_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getChargeMaxVoltageChannel() {
		return this.channel(ChannelId.CHARGE_MAX_VOLTAGE);
	}

	/**
	 * Gets the Charge Max Voltage in [V]. See {@link ChannelId#CHARGE_MAX_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getChargeMaxVoltage() {
		return this.getChargeMaxVoltageChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CHARGE_MAX_VOLTAGE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChargeMaxVoltage(Integer value) {
		this.getChargeMaxVoltageChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CHARGE_MAX_VOLTAGE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChargeMaxVoltage(int value) {
		this.getChargeMaxVoltageChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getChargeMaxCurrentChannel() {
		return this.channel(ChannelId.CHARGE_MAX_CURRENT);
	}

	/**
	 * Gets the Charge Max Current in [A]. See {@link ChannelId#CHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getChargeMaxCurrent() {
		return this.getChargeMaxCurrentChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CHARGE_MAX_CURRENT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChargeMaxCurrent(Integer value) {
		this.getChargeMaxCurrentChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CHARGE_MAX_CURRENT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChargeMaxCurrent(int value) {
		this.getChargeMaxCurrentChannel().setNextValue(value);
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

	/**
	 * Gets the Channel for {@link ChannelId#VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageChannel() {
		return this.channel(ChannelId.VOLTAGE);
	}

	/**
	 * Gets the Voltage in [V]. See {@link ChannelId#VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltage() {
		return this.getVoltageChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#VOLTAGE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setVoltage(Integer value) {
		this.getVoltageChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#VOLTAGE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setVoltage(int value) {
		this.getVoltageChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentChannel() {
		return this.channel(ChannelId.CURRENT);
	}

	/**
	 * Gets the Current in [mA]. See {@link ChannelId#CURRENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrent() {
		return this.getCurrentChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CURRENT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCurrent(Integer value) {
		this.getCurrentChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CURRENT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCurrent(int value) {
		this.getCurrentChannel().setNextValue(value);
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
	 * Generates a default DebugLog message for {@link Battery} implementations with
	 * a State-Machine.
	 * 
	 * @param battery      the {@link Battery}
	 * @param stateMachine the actual StateMachine (extends
	 *                     {@link AbstractStateMachine})
	 * @return a debug log String
	 */
	public static String generateDebugLog(Battery battery, AbstractStateMachine<?, ?> stateMachine) {
		return new StringBuilder() //
				.append(stateMachine.debugLog()) //
				.append("|SoC:").append(battery.getSoc()) //
				.append("|Actual:").append(battery.getVoltage()) //
				.append(";").append(battery.getCurrent()) //
				.append("|Charge:").append(battery.getChargeMaxVoltage()) //
				.append(";").append(battery.getChargeMaxCurrent()) //
				.append("|Discharge:").append(battery.getDischargeMinVoltage()) //
				.append(";").append(battery.getDischargeMaxCurrent()) //
				.toString();
	}
}
