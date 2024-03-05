package io.openems.edge.meter.api;

import java.util.function.Consumer;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.type.TypeUtils;

/**
 * Represents an electricity Meter.
 * 
 * <p>
 * Meaning of positive and negative values for Power and Current depends on the
 * {@link MeterType} (via {@link #getMeterType()}):
 * <ul>
 * <li>{@link MeterType#GRID}
 * <ul>
 * <li>positive: buy-from-grid
 * <li>negative: feed-to-grid
 * </ul>
 * <li>{@link MeterType#PRODUCTION}
 * <ul>
 * <li>positive: production
 * <li>negative: (undefined)
 * </ul>
 * <li>{@link MeterType#PRODUCTION_AND_CONSUMPTION}
 * <ul>
 * <li>positive: production
 * <li>negative: consumption
 * </ul>
 * <li>{@link MeterType#CONSUMPTION_METERED}
 * <ul>
 * <li>positive: consumption
 * <li>negative: (undefined)
 * </ul>
 * <li>{@link MeterType#CONSUMPTION_NOT_METERED}
 * <ul>
 * <li>positive: consumption
 * <li>negative: (undefined)
 * </ul>
 * </ul>
 * 
 * <p>
 * If values for all phases are equal (i.e. the measured device is 'symmetric'),
 * consider using the helper methods:
 * <ul>
 * <li>{@link #calculateSumActivePowerFromPhases(ElectricityMeter)}
 * <li>{@link #calculateSumReactivePowerFromPhases(ElectricityMeter)}
 * <li>{@link #calculatePhasesFromActivePower(ElectricityMeter)}
 * <li>{@link #calculatePhasesFromReactivePower(ElectricityMeter)}
 * <li>{@link #calculateSumActiveProductionEnergyFromPhases(ElectricityMeter)}
 * <li>{@link #calculateAverageVoltageFromPhases(ElectricityMeter)}
 * <li>{@link #calculateSumCurrentFromPhases(ElectricityMeter)}
 * </ul>
 * 
 * <p>
 * If only ever L1, L2 or L3 can be set, implement the {@link SinglePhaseMeter}
 * Nature additionally and consider using its helper methods.
 */
