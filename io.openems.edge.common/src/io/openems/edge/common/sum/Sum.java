package io.openems.edge.common.sum;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;

/**
 * Enables access to sum/average data.
 */
public interface Sum extends OpenemsComponent {

	public static final String SINGLETON_SERVICE_PID = "Core.Sum";
	public static final String SINGLETON_COMPONENT_ID = "_sum";

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Ess: Average State of Charge.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: Ess)
		 * <li>Type: Integer
		 * <li>Unit: %
		 * <li>Range: 0..100
		 * </ul>
		 */
		ESS_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text("Range 0..100")), //
		/**
		 * Ess: Active Power.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		ESS_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text("""
						AC-side power of Energy Storage System. \
						Includes excess DC-PV production for hybrid inverters. \
						Negative values for charge; positive for discharge""")),
		/**
		 * Reactive Power.
		 *
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * </ul>
		 */
		ESS_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Ess: Active Power L1.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss / AsymmetricEss)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		ESS_ACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text("""
						AC-side power of Energy Storage System on phase L1. \
						Includes excess DC-PV production for hybrid inverters. \
						Negative values for charge; positive for discharge""")),
		/**
		 * Ess: Active Power L2.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss / AsymmetricEss)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		ESS_ACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text("""
						AC-side power of Energy Storage System on phase L2. \
						Includes excess DC-PV production for hybrid inverters. \
						Negative values for charge; positive for discharge""")),
		/**
		 * Ess: Active Power L3.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss / AsymmetricEss)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		ESS_ACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text("""
						AC-side power of Energy Storage System on phase L3. \
						Includes excess DC-PV production for hybrid inverters. \
						Negative values for charge; positive for discharge""")),
		/**
		 * Ess: Discharge Power.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>For AC coupled energy storage systems this is the same as
		 * {@link ChannelId#ESS_ACTIVE_POWER}; for DC coupled or hybrid ESS this is the
		 * {@link ChannelId#ESS_ACTIVE_POWER} minus
		 * {@link ChannelId#PRODUCTION_DC_ACTUAL_POWER}, i.e. the power that is actually
		 * charged to or discharged from the battery.
		 * </ul>
		 */
		ESS_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text("Actual AC-side battery discharge power of Energy Storage System. " //
						+ "Negative values for charge; positive for discharge")),
		/**
		 * Ess: Minimum Ever Discharge Power (i.e. Maximum Ever Charge power as negative
		 * value).
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values or '0'
		 * </ul>
		 */
		ESS_MIN_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH)),
		/**
		 * Ess: Maximum Ever Discharge Power.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		ESS_MAX_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH)),
		/**
		 * Ess: Capacity.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: Ess)
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * <li>Range: should be only positive
		 * </ul>
		 */
		ESS_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //

		/**
		 * Grid: Active Power.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		GRID_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text("Grid exchange power. " //
						+ "Negative values for sell-to-grid; positive for buy-from-grid")),
		/**
		 * Grid: Active Power L1.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		GRID_ACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text("Grid exchange power on phase L1. " //
						+ "Negative values for sell-to-grid; positive for buy-from-grid")),
		/**
		 * Grid: Active Power L2.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		GRID_ACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text("Grid exchange power on phase L2. " //
						+ "Negative values for sell-to-grid; positive for buy-from-grid")),
		/**
		 * Grid: Active Power L3.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		GRID_ACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text("Grid exchange power on phase L3. " //
						+ "Negative values for sell-to-grid; positive for buy-from-grid")),
		/**
		 * Grid: Minimum Ever Active Power.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values or '0'
		 * </ul>
		 */
		GRID_MIN_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH)),
		/**
		 * Grid: Maximum Ever Active Power.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		GRID_MAX_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH)),
		/**
		 * Grid: Price for Buy-from-Grid.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: TimeOfUseTariff)
		 * <li>Type: Integer
		 * <li>Unit: Currency (see {@link Meta.ChannelId#CURRENCY}) per MWh
		 * </ul>
		 */
		GRID_BUY_PRICE(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.MONEY_PER_MEGAWATT_HOUR) //
				.persistencePriority(PersistencePriority.VERY_HIGH)),
		/**
		 * Production: Active Power.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter and ESS DC Charger)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * </ul>
		 */
		PRODUCTION_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text("Total production; always positive")),
		/**
		 * Production: AC Active Power.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * </ul>
		 */
		PRODUCTION_AC_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text("Production from AC source")),
		/**
		 * Production: AC Active Power L1.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * </ul>
		 */
		PRODUCTION_AC_ACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text("Production from AC source on phase L1")),
		/**
		 * Production: AC Active Power L2.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * </ul>
		 */
		PRODUCTION_AC_ACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text("Production from AC source on phase L2")),
		/**
		 * Production: AC Active Power L3.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * </ul>
		 */
		PRODUCTION_AC_ACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text("Production from AC source on phase L3")),
		/**
		 * Production: DC Actual Power.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: EssDcCharger)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * </ul>
		 */
		PRODUCTION_DC_ACTUAL_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text("Production from DC source")),
		/**
		 * Production: Maximum Ever Active Power.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		PRODUCTION_MAX_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Consumption: Active Power.
		 *
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * <li>Note: the value is calculated using the data from Grid-Meter,
		 * Production-Meter and charge/discharge of battery.
		 * </ul>
		 */
		CONSUMPTION_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Consumption: Active Power L1.
		 *
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * <li>Note: the value is calculated using the data from Grid-Meter,
		 * Production-Meter and charge/discharge of battery.
		 * </ul>
		 */
		CONSUMPTION_ACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Consumption: Active Power L2.
		 *
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * <li>Note: the value is calculated using the data from Grid-Meter,
		 * Production-Meter and charge/discharge of battery.
		 * </ul>
		 */
		CONSUMPTION_ACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Consumption: Active Power L3.
		 *
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * <li>Note: the value is calculated using the data from Grid-Meter,
		 * Production-Meter and charge/discharge of battery.
		 * </ul>
		 */
		CONSUMPTION_ACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Consumption: Maximum Ever Active Power.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		CONSUMPTION_MAX_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Unmanaged Consumption: Active Power.
		 *
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * <li>Note: this value represents the part of the Consumption that is not
		 * actively managed by OpenEMS, i.e. it is calculated as
		 * ({@link #CONSUMPTION_ACTIVE_POWER}) minus charge power for an electric
		 * vehicle charging station, etc. This value is used for forecasting of
		 * consumption.
		 * </ul>
		 */
		UNMANAGED_CONSUMPTION_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Grid-Mode.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss))
		 * <li>Type: Integer
		 * <li>Values: '-1' = UNDEFINED, '1' = On-Grid, '2' = Off-Grid
		 * </ul>
		 */
		GRID_MODE(Doc.of(GridMode.values()) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Cumulated Off-Grid time.
		 * 
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Cumulated Seconds
		 * </ul>
		 */
		GRID_MODE_OFF_GRID_TIME(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_SECONDS) //
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text("Total Off-Grid time")), //
		/**
		 * Ess: Max Apparent Power.
		 *
		 * <ul>
		 * <li>Interface: Max Apparent Power (origin: SymmetricEss))
		 * <li>Type: Integer
		 * <li>Unit: VA
		 * </ul>
		 */
		ESS_MAX_APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Ess: Active Charge Energy.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss)
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		ESS_ACTIVE_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Ess: Active Discharge Energy.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss)
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		ESS_ACTIVE_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Ess: DC Discharge Energy.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: HybridEss)
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		ESS_DC_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Ess: DC Charge Energy.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: HybridEss)
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		ESS_DC_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Grid: Buy-from-grid Energy ("Production").
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter)
		 * <li>Type: Integer
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		GRID_BUY_ACTIVE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Grid: Sell-to-grid Energy ("Consumption").
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter)
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		GRID_SELL_ACTIVE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Production: Energy.
		 *
		 * <ul>
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		PRODUCTION_ACTIVE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Production: AC Energy.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter)
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		PRODUCTION_AC_ACTIVE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Production: DC Energy.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: EssDcCharger)
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		PRODUCTION_DC_ACTIVE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Consumption: Energy.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter)
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		CONSUMPTION_ACTIVE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		/**
		 * Is there any Component Info/Warning/Fault that is getting ignored/hidden
		 * because of the 'ignoreStateComponents' configuration setting?.
		 */
		HAS_IGNORED_COMPONENT_STATES(Doc.of(Level.INFO) //
				.text("Component Warnings or Faults are being ignored"));

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
	 * Update all Channel-Values of this Sum-Component.
	 *
	 * <p>
	 * This method is called by the 'Cycle' just before the
	 * TOPIC_CYCLE_AFTER_PROCESS_IMAGE event.
	 */
	public void updateChannelsBeforeProcessImage();

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(Sum.class, accessMode, 220) //
				.channel(0, ChannelId.ESS_SOC, ModbusType.UINT16) //
				.channel(1, ChannelId.ESS_ACTIVE_POWER, ModbusType.FLOAT32) //
				.float32Reserved(3) // ChannelId.ESS_MIN_ACTIVE_POWER
				.float32Reserved(5) // ChannelId.ESS_MAX_ACTIVE_POWER
				.channel(7, ChannelId.ESS_REACTIVE_POWER, ModbusType.FLOAT32) // ESS_REACTIVE_POWER
				.float32Reserved(9) // ChannelId.ESS_MIN_REACTIVE_POWER
				.float32Reserved(11) // ChannelId.ESS_MAX_REACTIVE_POWER
				.channel(13, ChannelId.GRID_ACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(15, ChannelId.GRID_MIN_ACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(17, ChannelId.GRID_MAX_ACTIVE_POWER, ModbusType.FLOAT32) //
				.float32Reserved(19) // ChannelId.GRID_REACTIVE_POWER
				.float32Reserved(21) // ChannelId.GRID_MIN_REACTIVE_POWER
				.float32Reserved(23) // ChannelId.GRID_MAX_REACTIVE_POWER
				.channel(25, ChannelId.PRODUCTION_ACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(27, ChannelId.PRODUCTION_MAX_ACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(29, ChannelId.PRODUCTION_AC_ACTIVE_POWER, ModbusType.FLOAT32) //
				.float32Reserved(31) // ChannelId.PRODUCTION_MAX_AC_ACTIVE_POWER
				.float32Reserved(33) // ChannelId.PRODUCTION_AC_REACTIVE_POWER
				.float32Reserved(35) // ChannelId.PRODUCTION_MAX_AC_REACTIVE_POWER
				.channel(37, ChannelId.PRODUCTION_DC_ACTUAL_POWER, ModbusType.FLOAT32) //
				.float32Reserved(39) // ChannelId.PRODUCTION_MAX_DC_ACTUAL_POWER
				.channel(41, ChannelId.CONSUMPTION_ACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(43, ChannelId.CONSUMPTION_MAX_ACTIVE_POWER, ModbusType.FLOAT32) //
				.float32Reserved(45) // ChannelId.CONSUMPTION_REACTIVE_POWER
				.float32Reserved(47) // ChannelId.CONSUMPTION_MAX_REACTIVE_POWER
				.channel(49, ChannelId.ESS_ACTIVE_CHARGE_ENERGY, ModbusType.FLOAT64) //
				.channel(53, ChannelId.ESS_ACTIVE_DISCHARGE_ENERGY, ModbusType.FLOAT64) //
				.channel(57, ChannelId.GRID_BUY_ACTIVE_ENERGY, ModbusType.FLOAT64) //
				.channel(61, ChannelId.GRID_SELL_ACTIVE_ENERGY, ModbusType.FLOAT64) //
				.channel(65, ChannelId.PRODUCTION_ACTIVE_ENERGY, ModbusType.FLOAT64) //
				.channel(69, ChannelId.PRODUCTION_AC_ACTIVE_ENERGY, ModbusType.FLOAT64) //
				.channel(73, ChannelId.PRODUCTION_DC_ACTIVE_ENERGY, ModbusType.FLOAT64) //
				.channel(77, ChannelId.CONSUMPTION_ACTIVE_ENERGY, ModbusType.FLOAT64) //
				.channel(81, ChannelId.ESS_DC_CHARGE_ENERGY, ModbusType.FLOAT64) //
				.channel(85, ChannelId.ESS_DC_DISCHARGE_ENERGY, ModbusType.FLOAT64) //
				.channel(89, ChannelId.ESS_ACTIVE_POWER_L1, ModbusType.FLOAT32) //
				.channel(91, ChannelId.ESS_ACTIVE_POWER_L2, ModbusType.FLOAT32) //
				.channel(93, ChannelId.ESS_ACTIVE_POWER_L3, ModbusType.FLOAT32) //
				.channel(95, ChannelId.GRID_ACTIVE_POWER_L1, ModbusType.FLOAT32) //
				.channel(97, ChannelId.GRID_ACTIVE_POWER_L2, ModbusType.FLOAT32) //
				.channel(99, ChannelId.GRID_ACTIVE_POWER_L3, ModbusType.FLOAT32) //
				.channel(101, ChannelId.PRODUCTION_AC_ACTIVE_POWER_L1, ModbusType.FLOAT32) //
				.channel(103, ChannelId.PRODUCTION_AC_ACTIVE_POWER_L2, ModbusType.FLOAT32) //
				.channel(105, ChannelId.PRODUCTION_AC_ACTIVE_POWER_L3, ModbusType.FLOAT32) //
				.channel(107, ChannelId.CONSUMPTION_ACTIVE_POWER_L1, ModbusType.FLOAT32) //
				.channel(109, ChannelId.CONSUMPTION_ACTIVE_POWER_L2, ModbusType.FLOAT32) //
				.channel(111, ChannelId.CONSUMPTION_ACTIVE_POWER_L3, ModbusType.FLOAT32) //
				.channel(113, ChannelId.ESS_DISCHARGE_POWER, ModbusType.FLOAT32) //
				.channel(115, ChannelId.GRID_MODE, ModbusType.ENUM16) //
				.channel(116, ChannelId.GRID_MODE_OFF_GRID_TIME, ModbusType.FLOAT32) //
				.build();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS_SOC}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssSocChannel() {
		return this.channel(ChannelId.ESS_SOC);
	}

	/**
	 * Gets the Average of all Energy Storage System State of Charge in [%], range
	 * 0..100 %. See {@link ChannelId#ESS_SOC}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEssSoc() {
		return this.getEssSocChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ESS_SOC} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssSoc(Integer value) {
		this.getEssSocChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ESS_SOC} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssSoc(int value) {
		this.getEssSocChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssActivePowerChannel() {
		return this.channel(ChannelId.ESS_ACTIVE_POWER);
	}

	/**
	 * Gets the Sum of all Energy Storage System Active Power in [W]. Negative
	 * values for Charge; positive for Discharge. See
	 * {@link ChannelId#ESS_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEssActivePower() {
		return this.getEssActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ESS_ACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssActivePower(Integer value) {
		this.getEssActivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ESS_ACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssActivePower(int value) {
		this.getEssActivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS_REACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssReactivePowerChannel() {
		return this.channel(ChannelId.ESS_REACTIVE_POWER);
	}

	/**
	 * Gets the Sum of all Energy Storage System Reactive Power in [var].
	 * {@link ChannelId#ESS_REACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEssReactivePower() {
		return this.getEssReactivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ESS_REACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssReactivePower(Integer value) {
		this.getEssReactivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS_ACTIVE_POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssActivePowerL1Channel() {
		return this.channel(ChannelId.ESS_ACTIVE_POWER_L1);
	}

	/**
	 * Gets the Sum of all Energy Storage System Active Power on L1 in [W]. Negative
	 * values for Charge; positive for Discharge. See
	 * {@link ChannelId#ESS_ACTIVE_POWER_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEssActivePowerL1() {
		return this.getEssActivePowerL1Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ESS_ACTIVE_POWER_L1} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssActivePowerL1(Integer value) {
		this.getEssActivePowerL1Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ESS_ACTIVE_POWER_L1} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssActivePowerL1(int value) {
		this.getEssActivePowerL1Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS_ACTIVE_POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssActivePowerL2Channel() {
		return this.channel(ChannelId.ESS_ACTIVE_POWER_L2);
	}

	/**
	 * Gets the Sum of all Energy Storage System Active Power on L2 in [W]. Negative
	 * values for Charge; positive for Discharge. See
	 * {@link ChannelId#ESS_ACTIVE_POWER_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEssActivePowerL2() {
		return this.getEssActivePowerL2Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ESS_ACTIVE_POWER_L2} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssActivePowerL2(Integer value) {
		this.getEssActivePowerL2Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ESS_ACTIVE_POWER_L2} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssActivePowerL2(int value) {
		this.getEssActivePowerL2Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS_ACTIVE_POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssActivePowerL3Channel() {
		return this.channel(ChannelId.ESS_ACTIVE_POWER_L3);
	}

	/**
	 * Gets the Sum of all Energy Storage System Active Power on L3 in [W]. Negative
	 * values for Charge; positive for Discharge. See
	 * {@link ChannelId#ESS_ACTIVE_POWER_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEssActivePowerL3() {
		return this.getEssActivePowerL3Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ESS_ACTIVE_POWER_L3} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssActivePowerL3(Integer value) {
		this.getEssActivePowerL3Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ESS_ACTIVE_POWER_L3} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssActivePowerL3(int value) {
		this.getEssActivePowerL3Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS_DISCHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssDischargePowerChannel() {
		return this.channel(ChannelId.ESS_DISCHARGE_POWER);
	}

	/**
	 * Gets the Sum of all Energy Storage System Discharge Power [W]. See
	 * {@link ChannelId#ESS_DISCHARGE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEssDischargePower() {
		return this.getEssDischargePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ESS_DISCHARGE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssDischargePower(Integer value) {
		this.getEssDischargePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ESS_DISCHARGE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssDischargePower(int value) {
		this.getEssDischargePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS_MAX_DISCHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssMaxDischargePowerChannel() {
		return this.channel(ChannelId.ESS_MAX_DISCHARGE_POWER);
	}

	/**
	 * Gets the Total Maximum Ever ESS Discharge Power in [W]. See
	 * {@link ChannelId#ESS_MAX_DISCHARGE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEssMaxDischargePower() {
		return this.getEssMaxDischargePowerChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS_MIN_DISCHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssMinDischargePowerChannel() {
		return this.channel(ChannelId.ESS_MIN_DISCHARGE_POWER);
	}

	/**
	 * Gets the Total Minimum Ever ESS Discharge Power in [W] (i.e. Maximum Ever
	 * Charge power as negative value). See
	 * {@link ChannelId#ESS_MIN_DISCHARGE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEssMinDischargePower() {
		return this.getEssMinDischargePowerChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS_CAPACITY}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssCapacityChannel() {
		return this.channel(ChannelId.ESS_CAPACITY);
	}

	/**
	 * Gets the Sum of all Energy Storage System Capacity in [Wh]. See
	 * {@link ChannelId#ESS_CAPACITY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEssCapacity() {
		return this.getEssCapacityChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ESS_CAPACITY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssCapacity(Integer value) {
		this.getEssCapacityChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ESS_CAPACITY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssCapacity(int value) {
		this.getEssCapacityChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridActivePowerChannel() {
		return this.channel(ChannelId.GRID_ACTIVE_POWER);
	}

	/**
	 * Gets the Total Grid Active Power in [W]. See
	 * {@link ChannelId#GRID_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridActivePower() {
		return this.getGridActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#GRID_ACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridActivePower(Integer value) {
		this.getGridActivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#GRID_ACTIVE_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridActivePower(int value) {
		this.getGridActivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_ACTIVE_POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridActivePowerL1Channel() {
		return this.channel(ChannelId.GRID_ACTIVE_POWER_L1);
	}

	/**
	 * Gets the Total Grid Active Power on L1 in [W]. See
	 * {@link ChannelId#GRID_ACTIVE_POWER_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridActivePowerL1() {
		return this.getGridActivePowerL1Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#GRID_ACTIVE_POWER_L1} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridActivePowerL1(Integer value) {
		this.getGridActivePowerL1Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#GRID_ACTIVE_POWER_L1} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridActivePowerL1(int value) {
		this.getGridActivePowerL1Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_ACTIVE_POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridActivePowerL2Channel() {
		return this.channel(ChannelId.GRID_ACTIVE_POWER_L2);
	}

	/**
	 * Gets the Total Grid Active Power on L2 in [W]. See
	 * {@link ChannelId#GRID_ACTIVE_POWER_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridActivePowerL2() {
		return this.getGridActivePowerL2Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#GRID_ACTIVE_POWER_L2} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridActivePowerL2(Integer value) {
		this.getGridActivePowerL2Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#GRID_ACTIVE_POWER_L2} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridActivePowerL2(int value) {
		this.getGridActivePowerL2Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_ACTIVE_POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridActivePowerL3Channel() {
		return this.channel(ChannelId.GRID_ACTIVE_POWER_L3);
	}

	/**
	 * Gets the Total Grid Active Power on L3 in [W]. See
	 * {@link ChannelId#GRID_ACTIVE_POWER_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridActivePowerL3() {
		return this.getGridActivePowerL3Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#GRID_ACTIVE_POWER_L3} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridActivePowerL3(Integer value) {
		this.getGridActivePowerL3Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#GRID_ACTIVE_POWER_L3} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridActivePowerL3(int value) {
		this.getGridActivePowerL3Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_BUY_PRICE}.
	 *
	 * @return the Channel
	 */
	public default DoubleReadChannel getGridBuyPriceChannel() {
		return this.channel(ChannelId.GRID_BUY_PRICE);
	}

	/**
	 * Gets the Buy-from-Grid price [Currency/MWh]. See
	 * {@link ChannelId#GRID_BUY_PRICE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Double> getGridBuyPrice() {
		return this.getGridBuyPriceChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#GRID_BUY_PRICE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridBuyPrice(Double value) {
		this.getGridBuyPriceChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_MIN_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridMinActivePowerChannel() {
		return this.channel(ChannelId.GRID_MIN_ACTIVE_POWER);
	}

	/**
	 * Gets the Total Minimum Ever Active Power in [W]. See
	 * {@link ChannelId#GRID_MIN_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridMinActivePower() {
		return this.getGridMinActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#GRID_MIN_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridMinActivePower(Integer value) {
		this.getGridMinActivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#GRID_MIN_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridMinActivePower(int value) {
		this.getGridMinActivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_MAX_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridMaxActivePowerChannel() {
		return this.channel(ChannelId.GRID_MAX_ACTIVE_POWER);
	}

	/**
	 * Gets the Total Maximum Ever Active Power in [W]. See
	 * {@link ChannelId#GRID_MAX_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridMaxActivePower() {
		return this.getGridMaxActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#GRID_MAX_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridMaxActivePower(Integer value) {
		this.getGridMaxActivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#GRID_MAX_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridMaxActivePower(int value) {
		this.getGridMaxActivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PRODUCTION_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getProductionActivePowerChannel() {
		return this.channel(ChannelId.PRODUCTION_ACTIVE_POWER);
	}

	/**
	 * Gets the Total Production Active Power in [W]. See
	 * {@link ChannelId#PRODUCTION_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getProductionActivePower() {
		return this.getProductionActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionActivePower(Integer value) {
		this.getProductionActivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionActivePower(int value) {
		this.getProductionActivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PRODUCTION_AC_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getProductionAcActivePowerChannel() {
		return this.channel(ChannelId.PRODUCTION_AC_ACTIVE_POWER);
	}

	/**
	 * Gets the Total AC Production Active Power in [W]. See
	 * {@link ChannelId#PRODUCTION_AC_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getProductionAcActivePower() {
		return this.getProductionAcActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_AC_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionAcActivePower(Integer value) {
		this.getProductionAcActivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_AC_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionAcActivePower(int value) {
		this.getProductionAcActivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PRODUCTION_AC_ACTIVE_POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getProductionAcActivePowerL1Channel() {
		return this.channel(ChannelId.PRODUCTION_AC_ACTIVE_POWER_L1);
	}

	/**
	 * Gets the Total AC Production Active Power on L1 in [W]. See
	 * {@link ChannelId#PRODUCTION_AC_ACTIVE_POWER_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getProductionAcActivePowerL1() {
		return this.getProductionAcActivePowerL1Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_AC_ACTIVE_POWER_L1} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionAcActivePowerL1(Integer value) {
		this.getProductionAcActivePowerL1Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_AC_ACTIVE_POWER_L1} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionAcActivePowerL1(int value) {
		this.getProductionAcActivePowerL1Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PRODUCTION_AC_ACTIVE_POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getProductionAcActivePowerL2Channel() {
		return this.channel(ChannelId.PRODUCTION_AC_ACTIVE_POWER_L2);
	}

	/**
	 * Gets the Total AC Production Active Power on L2 in [W]. See
	 * {@link ChannelId#PRODUCTION_AC_ACTIVE_POWER_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getProductionAcActivePowerL2() {
		return this.getProductionAcActivePowerL2Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_AC_ACTIVE_POWER_L2} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionAcActivePowerL2(Integer value) {
		this.getProductionAcActivePowerL2Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_AC_ACTIVE_POWER_L2} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionAcActivePowerL2(int value) {
		this.getProductionAcActivePowerL2Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PRODUCTION_AC_ACTIVE_POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getProductionAcActivePowerL3Channel() {
		return this.channel(ChannelId.PRODUCTION_AC_ACTIVE_POWER_L3);
	}

	/**
	 * Gets the Total AC Production Active Power on L3 in [W]. See
	 * {@link ChannelId#PRODUCTION_AC_ACTIVE_POWER_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getProductionAcActivePowerL3() {
		return this.getProductionAcActivePowerL3Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_AC_ACTIVE_POWER_L3} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionAcActivePowerL3(Integer value) {
		this.getProductionAcActivePowerL3Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_AC_ACTIVE_POWER_L3} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionAcActivePowerL3(int value) {
		this.getProductionAcActivePowerL3Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PRODUCTION_DC_ACTUAL_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getProductionDcActualPowerChannel() {
		return this.channel(ChannelId.PRODUCTION_DC_ACTUAL_POWER);
	}

	/**
	 * Gets the Total DC Production Actual Power in [W]. See
	 * {@link ChannelId#PRODUCTION_DC_ACTUAL_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getProductionDcActualPower() {
		return this.getProductionDcActualPowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_DC_ACTUAL_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionDcActualPower(Integer value) {
		this.getProductionDcActualPowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_DC_ACTUAL_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionDcActualPower(int value) {
		this.getProductionDcActualPowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PRODUCTION_MAX_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getProductionMaxActivePowerChannel() {
		return this.channel(ChannelId.PRODUCTION_MAX_ACTIVE_POWER);
	}

	/**
	 * Gets the Total Maximum Ever Production Active Power in [W]. See
	 * {@link ChannelId#PRODUCTION_MAX_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getProductionMaxActivePower() {
		return this.getProductionMaxActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_MAX_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionMaxActivePower(Integer value) {
		this.getProductionMaxActivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_MAX_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionMaxActivePower(int value) {
		this.getProductionMaxActivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CONSUMPTION_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getConsumptionActivePowerChannel() {
		return this.channel(ChannelId.CONSUMPTION_ACTIVE_POWER);
	}

	/**
	 * Gets the Total Consumption Active Power in [W]. See
	 * {@link ChannelId#CONSUMPTION_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getConsumptionActivePower() {
		return this.getConsumptionActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CONSUMPTION_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConsumptionActivePower(Integer value) {
		this.getConsumptionActivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CONSUMPTION_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConsumptionActivePower(int value) {
		this.getConsumptionActivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#UNMANAGED_CONSUMPTION_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getUnmanagedConsumptionActivePowerChannel() {
		return this.channel(ChannelId.UNMANAGED_CONSUMPTION_ACTIVE_POWER);
	}

	/**
	 * Gets the Unmanaged Consumption Active Power in [W]. See
	 * {@link ChannelId#UNMANAGED_CONSUMPTION_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getUnmanagedConsumptionActivePower() {
		return this.getUnmanagedConsumptionActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#UNMANAGED_CONSUMPTION_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setUnmanagedConsumptionActivePower(Integer value) {
		this.getUnmanagedConsumptionActivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CONSUMPTION_ACTIVE_POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getConsumptionActivePowerL1Channel() {
		return this.channel(ChannelId.CONSUMPTION_ACTIVE_POWER_L1);
	}

	/**
	 * Gets the Total Consumption Active Power on L1 in [W]. See
	 * {@link ChannelId#CONSUMPTION_ACTIVE_POWER_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getConsumptionActivePowerL1() {
		return this.getConsumptionActivePowerL1Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CONSUMPTION_ACTIVE_POWER_L1} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConsumptionActivePowerL1(Integer value) {
		this.getConsumptionActivePowerL1Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CONSUMPTION_ACTIVE_POWER_L1} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConsumptionActivePowerL1(int value) {
		this.getConsumptionActivePowerL1Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CONSUMPTION_ACTIVE_POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getConsumptionActivePowerL2Channel() {
		return this.channel(ChannelId.CONSUMPTION_ACTIVE_POWER_L2);
	}

	/**
	 * Gets the Total Consumption Active Power on L2 in [W]. See
	 * {@link ChannelId#CONSUMPTION_ACTIVE_POWER_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getConsumptionActivePowerL2() {
		return this.getConsumptionActivePowerL2Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CONSUMPTION_ACTIVE_POWER_L2} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConsumptionActivePowerL2(Integer value) {
		this.getConsumptionActivePowerL2Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CONSUMPTION_ACTIVE_POWER_L2} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConsumptionActivePowerL2(int value) {
		this.getConsumptionActivePowerL2Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CONSUMPTION_ACTIVE_POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getConsumptionActivePowerL3Channel() {
		return this.channel(ChannelId.CONSUMPTION_ACTIVE_POWER_L3);
	}

	/**
	 * Gets the Total Consumption Active Power on L3 in [W]. See
	 * {@link ChannelId#CONSUMPTION_ACTIVE_POWER_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getConsumptionActivePowerL3() {
		return this.getConsumptionActivePowerL3Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CONSUMPTION_ACTIVE_POWER_L3} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConsumptionActivePowerL3(Integer value) {
		this.getConsumptionActivePowerL3Channel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CONSUMPTION_ACTIVE_POWER_L3} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConsumptionActivePowerL3(int value) {
		this.getConsumptionActivePowerL3Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CONSUMPTION_MAX_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getConsumptionMaxActivePowerChannel() {
		return this.channel(ChannelId.CONSUMPTION_MAX_ACTIVE_POWER);
	}

	/**
	 * Gets the Total Maximum Ever Consumption Active Power in [W]. See
	 * {@link ChannelId#CONSUMPTION_MAX_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getConsumptionMaxActivePower() {
		return this.getConsumptionMaxActivePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CONSUMPTION_MAX_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConsumptionMaxActivePower(Integer value) {
		this.getConsumptionMaxActivePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CONSUMPTION_MAX_ACTIVE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConsumptionMaxActivePower(int value) {
		this.getConsumptionMaxActivePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS_MAX_APPARENT_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssMaxApparentPowerChannel() {
		return this.channel(ChannelId.ESS_MAX_APPARENT_POWER);
	}

	/**
	 * Gets the Sum of all Energy Storage Systems Maximum Apparent Power in [VA].
	 * See {@link ChannelId#ESS_MAX_APPARENT_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEssMaxApparentPower() {
		return this.getEssMaxApparentPowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ESS_MAX_APPARENT_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssMaxApparentPower(Integer value) {
		this.getEssMaxApparentPowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ESS_MAX_APPARENT_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssMaxApparentPower(int value) {
		this.getEssMaxApparentPowerChannel().setNextValue(value);
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
	 * Gets the Overall GridMode of all Energy Storage Systems. See
	 * {@link ChannelId#GRID_MODE}.
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
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#GRID_MODE_OFF_GRID_TIME} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridModeOffGridTime(int value) {
		this.getGridModeOffGridTimeChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_MODE_OFF_GRID_TIME}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getGridModeOffGridTimeChannel() {
		return this.channel(ChannelId.GRID_MODE_OFF_GRID_TIME);
	}

	/**
	 * Gets the Overall GridMode of all Energy Storage Systems. See
	 * {@link ChannelId#GRID_MODE_OFF_GRID_TIME}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getGridModeOffGridTimeValue() {
		return this.getGridModeOffGridTimeChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS_ACTIVE_CHARGE_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getEssActiveChargeEnergyChannel() {
		return this.channel(ChannelId.ESS_ACTIVE_CHARGE_ENERGY);
	}

	/**
	 * Gets the Sum of all Energy Storage Systems Active Charge Energy in [Wh_Σ].
	 * See {@link ChannelId#ESS_ACTIVE_CHARGE_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getEssActiveChargeEnergy() {
		return this.getEssActiveChargeEnergyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ESS_ACTIVE_CHARGE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssActiveChargeEnergy(Long value) {
		this.getEssActiveChargeEnergyChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ESS_ACTIVE_CHARGE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssActiveChargeEnergy(long value) {
		this.getEssActiveChargeEnergyChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ESS_ACTIVE_DISCHARGE_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getEssActiveDischargeEnergyChannel() {
		return this.channel(ChannelId.ESS_ACTIVE_DISCHARGE_ENERGY);
	}

	/**
	 * Gets the Sum of all Energy Storage Systems Active Discharge Energy in [Wh_Σ].
	 * See {@link ChannelId#ESS_ACTIVE_DISCHARGE_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getEssActiveDischargeEnergy() {
		return this.getEssActiveDischargeEnergyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ESS_ACTIVE_DISCHARGE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssActiveDischargeEnergy(Long value) {
		this.getEssActiveDischargeEnergyChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ESS_ACTIVE_DISCHARGE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEssActiveDischargeEnergy(long value) {
		this.getEssActiveDischargeEnergyChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_BUY_ACTIVE_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getGridBuyActiveEnergyChannel() {
		return this.channel(ChannelId.GRID_BUY_ACTIVE_ENERGY);
	}

	/**
	 * Gets the Total Grid Buy Active Energy in [Wh_Σ]. See
	 * {@link ChannelId#GRID_BUY_ACTIVE_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getGridBuyActiveEnergy() {
		return this.getGridBuyActiveEnergyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#GRID_BUY_ACTIVE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridBuyActiveEnergy(Long value) {
		this.getGridBuyActiveEnergyChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#GRID_BUY_ACTIVE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridBuyActiveEnergy(long value) {
		this.getGridBuyActiveEnergyChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_SELL_ACTIVE_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getGridSellActiveEnergyChannel() {
		return this.channel(ChannelId.GRID_SELL_ACTIVE_ENERGY);
	}

	/**
	 * Gets the Total Grid Sell Active Energy in [Wh_Σ]. See
	 * {@link ChannelId#GRID_SELL_ACTIVE_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getGridSellActiveEnergy() {
		return this.getGridSellActiveEnergyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#GRID_SELL_ACTIVE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridSellActiveEnergy(Long value) {
		this.getGridSellActiveEnergyChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#GRID_SELL_ACTIVE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGridSellActiveEnergy(long value) {
		this.getGridSellActiveEnergyChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PRODUCTION_ACTIVE_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getProductionActiveEnergyChannel() {
		return this.channel(ChannelId.PRODUCTION_ACTIVE_ENERGY);
	}

	/**
	 * Gets the Total Production Active Energy in [Wh_Σ]. See
	 * {@link ChannelId#PRODUCTION_ACTIVE_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getProductionActiveEnergy() {
		return this.getProductionActiveEnergyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_ACTIVE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionActiveEnergy(Long value) {
		this.getProductionActiveEnergyChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_ACTIVE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionActiveEnergy(long value) {
		this.getProductionActiveEnergyChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PRODUCTION_AC_ACTIVE_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getProductionAcActiveEnergyChannel() {
		return this.channel(ChannelId.PRODUCTION_AC_ACTIVE_ENERGY);
	}

	/**
	 * Gets the Total AC Production Active Energy in [Wh_Σ]. See
	 * {@link ChannelId#PRODUCTION_AC_ACTIVE_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getProductionAcActiveEnergy() {
		return this.getProductionAcActiveEnergyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_AC_ACTIVE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionAcActiveEnergy(Long value) {
		this.getProductionAcActiveEnergyChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_AC_ACTIVE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionAcActiveEnergy(long value) {
		this.getProductionAcActiveEnergyChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PRODUCTION_DC_ACTIVE_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getProductionDcActiveEnergyChannel() {
		return this.channel(ChannelId.PRODUCTION_DC_ACTIVE_ENERGY);
	}

	/**
	 * Gets the Total DC Production Active Energy in [Wh_Σ]. See
	 * {@link ChannelId#PRODUCTION_DC_ACTIVE_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getProductionDcActiveEnergy() {
		return this.getProductionDcActiveEnergyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_DC_ACTIVE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionDcActiveEnergy(Long value) {
		this.getProductionDcActiveEnergyChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PRODUCTION_DC_ACTIVE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setProductionDcActiveEnergy(long value) {
		this.getProductionDcActiveEnergyChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CONSUMPTION_ACTIVE_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getConsumptionActiveEnergyChannel() {
		return this.channel(ChannelId.CONSUMPTION_ACTIVE_ENERGY);
	}

	/**
	 * Gets the Total Consumption Active Energy in [Wh_Σ]. See
	 * {@link ChannelId#CONSUMPTION_ACTIVE_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getConsumptionActiveEnergy() {
		return this.getConsumptionActiveEnergyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CONSUMPTION_ACTIVE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConsumptionActiveEnergy(Long value) {
		this.getConsumptionActiveEnergyChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CONSUMPTION_ACTIVE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConsumptionActiveEnergy(long value) {
		this.getConsumptionActiveEnergyChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#HAS_IGNORED_COMPONENT_STATES}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getHasIgnoredComponentStatesChannel() {
		return this.channel(ChannelId.HAS_IGNORED_COMPONENT_STATES);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#HAS_IGNORED_COMPONENT_STATES} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setHasIgnoredComponentStates(boolean value) {
		this.getHasIgnoredComponentStatesChannel().setNextValue(value);
	}
}
