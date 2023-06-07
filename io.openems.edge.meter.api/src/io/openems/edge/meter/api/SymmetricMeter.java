package io.openems.edge.meter.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.IntUtils;
import io.openems.common.utils.IntUtils.Round;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;

/**
 * Represents a Symmetric Meter.
 *
 * <p>
 * <ul>
 * <li>Negative ActivePower and ConsumptionActivePower represent Consumption,
 * i.e. power that is 'leaving the system', e.g. feed-to-grid
 * <li>Positive ActivePower and ProductionActivePower represent Production, i.e.
 * power that is 'entering the system', e.g. buy-from-grid
 * </ul>
 */
public interface SymmetricMeter extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Frequency.
		 *
		 * <ul>
		 * <li>Interface: Meter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: mHz
		 * <li>Range: only positive values
		 * </ul>
		 */
		FREQUENCY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIHERTZ) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Minimum Ever Active Power.
		 *
		 * <ul>
		 * <li>Interface: Meter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative or '0'
		 * <li>Implementation Note: value is automatically derived from ACTIVE_POWER
		 * </ul>
		 */
		MIN_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Maximum Ever Active Power.
		 *
		 * <ul>
		 * <li>Interface: Meter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive or '0'
		 * <li>Implementation Note: value is automatically derived from ACTIVE_POWER
		 * </ul>
		 */
		MAX_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Active Power.
		 *
		 * <ul>
		 * <li>Interface: Meter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		ACTIVE_POWER(new IntegerDoc() //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.onChannelSetNextValue((self, value) -> {
					/*
					 * Fill Min/Max Active Power channels
					 */
					value.ifPresent(newValue -> {
						{
							Channel<Integer> minActivePowerChannel = self.channel(ChannelId.MIN_ACTIVE_POWER);
							int minActivePower = minActivePowerChannel.value().orElse(0);
							int minNextActivePower = minActivePowerChannel.getNextValue().orElse(0);
							if (newValue < Math.min(minActivePower, minNextActivePower)) {
								// avoid getting called too often -> round to 100
								newValue = IntUtils.roundToPrecision(newValue, Round.TOWARDS_ZERO, 100);
								minActivePowerChannel.setNextValue(newValue);
							}
						}
						{
							Channel<Integer> maxActivePowerChannel = self.channel(ChannelId.MAX_ACTIVE_POWER);
							int maxActivePower = maxActivePowerChannel.value().orElse(0);
							int maxNextActivePower = maxActivePowerChannel.getNextValue().orElse(0);
							if (newValue > Math.max(maxActivePower, maxNextActivePower)) {
								// avoid getting called too often -> round to 100
								newValue = IntUtils.roundToPrecision(newValue, Round.AWAY_FROM_ZERO, 100);
								maxActivePowerChannel.setNextValue(newValue);
							}
						}
					});
				})), //

		/**
		 * Reactive Power.
		 *
		 * <ul>
		 * <li>Interface: Meter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Active Production Energy.
		 *
		 * <ul>
		 * <li>Interface: Meter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		ACTIVE_PRODUCTION_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Active Consumption Energy.
		 *
		 * <ul>
		 * <li>Interface: Meter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		ACTIVE_CONSUMPTION_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Voltage.
		 *
		 * <ul>
		 * <li>Interface: Meter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: mV
		 * </ul>
		 */
		VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Current.
		 *
		 * <ul>
		 * <li>Interface: Meter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
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
	 * Gets the type of this Meter.
	 *
	 * @return the MeterType
	 */
	public MeterType getMeterType();

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(SymmetricMeter.class, accessMode, 100) //
				.channel(0, ChannelId.FREQUENCY, ModbusType.FLOAT32) //
				.channel(2, ChannelId.MIN_ACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(4, ChannelId.MAX_ACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(6, ChannelId.ACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(8, ChannelId.REACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(10, ChannelId.ACTIVE_PRODUCTION_ENERGY, ModbusType.FLOAT32) //
				.channel(12, ChannelId.ACTIVE_CONSUMPTION_ENERGY, ModbusType.FLOAT32) //
				.channel(14, ChannelId.VOLTAGE, ModbusType.FLOAT32) //
				.channel(16, ChannelId.CURRENT, ModbusType.FLOAT32) //
				.build();
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
	 * Gets the Channel for {@link ChannelId#MIN_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMinActivePowerChannel() {
		return this.channel(ChannelId.MIN_ACTIVE_POWER);
	}

	/**
	 * Gets the Minimum Ever Active Power in [W]. See
	 * {@link ChannelId#MIN_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMinActivePower() {
		return this.getMinActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MIN_ACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMinActivePower(Integer value) {
		this.getMinActivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MIN_ACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMinActivePower(int value) {
		this.getMinActivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxActivePowerChannel() {
		return this.channel(ChannelId.MAX_ACTIVE_POWER);
	}

	/**
	 * Gets the Maximum Ever Active Power in [W]. See
	 * {@link ChannelId#MAX_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxActivePower() {
		return this.getMaxActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_ACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxActivePower(Integer value) {
		this.getMaxActivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_ACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxActivePower(int value) {
		this.getMaxActivePowerChannel().setNextValue(value);
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
	 * Gets the Channel for {@link ChannelId#ACTIVE_PRODUCTION_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getActiveProductionEnergyChannel() {
		return this.channel(ChannelId.ACTIVE_PRODUCTION_ENERGY);
	}

	/**
	 * Gets the Active Production Energy in [Wh_Σ]. This relates to positive
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
	 * Gets the Channel for {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getActiveConsumptionEnergyChannel() {
		return this.channel(ChannelId.ACTIVE_CONSUMPTION_ENERGY);
	}

	/**
	 * Gets the Active Consumption Energy in [Wh_Σ]. This relates to negative
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
}
