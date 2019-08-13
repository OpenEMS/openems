package io.openems.edge.common.sum;

import io.openems.common.OpenemsConstants;
import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;

/**
 * Enables access to sum/average data.
 */
public interface Sum extends OpenemsComponent {

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
				.unit(Unit.PERCENT)),
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
				.text(OpenemsConstants.POWER_DOC_TEXT)),
		
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
				.unit(Unit.WATT_HOURS)), //
		
		/**
		 * Grid: Active Power.
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricMeter))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		GRID_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(OpenemsConstants.POWER_DOC_TEXT)),
		/**
		 * Grid: Minimum Ever Active Power.
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricMeter))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values or '0'
		 * </ul>
		 */
		GRID_MIN_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Grid: Maximum Ever Active Power.
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricMeter)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		GRID_MAX_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Production: Active Power.
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricMeter and ESS DC Charger)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * </ul>
		 */
		PRODUCTION_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Production: AC Active Power.
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricMeter)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * </ul>
		 */
		PRODUCTION_AC_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
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
				.unit(Unit.WATT)),
		/**
		 * Production: Maximum Ever Active Power.
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricMeter))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		PRODUCTION_MAX_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Production: Maximum Ever AC Active Power.
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricMeter))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		PRODUCTION_MAX_AC_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Production: Maximum Ever DC Actual Power.
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: EssDcCharger}))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		PRODUCTION_MAX_DC_ACTUAL_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
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
				.unit(Unit.WATT)),
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
				.unit(Unit.WATT)),
		/**
		 * Grid-Mode.
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss))
		 * <li>Type: Integer
		 * <li>Values: '0' = UNDEFINED, '1' = ON GRID, '2' = OFF GRID
		 * </ul>
		 */
		GRID_MODE(Doc.of(GridMode.values())),

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
				.unit(Unit.VOLT_AMPERE)),
		/**
		 * Ess: Active Charge Energy.
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss)
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		ESS_ACTIVE_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),
		/**
		 * Ess: Active Discharge Energy.
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricEss)
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		ESS_ACTIVE_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),
		/**
		 * Grid: Buy-from-grid Energy ("Production").
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricMeter)
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		GRID_BUY_ACTIVE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),
		/**
		 * Grid: Sell-to-grid Energy ("Consumption").
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricMeter)
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		GRID_SELL_ACTIVE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),
		/**
		 * Production: Energy.
		 * 
		 * <ul>
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		PRODUCTION_ACTIVE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),
		/**
		 * Production: AC Energy.
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricMeter)
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		PRODUCTION_AC_ACTIVE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),
		/**
		 * Production: DC Energy.
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: EssDcCharger)
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		// TODO rename to Actual_Energy
		PRODUCTION_DC_ACTIVE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),
		/**
		 * Consumption: Energy.
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: SymmetricMeter)
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		CONSUMPTION_ACTIVE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(Sum.class, accessMode, 220) //
				.channel(0, ChannelId.ESS_SOC, ModbusType.UINT16) //
				.channel(1, ChannelId.ESS_ACTIVE_POWER, ModbusType.FLOAT32) //
				.float32Reserved(3) // ChannelId.ESS_MIN_ACTIVE_POWER
				.float32Reserved(5) // ChannelId.ESS_MAX_ACTIVE_POWER
				.float32Reserved(7) // ChannelId.ESS_REACTIVE_POWER
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
				.channel(31, ChannelId.PRODUCTION_MAX_AC_ACTIVE_POWER, ModbusType.FLOAT32) //
				.float32Reserved(33) // ChannelId.PRODUCTION_AC_REACTIVE_POWER
				.float32Reserved(35) // ChannelId.PRODUCTION_MAX_AC_REACTIVE_POWER
				.channel(37, ChannelId.PRODUCTION_DC_ACTUAL_POWER, ModbusType.FLOAT32) //
				.channel(39, ChannelId.PRODUCTION_MAX_DC_ACTUAL_POWER, ModbusType.FLOAT32) //
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
				.build();
	}

	public default Channel<Integer> getEssSoc() {
		return this.channel(ChannelId.ESS_SOC);
	}

	public default Channel<Integer> getEssActivePower() {
		return this.channel(ChannelId.ESS_ACTIVE_POWER);
	}

	public default Channel<Integer> getEssMaxApparentPower() {
		return this.channel(ChannelId.ESS_MAX_APPARENT_POWER);
	}
	
	public default Channel<Integer> getEssCapacity() {
		return this.channel(ChannelId.ESS_CAPACITY);
	}

	public default Channel<Integer> getGridActivePower() {
		return this.channel(ChannelId.GRID_ACTIVE_POWER);
	}

	public default Channel<Integer> getGridMinActivePower() {
		return this.channel(ChannelId.GRID_MIN_ACTIVE_POWER);
	}

	public default Channel<Integer> getGridMaxActivePower() {
		return this.channel(ChannelId.GRID_MAX_ACTIVE_POWER);
	}

	public default Channel<Integer> getProductionActivePower() {
		return this.channel(ChannelId.PRODUCTION_ACTIVE_POWER);
	}

	public default Channel<Integer> getProductionAcActivePower() {
		return this.channel(ChannelId.PRODUCTION_AC_ACTIVE_POWER);
	}

	public default Channel<Integer> getProductionDcActualPower() {
		return this.channel(ChannelId.PRODUCTION_DC_ACTUAL_POWER);
	}

	public default Channel<Integer> getProductionMaxActivePower() {
		return this.channel(ChannelId.PRODUCTION_MAX_ACTIVE_POWER);
	}

	public default Channel<Integer> getProductionMaxAcActivePower() {
		return this.channel(ChannelId.PRODUCTION_MAX_AC_ACTIVE_POWER);
	}

	public default Channel<Integer> getProductionMaxDcActualPower() {
		return this.channel(ChannelId.PRODUCTION_MAX_DC_ACTUAL_POWER);
	}

	public default Channel<Integer> getConsumptionActivePower() {
		return this.channel(ChannelId.CONSUMPTION_ACTIVE_POWER);
	}

	public default Channel<Integer> getConsumptionMaxActivePower() {
		return this.channel(ChannelId.CONSUMPTION_MAX_ACTIVE_POWER);
	}

	public default Channel<Integer> getGridMode() {
		return this.channel(ChannelId.GRID_MODE);
	}

	public default Channel<Long> getEssActiveChargeEnergy() {
		return this.channel(ChannelId.ESS_ACTIVE_CHARGE_ENERGY);
	}

	public default Channel<Long> getEssActiveDischargeEnergy() {
		return this.channel(ChannelId.ESS_ACTIVE_DISCHARGE_ENERGY);
	}

	public default Channel<Long> getGridBuyActiveEnergy() {
		return this.channel(ChannelId.GRID_BUY_ACTIVE_ENERGY);
	}

	public default Channel<Long> getGridSellActiveEnergy() {
		return this.channel(ChannelId.GRID_SELL_ACTIVE_ENERGY);
	}

	public default Channel<Long> getProductionActiveEnergy() {
		return this.channel(ChannelId.PRODUCTION_ACTIVE_ENERGY);
	}

	public default Channel<Long> getProductionAcActiveEnergy() {
		return this.channel(ChannelId.PRODUCTION_AC_ACTIVE_ENERGY);
	}

	public default Channel<Long> getProductionDcActiveEnergy() {
		return this.channel(ChannelId.PRODUCTION_DC_ACTIVE_ENERGY);
	}

	public default Channel<Long> getConsumptionActiveEnergy() {
		return this.channel(ChannelId.CONSUMPTION_ACTIVE_ENERGY);
	}
}
