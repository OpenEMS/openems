package io.openems.edge.common.sum;

import static io.openems.common.channel.Level.INFO;
import static io.openems.common.channel.PersistencePriority.VERY_HIGH;
import static io.openems.common.channel.Unit.CUMULATED_SECONDS;
import static io.openems.common.channel.Unit.CUMULATED_WATT_HOURS;
import static io.openems.common.channel.Unit.MONEY_PER_MEGAWATT_HOUR;
import static io.openems.common.channel.Unit.PERCENT;
import static io.openems.common.channel.Unit.VOLT_AMPERE;
import static io.openems.common.channel.Unit.VOLT_AMPERE_REACTIVE;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.channel.Unit.WATT_HOURS;
import static io.openems.common.types.OpenemsType.DOUBLE;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;

import io.openems.common.channel.AccessMode;
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
		 * Energy Storage System (ESS): Average State of Charge.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss)
		 * <li>Type: Integer
		 * <li>Unit: %
		 * <li>Range: 0..100
		 * </ul>
		 */
		ESS_SOC(Doc.of(INTEGER)//
				.unit(PERCENT)//
				.persistencePriority(VERY_HIGH)//
				.text("Range 0..100")), //

		/**
		 * Energy Storage System (ESS): Active Power.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		ESS_ACTIVE_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
				.text("""
						AC-side power of Energy Storage System. \
						Includes excess DC-PV production for hybrid inverters. \
						Negative values for charge; positive for discharge""")),

		/**
		 * Energy Storage System (ESS): Reactive Power.
		 *
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * </ul>
		 */
		ESS_REACTIVE_POWER(Doc.of(INTEGER)//
				.unit(VOLT_AMPERE_REACTIVE)//
				.persistencePriority(VERY_HIGH)), //

		/**
		 * Energy Storage System (ESS): Active Power L1.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: AsymmetricEss)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		ESS_ACTIVE_POWER_L1(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
				.text("""
						AC-side power of Energy Storage System on phase L1. \
						Includes excess DC-PV production for hybrid inverters. \
						Negative values for charge; positive for discharge""")),

		/**
		 * Energy Storage System (ESS): Active Power L2.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: AsymmetricEss)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		ESS_ACTIVE_POWER_L2(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
				.text("""
						AC-side power of Energy Storage System on phase L2. \
						Includes excess DC-PV production for hybrid inverters. \
						Negative values for charge; positive for discharge""")),

		/**
		 * Energy Storage System (ESS): Active Power L3.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: AsymmetricEss)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		ESS_ACTIVE_POWER_L3(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
				.text("""
						AC-side power of Energy Storage System on phase L3. \
						Includes excess DC-PV production for hybrid inverters. \
						Negative values for charge; positive for discharge""")),

		/**
		 * Energy Storage System (ESS): Discharge Power.
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
		ESS_DISCHARGE_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
				.text("Actual AC-side battery discharge power of Energy Storage System. "//
						+ "Negative values for charge; positive for discharge")),

		/**
		 * Energy Storage System (ESS): Minimum Ever Discharge Power (i.e. Maximum Ever
		 * Charge power as negative value).
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values or '0'
		 * </ul>
		 */
		ESS_MIN_DISCHARGE_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)),

		/**
		 * Energy Storage System (ESS): Maximum Ever Discharge Power.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		ESS_MAX_DISCHARGE_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)),

		/**
		 * Energy Storage System (ESS): Capacity.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: Ess)
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * <li>Range: should be only positive
		 * </ul>
		 */
		ESS_CAPACITY(Doc.of(INTEGER)//
				.unit(WATT_HOURS)//
				.persistencePriority(VERY_HIGH)), //

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
		GRID_ACTIVE_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
				.text("Grid exchange power. "//
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
		GRID_ACTIVE_POWER_L1(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
				.text("Grid exchange power on phase L1. "//
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
		GRID_ACTIVE_POWER_L2(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
				.text("Grid exchange power on phase L2. "//
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
		GRID_ACTIVE_POWER_L3(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
				.text("Grid exchange power on phase L3. "//
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
		GRID_MIN_ACTIVE_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)),

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
		GRID_MAX_ACTIVE_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)),

		/**
		 * Grid: Price for Buy-from-Grid.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: TimeOfUseTariff)
		 * <li>Type: Integer
		 * <li>Unit: Currency (see {@link Meta.ChannelId#CURRENCY}) per MWh
		 * </ul>
		 */
		GRID_BUY_PRICE(Doc.of(DOUBLE)//
				.unit(MONEY_PER_MEGAWATT_HOUR)//
				.persistencePriority(VERY_HIGH)),

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
		PRODUCTION_ACTIVE_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
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
		PRODUCTION_AC_ACTIVE_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
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
		PRODUCTION_AC_ACTIVE_POWER_L1(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
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
		PRODUCTION_AC_ACTIVE_POWER_L2(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
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
		PRODUCTION_AC_ACTIVE_POWER_L3(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
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
		PRODUCTION_DC_ACTUAL_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
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
		PRODUCTION_MAX_ACTIVE_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)), //

		/**
		 * Unmanaged Production: Active Power.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter and ESS DC Charger)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive; greater than or equal to
		 * {@link ChannelId#PRODUCTION_ACTIVE_POWER}
		 * <li>Note: this value represents the part of the active power production that
		 * is not actively managed or curtailed by OpenEMS or external regulations,
		 * i.e., it reflects the raw production before any control actions such as
		 * feed-in limitation due to grid constraints or new regulations on PV systems.
		 * This value can be used for forecasting or analysis of the unmanaged
		 * production.
		 * </ul>
		 */
		UNMANAGED_PRODUCTION_ACTIVE_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)), //

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
		CONSUMPTION_ACTIVE_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
				.text("Active power of the electrical consumption")), //

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
		CONSUMPTION_ACTIVE_POWER_L1(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
				.text("Active power of the electrical consumption on phase L1")), //

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
		CONSUMPTION_ACTIVE_POWER_L2(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
				.text("Active power of the electrical consumption on phase L2")), //

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
		CONSUMPTION_ACTIVE_POWER_L3(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
				.text("Active power of the electrical consumption on phase L3")), //

		/**
		 * Consumption: Maximum Ever Active Power.
		 *
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		CONSUMPTION_MAX_ACTIVE_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)//
				.text("Maximum measured active power of the electrical consumption")), //

		/**
		 * Unmanaged Consumption: Active Power.
		 *
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive; less than or equal to
		 * {@link ChannelId#CONSUMPTION_ACTIVE_POWER}
		 * <li>Note: this value represents the part of the Consumption that is not
		 * actively managed by OpenEMS, i.e. it is calculated as
		 * ({@link #CONSUMPTION_ACTIVE_POWER}) minus charge power for an electric
		 * vehicle charging station, etc. This value is used for forecasting of
		 * consumption.
		 * </ul>
		 */
		UNMANAGED_CONSUMPTION_ACTIVE_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)), //

		/**
		 * Grid-Mode.
		 *
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Integer
		 * <li>Values:
		 * <ul>
		 * <li>'-1' = UNDEFINED
		 * <li>'1' = On-Grid
		 * <li>'2' = Off-Grid
		 * <li>'3' = 'Off-Grid Genset'
		 * </ul>
		 * </ul>
		 */
		GRID_MODE(Doc.of(GridMode.values())//
				.persistencePriority(VERY_HIGH)), //

		/**
		 * Cumulated Off-Grid time.
		 *
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Cumulated Seconds
		 * </ul>
		 */
		GRID_MODE_OFF_GRID_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS)//
				.persistencePriority(VERY_HIGH)//
				.text("Total Off-Grid time")), //

		/**
		 * Cumulated Off-Grid Genset time.
		 *
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Cumulated Seconds
		 * </ul>
		 */
		GRID_MODE_OFF_GRID_GENSET_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS)//
				.persistencePriority(VERY_HIGH)//
				.text("Total Off-Grid Genset time")), //

		/**
		 * Grid Genset: Active Power.
		 *
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * </ul>
		 */
		GRID_GENSET_ACTIVE_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)), //

		/**
		 * Grid Genset: Active Power L1.
		 *
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * </ul>
		 */
		GRID_GENSET_ACTIVE_POWER_L1(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)),

		/**
		 * Grid Genset: Active Power L2.
		 *
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * </ul>
		 */
		GRID_GENSET_ACTIVE_POWER_L2(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)),

		/**
		 * Grid Genset: Active Power L3.
		 *
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * </ul>
		 */
		GRID_GENSET_ACTIVE_POWER_L3(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)),

		/**
		 * Energy Storage System (ESS): Max Apparent Power.
		 *
		 * <ul>
		 * <li>Interface: Max Apparent Power (origin: SymmetricEss))
		 * <li>Type: Integer
		 * <li>Unit: VA
		 * </ul>
		 */
		ESS_MAX_APPARENT_POWER(Doc.of(INTEGER)//
				.unit(VOLT_AMPERE)//
				.persistencePriority(VERY_HIGH)), //

		/**
		 * Energy Storage System (ESS): Active Charge Energy.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss)
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		ESS_ACTIVE_CHARGE_ENERGY(Doc.of(LONG)//
				.unit(CUMULATED_WATT_HOURS)//
				.persistencePriority(VERY_HIGH)//
				.text("Accumulated electrical energy of the AC-side storage charging incl. excess PV generation at the hybrid inverter")), //

		/**
		 * Energy Storage System (ESS): Active Discharge Energy.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss)
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		ESS_ACTIVE_DISCHARGE_ENERGY(Doc.of(LONG)//
				.unit(CUMULATED_WATT_HOURS)//
				.persistencePriority(VERY_HIGH)//
				.text("Accumulated electrical energy of the AC-side storage discharge incl. excess PV generation at the hybrid inverter")), //

		/**
		 * Energy Storage System (ESS): DC Discharge Energy.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: HybridEss)
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		ESS_DC_DISCHARGE_ENERGY(Doc.of(LONG)//
				.unit(CUMULATED_WATT_HOURS)//
				.persistencePriority(VERY_HIGH)//
				.text("Accumulated DC electrical energy of the storage discharging")), //

		/**
		 * Energy Storage System (ESS): DC Charge Energy.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: HybridEss)
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		ESS_DC_CHARGE_ENERGY(Doc.of(LONG)//
				.unit(CUMULATED_WATT_HOURS)//
				.persistencePriority(VERY_HIGH)//
				.text("Accumulated DC electrical energy of the storage charging")), //

		/**
		 * Grid: Buy-from-grid Energy ("Production").
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter)
		 * <li>Type: Integer
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		GRID_BUY_ACTIVE_ENERGY(Doc.of(LONG)//
				.unit(CUMULATED_WATT_HOURS)//
				.persistencePriority(VERY_HIGH)//
				.text("Accumulated electrical energy of grid consumption")), //

		/**
		 * Grid: Sell-to-grid Energy ("Consumption").
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter)
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		GRID_SELL_ACTIVE_ENERGY(Doc.of(LONG)//
				.unit(CUMULATED_WATT_HOURS)//
				.persistencePriority(VERY_HIGH)//
				.text("Accumulated electrical energy of grid feed-in")), //

		/**
		 * Production: Energy.
		 *
		 * <ul>
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		PRODUCTION_ACTIVE_ENERGY(Doc.of(LONG)//
				.unit(CUMULATED_WATT_HOURS)//
				.persistencePriority(VERY_HIGH)//
				.text("Accumulated electrical energy of DC- and AC-side generators, e.g. photovoltaics")), //

		/**
		 * Production: AC Energy.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter)
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		PRODUCTION_AC_ACTIVE_ENERGY(Doc.of(LONG)//
				.unit(CUMULATED_WATT_HOURS)//
				.persistencePriority(VERY_HIGH)//
				.text("Accumulated electrical energy of AC-side generators")), //

		/**
		 * Production: DC Energy.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: EssDcCharger)
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		PRODUCTION_DC_ACTIVE_ENERGY(Doc.of(LONG)//
				.unit(CUMULATED_WATT_HOURS)//
				.persistencePriority(VERY_HIGH)//
				.text("Accumulated electrical energy of DC-side generators")), //

		/**
		 * Consumption: Energy.
		 *
		 * <ul>
		 * <li>Interface: Sum (origin: ElectricityMeter)
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		CONSUMPTION_ACTIVE_ENERGY(Doc.of(LONG)//
				.unit(CUMULATED_WATT_HOURS)//
				.persistencePriority(VERY_HIGH)//
				.text("Accumulated electrical energy consumption")), //

		/**
		 * Production to Consumption: Power.
		 * 
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: only positive
		 * </ul>
		 */
		PRODUCTION_TO_CONSUMPTION_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)), //

		/**
		 * Production to Consumption: Energy.
		 * 
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		PRODUCTION_TO_CONSUMPTION_ENERGY(Doc.of(LONG)//
				.unit(CUMULATED_WATT_HOURS)//
				.persistencePriority(VERY_HIGH)), //

		/**
		 * Production to Grid: Power.
		 * 
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: only positive
		 * </ul>
		 */
		PRODUCTION_TO_GRID_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)), //

		/**
		 * Production to Grid: Energy.
		 * 
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		PRODUCTION_TO_GRID_ENERGY(Doc.of(LONG)//
				.unit(CUMULATED_WATT_HOURS)//
				.persistencePriority(VERY_HIGH)), //

		/**
		 * Production to ESS: Power.
		 * 
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Integer
		 * <li>Unit: Wh_W
		 * <li>Range: only positive
		 * </ul>
		 */
		PRODUCTION_TO_ESS_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)), //

		/**
		 * Production to ESS: Energy.
		 * 
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		PRODUCTION_TO_ESS_ENERGY(Doc.of(LONG)//
				.unit(CUMULATED_WATT_HOURS)//
				.persistencePriority(VERY_HIGH)), //

		/**
		 * Grid to Consumption: Power.
		 * 
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: only positive
		 * </ul>
		 */
		GRID_TO_CONSUMPTION_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)), //

		/**
		 * Grid to Consumption: Energy.
		 * 
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		GRID_TO_CONSUMPTION_ENERGY(Doc.of(LONG)//
				.unit(CUMULATED_WATT_HOURS)//
				.persistencePriority(VERY_HIGH)), //

		/**
		 * ESS to Consumption: Power.
		 * 
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: only positive
		 * </ul>
		 */
		ESS_TO_CONSUMPTION_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)), //

		/**
		 * ESS to Consumption: Energy.
		 * 
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		ESS_TO_CONSUMPTION_ENERGY(Doc.of(LONG)//
				.unit(CUMULATED_WATT_HOURS)//
				.persistencePriority(VERY_HIGH)), //

		/**
		 * Grid to ESS: Power.
		 * 
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: discharge-to-grid negative, charge-from-grid positive
		 * </ul>
		 */
		GRID_TO_ESS_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_HIGH)), //

		/**
		 * Grid to ESS: Energy.
		 * 
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		GRID_TO_ESS_ENERGY(Doc.of(LONG)//
				.unit(CUMULATED_WATT_HOURS)//
				.persistencePriority(VERY_HIGH)), //

		/**
		 * ESS to Grid: Energy.
		 *
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Long
		 * <li>Unit: Wh_Σ
		 * </ul>
		 */
		ESS_TO_GRID_ENERGY(Doc.of(LONG)//
				.unit(CUMULATED_WATT_HOURS)//
				.persistencePriority(VERY_HIGH)), //

		/**
		 * Is there any Component Info/Warning/Fault that is getting ignored/hidden
		 * because of the 'ignoreStateComponents' configuration setting?.
		 */
		HAS_IGNORED_COMPONENT_STATES(Doc.of(INFO)//
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
				.channel(118, ChannelId.ESS_CAPACITY, ModbusType.FLOAT32) //
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
	 * Gets the Channel for {@link ChannelId#UNMANAGED_PRODUCTION_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getUnmanagedProductionActivePowerChannel() {
		return this.channel(ChannelId.UNMANAGED_PRODUCTION_ACTIVE_POWER);
	}

	/**
	 * Gets the Unmanaged Production Active Power in [W]. See
	 * {@link ChannelId#UNMANAGED_PRODUCTION_ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getUnmanagedProductionActivePower() {
		return this.getUnmanagedProductionActivePowerChannel().value();
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
	 * Gets the Channel for {@link ChannelId#GRID_GENSET_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridGensetActivePowerChannel() {
		return this.channel(ChannelId.GRID_GENSET_ACTIVE_POWER);
	}

	/**
	 * Gets the Grid Genset Active Power in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridGensetActivePower() {
		return this.getGridGensetActivePowerChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_GENSET_ACTIVE_POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridGensetActivePowerL1Channel() {
		return this.channel(ChannelId.GRID_GENSET_ACTIVE_POWER_L1);
	}

	/**
	 * Gets the Grid Genset Active Power L1 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridGensetActivePowerL1() {
		return this.getGridGensetActivePowerL1Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_GENSET_ACTIVE_POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridGensetActivePowerL2Channel() {
		return this.channel(ChannelId.GRID_GENSET_ACTIVE_POWER_L2);
	}

	/**
	 * Gets the Grid Genset Active Power L2 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridGensetActivePowerL2() {
		return this.getGridGensetActivePowerL2Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_GENSET_ACTIVE_POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridGensetActivePowerL3Channel() {
		return this.channel(ChannelId.GRID_GENSET_ACTIVE_POWER_L3);
	}

	/**
	 * Gets the Grid Genset Active Power L3 in [W].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridGensetActivePowerL3() {
		return this.getGridGensetActivePowerL3Channel().value();
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
	 * Gets the Channel for {@link ChannelId#HAS_IGNORED_COMPONENT_STATES}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getHasIgnoredComponentStatesChannel() {
		return this.channel(ChannelId.HAS_IGNORED_COMPONENT_STATES);
	}
}