@ProviderType
public interface ElectricityMeter extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Active Power.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#WATT}
		 * <li>Range: see {@link ElectricityMeter}
		 * </ul>
		 */
		ACTIVE_POWER(new IntegerDoc() //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Active Power L1.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#WATT}
		 * <li>Range: see {@link ElectricityMeter}
		 * </ul>
		 */
		ACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Active Power L2.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#WATT}
		 * <li>Range: see {@link ElectricityMeter}
		 * </ul>
		 */
		ACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Active Power L3.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#WATT}
		 * <li>Range: see {@link ElectricityMeter}
		 * </ul>
		 */
		ACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Reactive Power.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#VOLT_AMPERE_REACTIVE}
		 * </ul>
		 */
		REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Reactive Power L1.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#VOLT_AMPERE_REACTIVE}
		 * </ul>
		 */
		REACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Reactive Power L2.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#VOLT_AMPERE_REACTIVE}
		 * </ul>
		 */
		REACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Reactive Power L3.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#VOLT_AMPERE_REACTIVE}
		 * </ul>
		 */
		REACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Voltage.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#MILLIVOLT}
		 * <li>Range: only positive values
		 * </ul>
		 */
		VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Voltage L1.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#MILLIVOLT}
		 * <li>Range: only positive values
		 * </ul>
		 */
		VOLTAGE_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Voltage L2.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#MILLIVOLT}
		 * <li>Range: only positive values
		 * </ul>
		 */
		VOLTAGE_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Voltage L3.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#MILLIVOLT}
		 * <li>Range: only positive values
		 * </ul>
		 */
		VOLTAGE_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Current.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#MILLIAMPERE}
		 * <li>Range: see {@link ElectricityMeter}
		 * </ul>
		 */
		CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Current L1.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#MILLIAMPERE}
		 * <li>Range: see {@link ElectricityMeter}
		 * </ul>
		 */
		CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Current L2.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#MILLIAMPERE}
		 * <li>Range: see {@link ElectricityMeter}
		 * </ul>
		 */
		CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Current L3.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#MILLIAMPERE}
		 * <li>Range: see {@link ElectricityMeter}
		 * </ul>
		 */
		CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Frequency.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#MILLIHERTZ}
		 * <li>Range: only positive values
		 * </ul>
		 */
		FREQUENCY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIHERTZ) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Active Production Energy.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#LONG}
		 * <li>Unit: {@link Unit#CUMULATED_WATT_HOURS}
		 * <li>Range: only positive values
		 * <li>Source: integral over positive values of {@link #ACTIVE_POWER} (see
		 * {@link ElectricityMeter})
		 * </ul>
		 */
		ACTIVE_PRODUCTION_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * The ActiveProductionEnergy on L1.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#LONG}
		 * <li>Unit: {@link Unit#CUMULATED_WATT_HOURS}
		 * <li>Range: only positive values
		 * <li>Source: integral over positive values of {@link #ACTIVE_POWER_L1} (see
		 * {@link ElectricityMeter})
		 * </ul>
		 */
		ACTIVE_PRODUCTION_ENERGY_L1(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * The ActiveProductionEnergy on L2.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#LONG}
		 * <li>Unit: {@link Unit#CUMULATED_WATT_HOURS}
		 * <li>Range: only positive values
		 * <li>Source: integral over positive values of {@link #ACTIVE_POWER_L2} (see
		 * {@link ElectricityMeter})
		 * </ul>
		 */
		ACTIVE_PRODUCTION_ENERGY_L2(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * The ActiveProductionEnergy on L3.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#LONG}
		 * <li>Unit: {@link Unit#CUMULATED_WATT_HOURS}
		 * <li>Range: only positive values
		 * <li>Source: integral over positive values of {@link #ACTIVE_POWER_L3} (see
		 * {@link ElectricityMeter})
		 * </ul>
		 */
		ACTIVE_PRODUCTION_ENERGY_L3(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Active Consumption Energy.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#LONG}
		 * <li>Unit: {@link Unit#CUMULATED_WATT_HOURS}
		 * <li>Range: only positive values
		 * <li>Source: integral over negative values of {@link #ACTIVE_POWER} (see
		 * {@link ElectricityMeter})
		 * </ul>
		 */
		ACTIVE_CONSUMPTION_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * The ActiveConsumptionEnergy on L1.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#LONG}
		 * <li>Unit: {@link Unit#CUMULATED_WATT_HOURS}
		 * <li>Range: only positive values
		 * <li>Source: integral over negative values of {@link #ACTIVE_POWER_L1} (see
		 * {@link ElectricityMeter})
		 * </ul>
		 */
		ACTIVE_CONSUMPTION_ENERGY_L1(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * The ActiveConsumptionEnergy on L2.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#LONG}
		 * <li>Unit: {@link Unit#CUMULATED_WATT_HOURS}
		 * <li>Range: only positive values
		 * <li>Source: integral over negative values of {@link #ACTIVE_POWER_L2} (see
		 * {@link ElectricityMeter})
		 * </ul>
		 */
		ACTIVE_CONSUMPTION_ENERGY_L2(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * The ActiveConsumptionEnergy on L3.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#LONG}
		 * <li>Unit: {@link Unit#CUMULATED_WATT_HOURS}
		 * <li>Range: only positive values
		 * <li>Source: integral over negative values of {@link #ACTIVE_POWER_L1} (see
		 * {@link ElectricityMeter})
		 * </ul>
		 */
		ACTIVE_CONSUMPTION_ENERGY_L3(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),; //

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
	 * Gets the type of this Meter.
	 *
	 * @return the {@link MeterType}
	 */
	public MeterType getMeterType();

	/**
	 * Is this device actively managed by OpenEMS?.
	 * 
	 * <p>
	 * If this is a normal electricity meter, return false.
	 * 
	 * <p>
	 * If this is an actively managed device like a heat-pump or electric vehicle
	 * charging station, return true. The value will then get ignored for
	 * 'UnmanagedActivePower' prediction.
	 *
	 * @return true for managed, false for unmanaged devices.
	 */
	public default boolean isManaged() {
		return false;
	}

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(ElectricityMeter.class, accessMode, 200) //
				.channel(0, ChannelId.FREQUENCY, ModbusType.FLOAT32) //
				.uint32Reserved(2).uint32Reserved(4) //
				.channel(6, ChannelId.ACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(8, ChannelId.REACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(10, ChannelId.ACTIVE_PRODUCTION_ENERGY, ModbusType.FLOAT32) //
				.channel(12, ChannelId.ACTIVE_CONSUMPTION_ENERGY, ModbusType.FLOAT32) //
				.channel(14, ChannelId.VOLTAGE, ModbusType.FLOAT32) //
				.channel(16, ChannelId.CURRENT, ModbusType.FLOAT32) //
				// Reserved, to stay compatible with previous Symmetric-/AsymmetricMeter
				// implementation;
				// TODO find more elegant solution for this to avoid empty lines in Excel export
				.uint32Reserved(18) //
				.uint32Reserved(20).uint32Reserved(22).uint32Reserved(24).uint32Reserved(26).uint32Reserved(28) //
				.uint32Reserved(30).uint32Reserved(32).uint32Reserved(34).uint32Reserved(36).uint32Reserved(38) //
				.uint32Reserved(40).uint32Reserved(42).uint32Reserved(44).uint32Reserved(46).uint32Reserved(48) //
				.uint32Reserved(50).uint32Reserved(52).uint32Reserved(54).uint32Reserved(56).uint32Reserved(58) //
				.uint32Reserved(60).uint32Reserved(62).uint32Reserved(64).uint32Reserved(66).uint32Reserved(68) //
				.uint32Reserved(70).uint32Reserved(72).uint32Reserved(74).uint32Reserved(76).uint32Reserved(78) //
				.uint32Reserved(80).uint32Reserved(82).uint32Reserved(84).uint32Reserved(86).uint32Reserved(88) //
				.uint32Reserved(90).uint32Reserved(92).uint32Reserved(94).uint32Reserved(96).uint32Reserved(98) //

				.channel(100, ChannelId.ACTIVE_POWER_L1, ModbusType.FLOAT32) //
				.channel(102, ChannelId.ACTIVE_POWER_L2, ModbusType.FLOAT32) //
				.channel(104, ChannelId.ACTIVE_POWER_L3, ModbusType.FLOAT32) //
				.channel(106, ChannelId.REACTIVE_POWER_L1, ModbusType.FLOAT32) //
				.channel(108, ChannelId.REACTIVE_POWER_L2, ModbusType.FLOAT32) //
				.channel(110, ChannelId.REACTIVE_POWER_L3, ModbusType.FLOAT32) //
				.channel(112, ChannelId.VOLTAGE_L1, ModbusType.FLOAT32) //
				.channel(114, ChannelId.VOLTAGE_L2, ModbusType.FLOAT32) //
				.channel(116, ChannelId.VOLTAGE_L3, ModbusType.FLOAT32) //
				.channel(118, ChannelId.CURRENT_L1, ModbusType.FLOAT32) //
				.channel(120, ChannelId.CURRENT_L2, ModbusType.FLOAT32) //
				.channel(122, ChannelId.CURRENT_L3, ModbusType.FLOAT32) //
				.channel(124, ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, ModbusType.FLOAT32) //
				.channel(126, ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, ModbusType.FLOAT32) //
				.channel(128, ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, ModbusType.FLOAT32) //
				.channel(130, ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, ModbusType.FLOAT32) //
				.channel(132, ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, ModbusType.FLOAT32) //
				.channel(134, ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, ModbusType.FLOAT32) //
				.build();
	}

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component - without individual phases.
	 * 
	 * <p>
	 * This method provides a way to stay compatible with previous SymmetricMeter
	 * implementations that did not support AsymmetricMeter. Do not use for new
	 * implementations!
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTableWithoutIndividualPhases(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(ElectricityMeter.class, accessMode, 200) //
				.channel(0, ChannelId.FREQUENCY, ModbusType.FLOAT32) //
				.uint32Reserved(2).uint32Reserved(4) //
				.channel(6, ChannelId.ACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(8, ChannelId.REACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(10, ChannelId.ACTIVE_PRODUCTION_ENERGY, ModbusType.FLOAT32) //
				.channel(12, ChannelId.ACTIVE_CONSUMPTION_ENERGY, ModbusType.FLOAT32) //
				.channel(14, ChannelId.VOLTAGE, ModbusType.FLOAT32) //
				.channel(16, ChannelId.CURRENT, ModbusType.FLOAT32) //
				.build();
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
	 * Gets the Active Power in [W]. Negative values for Consumption (power that is
	 * 'leaving the system', e.g. feed-to-grid); positive for Production (power that
	 * is 'entering the system'). See {@link ChannelId#ACTIVE_POWER}.
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
	 * Gets the Channel for {@link ChannelId#VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageChannel() {
		return this.channel(ChannelId.VOLTAGE);
	}

	/**
	 * Gets the Voltage in [mV]. See {@link ChannelId#VOLTAGE}.
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
	 * Gets the Channel for {@link ChannelId#FREQUENCY}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getFrequencyChannel() {
		return this.channel(ChannelId.FREQUENCY);
	}

	/**
	 * Gets the Frequency in [mHz]. See {@link ChannelId#FREQUENCY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getFrequency() {
		return this.getFrequencyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#FREQUENCY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setFrequency(Integer value) {
		this.getFrequencyChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#FREQUENCY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setFrequency(int value) {
		this.getFrequencyChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_PRODUCTION_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getActiveProductionEnergyChannel() {
		return this.channel(ChannelId.ACTIVE_PRODUCTION_ENERGY);
	}

	/**
	 * Gets the Active Production Energy in [Wh]. This relates to positive
	 * ACTIVE_POWER. See {@link ChannelId#ACTIVE_PRODUCTION_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getActiveProductionEnergy() {
		return this.getActiveProductionEnergyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_PRODUCTION_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActiveProductionEnergy(Long value) {
		this.getActiveProductionEnergyChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_PRODUCTION_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActiveProductionEnergy(long value) {
		this.getActiveProductionEnergyChannel().setNextValue(value);
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
	 * Gets the Active Production Energy on L1 in [Wh]. This relates to positive
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
	 * Gets the Active Production Energy on L2 in [Wh]. This relates to positive
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
	 * Gets the Active Production Energy on L3 in [Wh]. This relates to positive
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
	 * Gets the Channel for {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getActiveConsumptionEnergyChannel() {
		return this.channel(ChannelId.ACTIVE_CONSUMPTION_ENERGY);
	}

	/**
	 * Gets the Active Consumption Energy in [Wh]. This relates to negative
	 * ACTIVE_POWER. See {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getActiveConsumptionEnergy() {
		return this.getActiveConsumptionEnergyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActiveConsumptionEnergy(Long value) {
		this.getActiveConsumptionEnergyChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActiveConsumptionEnergy(long value) {
		this.getActiveConsumptionEnergyChannel().setNextValue(value);
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
	 * Gets the Active Consumption Energy on L1 in [Wh]. This relates to negative
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
	 * Gets the Active Consumption Energy on L2 in [Wh]. This relates to negative
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
	 * Gets the Active Consumption Energy on L3 in [Wh]. This relates to negative
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
	 * Initializes Channel listeners to calculate the
	 * {@link ChannelId#ACTIVE_POWER}-Channel value as the sum of
	 * {@link ChannelId#ACTIVE_POWER_L1}, {@link ChannelId#ACTIVE_POWER_L2} and
	 * {@link ChannelId#ACTIVE_POWER_L3}.
	 *
	 * @param meter the {@link ElectricityMeter}
	 */
	public static void calculateSumActivePowerFromPhases(ElectricityMeter meter) {
		final Consumer<Value<Integer>> calculate = ignore -> {
			meter._setActivePower(TypeUtils.sum(//
					meter.getActivePowerL1Channel().getNextValue().get(), //
					meter.getActivePowerL2Channel().getNextValue().get(), //
					meter.getActivePowerL3Channel().getNextValue().get())); //
		};
		meter.getActivePowerL1Channel().onSetNextValue(calculate);
		meter.getActivePowerL2Channel().onSetNextValue(calculate);
		meter.getActivePowerL3Channel().onSetNextValue(calculate);
	}

	/**
	 * Initializes Channel listeners to calculate the
	 * {@link ChannelId#REACTIVE_POWER}-Channel value as the sum of
	 * {@link ChannelId#REACTIVE_POWER_L1}, {@link ChannelId#REACTIVE_POWER_L2} and
	 * {@link ChannelId#REACTIVE_POWER_L3}.
	 *
	 * @param meter the {@link ElectricityMeter}
	 */
	public static void calculateSumReactivePowerFromPhases(ElectricityMeter meter) {
		final Consumer<Value<Integer>> calculate = ignore -> {
			meter._setReactivePower(TypeUtils.sum(//
					meter.getReactivePowerL1Channel().getNextValue().get(), //
					meter.getReactivePowerL2Channel().getNextValue().get(), //
					meter.getReactivePowerL3Channel().getNextValue().get())); //
		};
		meter.getReactivePowerL1Channel().onSetNextValue(calculate);
		meter.getReactivePowerL2Channel().onSetNextValue(calculate);
		meter.getReactivePowerL3Channel().onSetNextValue(calculate);
	}

	/**
	 * Initializes Channel listeners to calculate the
	 * {@link ChannelId#CURRENT}-Channel value as the sum of
	 * {@link ChannelId#CURRENT_L1}, {@link ChannelId#CURRENT_L2} and
	 * {@link ChannelId#CURRENT_L3}.
	 *
	 * @param meter the {@link ElectricityMeter}
	 */
	public static void calculateSumCurrentFromPhases(ElectricityMeter meter) {
		final Consumer<Value<Integer>> calculate = ignore -> {
			meter._setCurrent(TypeUtils.sum(//
					meter.getCurrentL1Channel().getNextValue().get(), //
					meter.getCurrentL2Channel().getNextValue().get(), //
					meter.getCurrentL3Channel().getNextValue().get())); //
		};
		meter.getCurrentL1Channel().onSetNextValue(calculate);
		meter.getCurrentL2Channel().onSetNextValue(calculate);
		meter.getCurrentL3Channel().onSetNextValue(calculate);
	}

	/**
	 * Initializes Channel listeners to calculate the
	 * {@link ChannelId#VOLTAGE}-Channel value as the average of
	 * {@link ChannelId#VOLTAGE_L1}, {@link ChannelId#VOLTAGE_L2} and
	 * {@link ChannelId#VOLTAGE_L3}.
	 *
	 * @param meter the {@link ElectricityMeter}
	 */
	public static void calculateAverageVoltageFromPhases(ElectricityMeter meter) {
		final Consumer<Value<Integer>> calculateAverageVoltage = ignore -> {
			meter._setVoltage(TypeUtils.averageRounded(//
					meter.getVoltageL1Channel().getNextValue().get(), //
					meter.getVoltageL2Channel().getNextValue().get(), //
					meter.getVoltageL3Channel().getNextValue().get() //
			));
		};
		meter.getVoltageL1Channel().onSetNextValue(calculateAverageVoltage);
		meter.getVoltageL2Channel().onSetNextValue(calculateAverageVoltage);
		meter.getVoltageL3Channel().onSetNextValue(calculateAverageVoltage);
	}

	/**
	 * Initializes Channel listeners to calculate the
	 * {@link ChannelId#ACTIVE_PRODUCTION_ENERGY}-Channel value as the sum of
	 * {@link ChannelId#ACTIVE_PRODUCTION_ENERGY_L1},
	 * {@link ChannelId#ACTIVE_PRODUCTION_ENERGY_L2} and
	 * {@link ChannelId#ACTIVE_PRODUCTION_ENERGY_L3}.
	 *
	 * @param meter the {@link ElectricityMeter}
	 */
	public static void calculateSumActiveProductionEnergyFromPhases(ElectricityMeter meter) {
		final Consumer<Value<Long>> calculate = ignore -> {
			meter._setActiveProductionEnergy(TypeUtils.sum(//
					meter.getActiveProductionEnergyL1Channel().getNextValue().get(), //
					meter.getActiveProductionEnergyL2Channel().getNextValue().get(), //
					meter.getActiveProductionEnergyL3Channel().getNextValue().get())); //
		};
		meter.getActiveProductionEnergyL1Channel().onSetNextValue(calculate);
		meter.getActiveProductionEnergyL2Channel().onSetNextValue(calculate);
		meter.getActiveProductionEnergyL3Channel().onSetNextValue(calculate);
	}

	/**
	 * Initializes Channel listeners for a Symmetric {@link ElectricityMeter}.
	 * 
	 * <p>
	 * Calculate the {@link ChannelId#ACTIVE_POWER_L1},
	 * {@link ChannelId#ACTIVE_POWER_L2} and
	 * {@link ChannelId#ACTIVE_POWER_L3}-Channels from
	 * {@link ChannelId#ACTIVE_POWER} by dividing by three.
	 *
	 * @param meter the {@link ElectricityMeter}
	 */
	public static void calculatePhasesFromActivePower(ElectricityMeter meter) {
		meter.getActivePowerChannel().onSetNextValue(value -> {
			var phase = TypeUtils.divide(value.get(), 3);
			meter.getActivePowerL1Channel().setNextValue(phase);
			meter.getActivePowerL2Channel().setNextValue(phase);
			meter.getActivePowerL3Channel().setNextValue(phase);
		});
	}

	/**
	 * Initializes Channel listeners for a Symmetric {@link ElectricityMeter}.
	 * 
	 * <p>
	 * Calculate the {@link ChannelId#REACTIVE_POWER_L1},
	 * {@link ChannelId#REACTIVE_POWER_L2} and
	 * {@link ChannelId#REACTIVE_POWER_L3}-Channels from
	 * {@link ChannelId#REACTIVE_POWER} by dividing by three.
	 *
	 * @param meter the {@link ElectricityMeter}
	 */
	public static void calculatePhasesFromReactivePower(ElectricityMeter meter) {
		meter.getReactivePowerChannel().onSetNextValue(value -> {
			var phase = TypeUtils.divide(value.get(), 3);
			meter.getReactivePowerL1Channel().setNextValue(phase);
			meter.getReactivePowerL2Channel().setNextValue(phase);
			meter.getReactivePowerL3Channel().setNextValue(phase);
		});
	}

	/**
	 * Initializes Channel listeners to calculate the
	 * {@link ChannelId#ACTIVE_POWER}-Channel value as the product of
	 * {@link ChannelId#CURRENT} and {@link ChannelId#VOLTAGE} for each phase.
	 *
	 * @param meter the {@link ElectricityMeter}
	 */
	public static void calculatePhasesWithVoltAndAmpere(ElectricityMeter meter) {
		// For Phase L1
		final Consumer<Value<Integer>> calculateActivePowerL1 = ignore -> {
			Integer currentL1 = meter.getCurrentL1Channel().getNextValue().get();
			Integer voltageL1 = meter.getVoltageL1Channel().getNextValue().get();
			if (currentL1 != null && voltageL1 != null) {
				meter._setActivePowerL1((int) ((double) currentL1 * voltageL1 / 1_000_000)); // Convert from uW to W
			}
		};
		meter.getCurrentL1Channel().onSetNextValue(calculateActivePowerL1);
		meter.getVoltageL1Channel().onSetNextValue(calculateActivePowerL1);

		// For Phase L2
		final Consumer<Value<Integer>> calculateActivePowerL2 = ignore -> {
			Integer currentL2 = meter.getCurrentL2Channel().getNextValue().get();
			Integer voltageL2 = meter.getVoltageL2Channel().getNextValue().get();
			if (currentL2 != null && voltageL2 != null) {
				meter._setActivePowerL2((int) ((double) currentL2 * voltageL2 / 1_000_000)); // Convert from uW to W
			}
		};
		meter.getCurrentL2Channel().onSetNextValue(calculateActivePowerL2);
		meter.getVoltageL2Channel().onSetNextValue(calculateActivePowerL2);

		// For Phase L3
		final Consumer<Value<Integer>> calculateActivePowerL3 = ignore -> {
			Integer currentL3 = meter.getCurrentL3Channel().getNextValue().get();
			Integer voltageL3 = meter.getVoltageL3Channel().getNextValue().get();
			if (currentL3 != null && voltageL3 != null) {
				meter._setActivePowerL3((int) ((double) currentL3 * voltageL3 / 1_000_000)); // Convert from uW to W
			}
		};
		meter.getCurrentL3Channel().onSetNextValue(calculateActivePowerL3);
		meter.getVoltageL3Channel().onSetNextValue(calculateActivePowerL3);
	}

}
