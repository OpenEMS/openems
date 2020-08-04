package io.openems.edge.battery.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;

@ProviderType
public interface Battery extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Indicates that the battery has started and is ready for charging/discharging.
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Boolean
		 * </ul>
		 */
		// TODO: why can this not be handled using 'STATE'?
		READY_FOR_WORKING(Doc.of(OpenemsType.BOOLEAN)),

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
		SOC(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),

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
		SOH(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),

		/**
		 * Voltage of battery.
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * </ul>
		 */
		VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),

		/**
		 * Current of battery.
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: A
		 * </ul>
		 */
		CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)),

		/**
		 * Capacity of battery.
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * </ul>
		 */
		CAPACITY(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS)),

		/**
		 * Maximal voltage for charging.
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: V
		 * </ul>
		 */
		CHARGE_MAX_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),

		/**
		 * Maximum current for charging.
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: A
		 * </ul>
		 */
		CHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)),

		/**
		 * Minimal voltage for discharging.
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: V
		 * </ul>
		 */
		DISCHARGE_MIN_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)),

		/**
		 * Maximum current for discharging.
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: A
		 * </ul>
		 */
		DISCHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)),

		/**
		 * Minimal Cell Temperature.
		 *
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: Celsius
		 * <li>Range: (-50)..100
		 * </ul>
		 */
		MIN_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),

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
		MAX_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),

		/**
		 * Minimal cell voltage.
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * </ul>
		 */
		MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)),

		/**
		 * Maximum cell voltage.
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * </ul>
		 */
		MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT));

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
	 * Gets the State of Charge in [%], range 0..100 %.
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getSoc() {
		return this.channel(ChannelId.SOC);
	}

	/**
	 * Gets the State of Health in [%], range 0..100 %.
	 *
	 * @return the Channel
	 */
	default Channel<Integer> getSoh() {
		return this.channel(ChannelId.SOH);
	}

	/**
	 * Gets the capacity.
	 *
	 * @return the Channel
	 */
	default Channel<Integer> getCapacity() {
		return this.channel(ChannelId.CAPACITY);
	}

	/**
	 * Gets the min voltage for discharging.
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getDischargeMinVoltage() {
		return this.channel(ChannelId.DISCHARGE_MIN_VOLTAGE);
	}

	/**
	 * Gets the max current for discharging.
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getDischargeMaxCurrent() {
		return this.channel(ChannelId.DISCHARGE_MAX_CURRENT);
	}

	/**
	 * Gets the max voltage for charging.
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getChargeMaxVoltage() {
		return this.channel(ChannelId.CHARGE_MAX_VOLTAGE);
	}

	/**
	 * Gets the max current for charging.
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getChargeMaxCurrent() {
		return this.channel(ChannelId.CHARGE_MAX_CURRENT);
	}

	/**
	 * Gets the Minimal Cell Temperature in [degC], range (-50)..100.
	 *
	 * @return the Channel
	 */
	default Channel<Integer> getMinCellTemperature() {
		return this.channel(ChannelId.MIN_CELL_TEMPERATURE);
	}

	/**
	 * Gets the Maximum Cell Temperature in [degC], range (-50)..100.
	 *
	 * @return the Channel
	 */
	default Channel<Integer> getMaxCellTemperature() {
		return this.channel(ChannelId.MAX_CELL_TEMPERATURE);
	}

	/**
	 * Gets the indicator if ready to charge/discharge.
	 * 
	 * @return the Channel
	 */
	default Channel<Boolean> getReadyForWorking() {
		return this.channel(ChannelId.READY_FOR_WORKING);
	}

	/**
	 * Gets the total voltage of this battery system.
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getVoltage() {
		return this.channel(ChannelId.VOLTAGE);
	}

	/**
	 * Gets the total current of this battery system.
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getCurrent() {
		return this.channel(ChannelId.CURRENT);
	}

	/**
	 * Gets the minimal cell voltage of this battery.
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getMinCellVoltage() {
		return this.channel(ChannelId.MIN_CELL_VOLTAGE);
	}

	/**
	 * Gets the maximum cell voltage of this battery.
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getMaxCellVoltage() {
		return this.channel(ChannelId.MAX_CELL_VOLTAGE);
	}
}
