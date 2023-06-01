package io.openems.edge.meter.api;

import java.util.function.Consumer;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.type.TypeUtils;

/**
 * Represents an Asymmetric Meter.
 *
 * <ul>
 * <li>Negative ActivePowerL1/L2/L3 and ConsumptionActivePowerL1/L2/L3 represent
 * Consumption, i.e. power that is 'leaving the system', e.g. feed-to-grid
 * <li>Positive ActivePowerL1/L2/L3 and ProductionActivePowerL1/L2/L3 represent
 * Production, i.e. power that is 'entering the system', e.g. buy-from-grid
 * </ul>
 */
public interface AsymmetricMeter extends SymmetricMeter {

	public static final String POWER_DOC_TEXT = "Negative values for Consumption; positive for Production";

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Active Power L1.
		 *
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		ACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text(POWER_DOC_TEXT)), //
		/**
		 * Active Power L2.
		 *
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		ACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text(POWER_DOC_TEXT)), //
		/**
		 * Active Power L3.
		 *
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		ACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text(POWER_DOC_TEXT)), //
		/**
		 * Reactive Power L1.
		 *
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		REACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text(POWER_DOC_TEXT)), //
		/**
		 * Reactive Power L2.
		 *
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		REACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text(POWER_DOC_TEXT)), //
		/**
		 * Reactive Power L3.
		 *
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		REACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text(POWER_DOC_TEXT)), //
		/**
		 * Voltage L1.
		 *
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: mV
		 * </ul>
		 */
		VOLTAGE_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Voltage L2.
		 *
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: mV
		 * </ul>
		 */
		VOLTAGE_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Voltage L3.
		 *
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: mV
		 * </ul>
		 */
		VOLTAGE_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Current L1.
		 *
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Current L2.
		 *
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Current L3.
		 *
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * The ActiveProductionEnergy on L1.
		 *
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Long
		 * <li>Unit: {@link Unit#WATT_HOURS}
		 * </ul>
		 */
		ACTIVE_PRODUCTION_ENERGY_L1(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * The ActiveProductionEnergy on L2.
		 *
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Long
		 * <li>Unit: {@link Unit#WATT_HOURS}
		 * </ul>
		 */
		ACTIVE_PRODUCTION_ENERGY_L2(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * The ActiveProductionEnergy on L3.
		 *
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Long
		 * <li>Unit: {@link Unit#WATT_HOURS}
		 * </ul>
		 */
		ACTIVE_PRODUCTION_ENERGY_L3(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * The ActiveConsumptionEnergy on L1.
		 *
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Long
		 * <li>Unit: {@link Unit#WATT_HOURS}
		 * </ul>
		 */
		ACTIVE_CONSUMPTION_ENERGY_L1(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * The ActiveConsumptionEnergy on L2.
		 *
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Long
		 * <li>Unit: {@link Unit#WATT_HOURS}
		 * </ul>
		 */
		ACTIVE_CONSUMPTION_ENERGY_L2(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * The ActiveConsumptionEnergy on L3.
		 *
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Long
		 * <li>Unit: {@link Unit#WATT_HOURS}
		 * </ul>
		 */
		ACTIVE_CONSUMPTION_ENERGY_L3(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		; //

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
		return ModbusSlaveNatureTable.of(AsymmetricMeter.class, accessMode, 100) //
				.channel(0, ChannelId.ACTIVE_POWER_L1, ModbusType.FLOAT32) //
				.channel(2, ChannelId.ACTIVE_POWER_L2, ModbusType.FLOAT32) //
				.channel(4, ChannelId.ACTIVE_POWER_L3, ModbusType.FLOAT32) //
				.channel(6, ChannelId.REACTIVE_POWER_L1, ModbusType.FLOAT32) //
				.channel(8, ChannelId.REACTIVE_POWER_L2, ModbusType.FLOAT32) //
				.channel(10, ChannelId.REACTIVE_POWER_L3, ModbusType.FLOAT32) //
				.channel(12, ChannelId.VOLTAGE_L1, ModbusType.FLOAT32) //
				.channel(14, ChannelId.VOLTAGE_L2, ModbusType.FLOAT32) //
				.channel(16, ChannelId.VOLTAGE_L3, ModbusType.FLOAT32) //
				.channel(18, ChannelId.CURRENT_L1, ModbusType.FLOAT32) //
				.channel(20, ChannelId.CURRENT_L2, ModbusType.FLOAT32) //
				.channel(22, ChannelId.CURRENT_L3, ModbusType.FLOAT32) //
				.channel(24, ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, ModbusType.FLOAT32) //
				.channel(26, ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, ModbusType.FLOAT32) //
				.channel(28, ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, ModbusType.FLOAT32) //
				.channel(30, ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, ModbusType.FLOAT32) //
				.channel(32, ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, ModbusType.FLOAT32) //
				.channel(34, ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, ModbusType.FLOAT32) //
				.build();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getActivePowerL1Channel() {
		return this.channel(ChannelId.ACTIVE_POWER_L1);
	}

	/**
	 * Gets the Active Power on L1 in [W]. Negative values for Consumption (power
	 * that is 'leaving the system', e.g. feed-to-grid); positive for Production
	 * (power that is 'entering the system'). See {@link ChannelId#ACTIVE_POWER_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActivePowerL1() {
		return this.getActivePowerL1Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ACTIVE_POWER_L1}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActivePowerL1(Integer value) {
		this.getActivePowerL1Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ACTIVE_POWER_L1}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActivePowerL1(int value) {
		this.getActivePowerL1Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getActivePowerL2Channel() {
		return this.channel(ChannelId.ACTIVE_POWER_L2);
	}

	/**
	 * Gets the Active Power on L2 in [W]. Negative values for Consumption (power
	 * that is 'leaving the system', e.g. feed-to-grid); positive for Production
	 * (power that is 'entering the system'). See {@link ChannelId#ACTIVE_POWER_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActivePowerL2() {
		return this.getActivePowerL2Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ACTIVE_POWER_L2}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActivePowerL2(Integer value) {
		this.getActivePowerL2Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ACTIVE_POWER_L2}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActivePowerL2(int value) {
		this.getActivePowerL2Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getActivePowerL3Channel() {
		return this.channel(ChannelId.ACTIVE_POWER_L3);
	}

	/**
	 * Gets the Active Power on L3 in [W]. Negative values for Consumption (power
	 * that is 'leaving the system', e.g. feed-to-grid); positive for Production
	 * (power that is 'entering the system'). See {@link ChannelId#ACTIVE_POWER_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActivePowerL3() {
		return this.getActivePowerL3Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ACTIVE_POWER_L3}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActivePowerL3(Integer value) {
		this.getActivePowerL3Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ACTIVE_POWER_L3}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActivePowerL3(int value) {
		this.getActivePowerL3Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#REACTIVE_POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getReactivePowerL1Channel() {
		return this.channel(ChannelId.REACTIVE_POWER_L1);
	}

	/**
	 * Gets the Reactive Power on L1 in [var]. See
	 * {@link ChannelId#REACTIVE_POWER_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getReactivePowerL1() {
		return this.getReactivePowerL1Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#REACTIVE_POWER_L1}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setReactivePowerL1(Integer value) {
		this.getReactivePowerL1Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#REACTIVE_POWER_L1}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setReactivePowerL1(int value) {
		this.getReactivePowerL1Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#REACTIVE_POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getReactivePowerL2Channel() {
		return this.channel(ChannelId.REACTIVE_POWER_L2);
	}

	/**
	 * Gets the Reactive Power on L2 in [var]. See
	 * {@link ChannelId#REACTIVE_POWER_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getReactivePowerL2() {
		return this.getReactivePowerL2Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#REACTIVE_POWER_L2}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setReactivePowerL2(Integer value) {
		this.getReactivePowerL2Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#REACTIVE_POWER_L2}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setReactivePowerL2(int value) {
		this.getReactivePowerL2Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#REACTIVE_POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getReactivePowerL3Channel() {
		return this.channel(ChannelId.REACTIVE_POWER_L3);
	}

	/**
	 * Gets the Reactive Power on L3 in [var]. See
	 * {@link ChannelId#REACTIVE_POWER_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getReactivePowerL3() {
		return this.getReactivePowerL3Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#REACTIVE_POWER_L3}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setReactivePowerL3(Integer value) {
		this.getReactivePowerL3Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#REACTIVE_POWER_L3}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setReactivePowerL3(int value) {
		this.getReactivePowerL3Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#VOLTAGE_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageL1Channel() {
		return this.channel(ChannelId.VOLTAGE_L1);
	}

	/**
	 * Gets the Voltage on L1 in [mV]. See {@link ChannelId#VOLTAGE_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltageL1() {
		return this.getVoltageL1Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#VOLTAGE_L1}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setVoltageL1(Integer value) {
		this.getVoltageL1Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#VOLTAGE_L1}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setVoltageL1(int value) {
		this.getVoltageL1Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#VOLTAGE_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageL2Channel() {
		return this.channel(ChannelId.VOLTAGE_L2);
	}

	/**
	 * Gets the Voltage on L2 in [mV]. See {@link ChannelId#VOLTAGE_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltageL2() {
		return this.getVoltageL2Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#VOLTAGE_L2}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setVoltageL2(Integer value) {
		this.getVoltageL2Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#VOLTAGE_L2}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setVoltageL2(int value) {
		this.getVoltageL2Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#VOLTAGE_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageL3Channel() {
		return this.channel(ChannelId.VOLTAGE_L3);
	}

	/**
	 * Gets the Voltage on L3 in [mV]. See {@link ChannelId#VOLTAGE_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltageL3() {
		return this.getVoltageL3Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#VOLTAGE_L3}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setVoltageL3(Integer value) {
		this.getVoltageL3Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#VOLTAGE_L3}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setVoltageL3(int value) {
		this.getVoltageL3Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentL1Channel() {
		return this.channel(ChannelId.CURRENT_L1);
	}

	/**
	 * Gets the Current on L1 in [mA]. See {@link ChannelId#CURRENT_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentL1() {
		return this.getCurrentL1Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CURRENT_L1}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCurrentL1(Integer value) {
		this.getCurrentL1Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CURRENT_L1}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCurrentL1(int value) {
		this.getCurrentL1Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentL2Channel() {
		return this.channel(ChannelId.CURRENT_L2);
	}

	/**
	 * Gets the Current on L2 in [mA]. See {@link ChannelId#CURRENT_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentL2() {
		return this.getCurrentL2Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CURRENT_L2}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCurrentL2(Integer value) {
		this.getCurrentL2Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CURRENT_L2}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCurrentL2(int value) {
		this.getCurrentL2Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentL3Channel() {
		return this.channel(ChannelId.CURRENT_L3);
	}

	/**
	 * Gets the Current on L3 in [mA]. See {@link ChannelId#CURRENT_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentL3() {
		return this.getCurrentL3Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CURRENT_L3}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCurrentL3(Integer value) {
		this.getCurrentL3Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CURRENT_L3}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCurrentL3(int value) {
		this.getCurrentL3Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_PRODUCTION_ENERGY_L1}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getActiveProductionEnergyL1Channel() {
		return this.channel(ChannelId.ACTIVE_PRODUCTION_ENERGY_L1);
	}

	/**
	 * Gets the Active Production Energy on L1 in [Wh_Σ]. This relates to positive
	 * ACTIVE_POWER_L1. See {@link ChannelId#ACTIVE_PRODUCTION_ENERGY_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getActiveProductionEnergyL1() {
		return this.getActiveProductionEnergyL1Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_PRODUCTION_ENERGY_L1} Channel.
	 *
	 * @param value the next value in {@link Long}
	 */
	public default void _setActiveProductionEnergyL1(Long value) {
		this.getActiveProductionEnergyL1Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_PRODUCTION_ENERGY_L1} Channel.
	 *
	 * @param value the next value in {@link long}
	 */
	public default void _setActiveProductionEnergyL1(long value) {
		this.getActiveProductionEnergyL1Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_PRODUCTION_ENERGY_L2}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getActiveProductionEnergyL2Channel() {
		return this.channel(ChannelId.ACTIVE_PRODUCTION_ENERGY_L2);
	}

	/**
	 * Gets the Active Production Energy on L2 in [Wh_Σ]. This relates to positive
	 * ACTIVE_POWER_L2. See {@link ChannelId#ACTIVE_PRODUCTION_ENERGY_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getActiveProductionEnergyL2() {
		return this.getActiveProductionEnergyL2Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_PRODUCTION_ENERGY_L2} Channel.
	 *
	 * @param value the next value in {@link Long}
	 */
	public default void _setActiveProductionEnergyL2(Long value) {
		this.getActiveProductionEnergyL2Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_PRODUCTION_ENERGY_L2} Channel.
	 *
	 * @param value the next value in {@link long}
	 */
	public default void _setActiveProductionEnergyL2(long value) {
		this.getActiveProductionEnergyL2Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_PRODUCTION_ENERGY_L3}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getActiveProductionEnergyL3Channel() {
		return this.channel(ChannelId.ACTIVE_PRODUCTION_ENERGY_L3);
	}

	/**
	 * Gets the Active Production Energy on L3 in [Wh_Σ]. This relates to positive
	 * ACTIVE_POWER_L3. See {@link ChannelId#ACTIVE_PRODUCTION_ENERGY_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getActiveProductionEnergyL3() {
		return this.getActiveProductionEnergyL3Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_PRODUCTION_ENERGY_L3} Channel.
	 *
	 * @param value the next value in {@link Long}
	 */
	public default void _setActiveProductionEnergyL3(Long value) {
		this.getActiveProductionEnergyL3Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_PRODUCTION_ENERGY_L3} Channel.
	 *
	 * @param value the next value in {@link long}
	 */
	public default void _setActiveProductionEnergyL3(long value) {
		this.getActiveProductionEnergyL3Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY_L1}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getActiveConsumptionEnergyL1Channel() {
		return this.channel(ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1);
	}

	/**
	 * Gets the Active Consumption Energy on L1 in [Wh_Σ]. This relates to negative
	 * ACTIVE_POWER_L1. See {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getActiveConsumptionEnergyL1() {
		return this.getActiveConsumptionEnergyL1Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY_L1} Channel.
	 *
	 * @param value the next value in {@link Long}
	 */
	public default void _setActiveConsumptionEnergyL1(Long value) {
		this.getActiveConsumptionEnergyL1Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY_L1} Channel.
	 *
	 * @param value the next value in {@link long}
	 */
	public default void _setActiveConsumptionEnergyL1(long value) {
		this.getActiveConsumptionEnergyL1Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY_L2}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getActiveConsumptionEnergyL2Channel() {
		return this.channel(ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2);
	}

	/**
	 * Gets the Active Consumption Energy on L2 in [Wh_Σ]. This relates to negative
	 * ACTIVE_POWER_L2. See {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getActiveConsumptionEnergyL2() {
		return this.getActiveConsumptionEnergyL2Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY_L2} Channel.
	 *
	 * @param value the next value in {@link Long}
	 */
	public default void _setActiveConsumptionEnergyL2(Long value) {
		this.getActiveConsumptionEnergyL2Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY_L2} Channel.
	 *
	 * @param value the next value in {@link long}
	 */
	public default void _setActiveConsumptionEnergyL2(long value) {
		this.getActiveConsumptionEnergyL2Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY_L3}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getActiveConsumptionEnergyL3Channel() {
		return this.channel(ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3);
	}

	/**
	 * Gets the Active Consumption Energy on L3 in [Wh_Σ]. This relates to negative
	 * ACTIVE_POWER_L3. See {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getActiveConsumptionEnergyL3() {
		return this.getActiveConsumptionEnergyL3Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY_L3} Channel.
	 *
	 * @param value the next value in {@link Long}
	 */
	public default void _setActiveConsumptionEnergyL3(Long value) {
		this.getActiveConsumptionEnergyL3Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY_L3} Channel.
	 *
	 * @param value the next value in {@link long}
	 */
	public default void _setActiveConsumptionEnergyL3(long value) {
		this.getActiveConsumptionEnergyL3Channel().setNextValue(value);
	}

	/**
	 * Initializes Channel listeners to set the Active- and Reactive-Power Channel
	 * value as the sum of L1 + L2 + L3.
	 *
	 * @param meter the {@link AsymmetricMeter}
	 */
	public static void initializePowerSumChannels(AsymmetricMeter meter) {
		// Active Power
		final Consumer<Value<Integer>> activePowerSum = ignore -> {
			meter._setActivePower(TypeUtils.sum(//
					meter.getActivePowerL1Channel().getNextValue().get(), //
					meter.getActivePowerL2Channel().getNextValue().get(), //
					meter.getActivePowerL3Channel().getNextValue().get())); //
		};
		meter.getActivePowerL1Channel().onSetNextValue(activePowerSum);
		meter.getActivePowerL2Channel().onSetNextValue(activePowerSum);
		meter.getActivePowerL3Channel().onSetNextValue(activePowerSum);

		// Reactive Power
		final Consumer<Value<Integer>> reactivePowerSum = ignore -> {
			meter._setReactivePower(TypeUtils.sum(//
					meter.getReactivePowerL1Channel().getNextValue().get(), //
					meter.getReactivePowerL2Channel().getNextValue().get(), //
					meter.getReactivePowerL3Channel().getNextValue().get())); //
		};
		meter.getReactivePowerL1Channel().onSetNextValue(reactivePowerSum);
		meter.getReactivePowerL2Channel().onSetNextValue(reactivePowerSum);
		meter.getReactivePowerL3Channel().onSetNextValue(reactivePowerSum);
	}
}
