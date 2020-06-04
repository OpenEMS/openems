package io.openems.edge.batteryinverter.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
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
		GRID_MODE(Doc.of(GridMode.values())),
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
				.unit(Unit.VOLT_AMPERE)), //
		/**
		 * Active Production Energy.
		 * 
		 * <p>
		 * The discharge energy.
		 * 
		 * <ul>
		 * <li>Interface: SymmetricBatteryInverter
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		ACTIVE_PRODUCTION_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),
		/**
		 * Active Consumption Energy.
		 * 
		 * <p>
		 * The charge energy.
		 * 
		 * <ul>
		 * <li>Interface: SymmetricBatteryInverter
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		ACTIVE_CONSUMPTION_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

	}

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
		return this.getGridModeChannel().value().get();
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
	 * Gets the Channel for {@link ChannelId#ACTIVE_PRODUCTION_ENERGY}.
	 * 
	 * @return the Channel
	 */
	public default LongReadChannel getActiveProductionEnergyChannel() {
		return this.channel(ChannelId.ACTIVE_PRODUCTION_ENERGY);
	}

	/**
	 * Gets the Active Production Energy in [Wh], i.e. the discharge energy. Range
	 * "&gt;= 0". See {@link ChannelId#ACTIVE_PRODUCTION_ENERGY}.
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
	 * Gets the Channel for {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY}.
	 * 
	 * @return the Channel
	 */
	public default LongReadChannel getActiveConsumptionEnergyChannel() {
		return this.channel(ChannelId.ACTIVE_CONSUMPTION_ENERGY);
	}

	/**
	 * Gets the Active Consumption Energy in [Wh], i.e. the charge energy. Range
	 * "&gt;= 0". See {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY}.
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

}
