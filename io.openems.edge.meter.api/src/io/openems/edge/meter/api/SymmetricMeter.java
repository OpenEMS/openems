package io.openems.edge.meter.api;

import io.openems.common.OpenemsConstants;
import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.IntUtils;
import io.openems.common.utils.IntUtils.Round;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
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
				.unit(Unit.MILLIHERTZ)), //
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
				.unit(Unit.WATT)), //
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
				.unit(Unit.WATT)), //
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
				.text(OpenemsConstants.POWER_DOC_TEXT) //
				.onInit(channel -> {
					channel.onSetNextValue(value -> {
						/*
						 * Fill Min/Max Active Power channels
						 */
						if (value.isDefined()) {
							int newValue = value.get();
							{
								Channel<Integer> minActivePowerChannel = channel.getComponent()
										.channel(ChannelId.MIN_ACTIVE_POWER);
								int minActivePower = minActivePowerChannel.value().orElse(0);
								int minNextActivePower = minActivePowerChannel.getNextValue().orElse(0);
								if (newValue < Math.min(minActivePower, minNextActivePower)) {
									// avoid getting called too often -> round to 100
									newValue = IntUtils.roundToPrecision(newValue, Round.TOWARDS_ZERO, 100);
									minActivePowerChannel.setNextValue(newValue);
								}
							}
							{
								Channel<Integer> maxActivePowerChannel = channel.getComponent()
										.channel(ChannelId.MAX_ACTIVE_POWER);
								int maxActivePower = maxActivePowerChannel.value().orElse(0);
								int maxNextActivePower = maxActivePowerChannel.getNextValue().orElse(0);
								if (newValue > Math.max(maxActivePower, maxNextActivePower)) {
									// avoid getting called too often -> round to 100
									newValue = IntUtils.roundToPrecision(newValue, Round.AWAY_FROM_ZERO, 100);
									maxActivePowerChannel.setNextValue(newValue);
								}
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
				.text(OpenemsConstants.POWER_DOC_TEXT)), //
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
				.unit(Unit.WATT_HOURS)),
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
				.unit(Unit.WATT_HOURS)),
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
				.unit(Unit.MILLIVOLT)), //
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
				.unit(Unit.MILLIAMPERE)); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the type of this Meter.
	 * 
	 * @return the MeterType
	 */
	MeterType getMeterType();

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
	 * Gets the Active Power in [W]. Negative values for Consumption; positive for
	 * Production
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getActivePower() {
		return this.channel(ChannelId.ACTIVE_POWER);
	}

	/**
	 * Gets the Reactive Power in [var]. Negative values for Consumption; positive
	 * for Production.
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getReactivePower() {
		return this.channel(ChannelId.REACTIVE_POWER);
	}

	/**
	 * Gets the Production Active Energy in [Wh]. This relates to positive
	 * ACTIVE_POWER.
	 * 
	 * @return the Channel
	 */
	default Channel<Long> getActiveProductionEnergy() {
		return this.channel(ChannelId.ACTIVE_PRODUCTION_ENERGY);
	}

	/**
	 * Gets the Frequency in [mHz]. FREQUENCY
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getFrequency() {
		return this.channel(ChannelId.FREQUENCY);
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
	 * Gets the Voltage in [mV], see {@link ChannelId#VOLTAGE}.
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
	 * Gets the Consumption Active Energy in [Wh]. This relates to negative
	 * ACTIVE_POWER.
	 * 
	 * @return the Channel
	 */
	default Channel<Long> getActiveConsumptionEnergy() {
		return this.channel(ChannelId.ACTIVE_CONSUMPTION_ENERGY);
	}

	/**
	 * Gets the Minimum Ever Active Power.
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getMinActivePower() {
		return this.channel(ChannelId.MIN_ACTIVE_POWER);
	}

	/**
	 * Gets the Maximum Ever Active Power.
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getMaxActivePower() {
		return this.channel(ChannelId.MAX_ACTIVE_POWER);
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
	 * Gets the Current in [mA], see {@link ChannelId#CURRENT}.
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
