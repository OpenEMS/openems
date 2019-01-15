package io.openems.edge.common.sum;

import io.openems.common.OpenemsConstants;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;

/**
 * Enables access to sum/average data.
 */
public interface Sum extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * Ess: Average State of Charge
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: Ess)
		 * <li>Type: Integer
		 * <li>Unit: %
		 * <li>Range: 0..100
		 * </ul>
		 */
		ESS_SOC(new Doc().type(OpenemsType.INTEGER).unit(Unit.PERCENT)),
		/**
		 * Ess: Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: @see {@link SymmetricEss})
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		ESS_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(OpenemsConstants.POWER_DOC_TEXT)),
		/**
		 * Grid: Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: @see {@link SymmetricMeter}))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		GRID_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(OpenemsConstants.POWER_DOC_TEXT)),
		/**
		 * Grid: Minimum Ever Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: @see {@link SymmetricMeter}))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values or '0'
		 * </ul>
		 */
		GRID_MIN_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Grid: Maximum Ever Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: @see {@link SymmetricMeter}))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		GRID_MAX_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Production: Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: Meter Symmetric and ESS DC Charger)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * </ul>
		 */
		PRODUCTION_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Production: AC Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: Meter Symmetric)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * </ul>
		 */
		PRODUCTION_AC_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Production: DC Actual Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: ESS DC Charger)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * </ul>
		 */
		PRODUCTION_DC_ACTUAL_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Production: Maximum Ever Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: @see {@link SymmetricMeter}))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		PRODUCTION_MAX_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Production: Maximum Ever AC Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: @see {@link SymmetricMeter}))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		PRODUCTION_MAX_AC_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Production: Maximum Ever DC Actual Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: @see {@link EssDcCharger}))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		PRODUCTION_MAX_DC_ACTUAL_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Consumption: Active Power
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
		CONSUMPTION_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Consumption: Maximum Ever Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: @see {@link SymmetricMeter}))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		CONSUMPTION_MAX_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),

		/**
		 * Gridmode
		 * 
		 * <ul>
		 * <li>Interface: Gridmode (origin: @see {@link SymmetricEss}))
		 * <li>Type: Integer
		 * <li>Values: '0' = UNDEFINED, '1' = ON GRID, '2' = OFF GRID
		 * </ul>
		 */
		GRID_MODE(new Doc() //
				.type(OpenemsType.INTEGER).options(GridMode.values())),

		/**
		 * Max Apparent Power
		 * 
		 * <ul>
		 * <li>Interface: Max Apparent Power (origin: @see {@link SymmetricEss}))
		 * <li>Type: Integer
		 * <li>Unit: VA
		 * </ul>
		 */
		ESS_MAX_APPARENT_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	public static ModbusSlaveNatureTable getModbusSlaveNatureTable() {
		return ModbusSlaveNatureTable.of(Sum.class, 220) //
				.channel(0, OpenemsComponent.ChannelId.STATE, ModbusType.UINT16) //
				.channel(1, ChannelId.ESS_SOC, ModbusType.UINT16) //
				.channel(2, ChannelId.ESS_ACTIVE_POWER, ModbusType.FLOAT32) //
				.float32Reserved(4) // ChannelId.ESS_MIN_ACTIVE_POWER
				.float32Reserved(6) // ChannelId.ESS_MAX_ACTIVE_POWER
				.float32Reserved(8) // ChannelId.ESS_REACTIVE_POWER
				.float32Reserved(10) // ChannelId.ESS_MIN_REACTIVE_POWER
				.float32Reserved(12) // ChannelId.ESS_MAX_REACTIVE_POWER
				.channel(14, ChannelId.GRID_ACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(16, ChannelId.GRID_MIN_ACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(18, ChannelId.GRID_MAX_ACTIVE_POWER, ModbusType.FLOAT32) //
				.float32Reserved(20) // ChannelId.GRID_REACTIVE_POWER
				.float32Reserved(22) // ChannelId.GRID_MIN_REACTIVE_POWER
				.float32Reserved(24) // ChannelId.GRID_MAX_REACTIVE_POWER
				.channel(26, ChannelId.PRODUCTION_ACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(28, ChannelId.PRODUCTION_MAX_ACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(30, ChannelId.PRODUCTION_AC_ACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(32, ChannelId.PRODUCTION_MAX_AC_ACTIVE_POWER, ModbusType.FLOAT32) //
				.float32Reserved(34) // ChannelId.PRODUCTION_AC_REACTIVE_POWER
				.float32Reserved(36) // ChannelId.PRODUCTION_MAX_AC_REACTIVE_POWER
				.channel(38, ChannelId.PRODUCTION_DC_ACTUAL_POWER, ModbusType.FLOAT32) //
				.channel(40, ChannelId.PRODUCTION_MAX_DC_ACTUAL_POWER, ModbusType.FLOAT32) //
				.channel(42, ChannelId.CONSUMPTION_ACTIVE_POWER, ModbusType.FLOAT32) //
				.channel(44, ChannelId.CONSUMPTION_MAX_ACTIVE_POWER, ModbusType.FLOAT32) //
				.float32Reserved(46) // ChannelId.CONSUMPTION_REACTIVE_POWER
				.float32Reserved(48) // ChannelId.CONSUMPTION_MAX_REACTIVE_POWER
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

}
