package io.openems.edge.pvinverter.sma;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.pvinverter.sunspec.SunSpecPvInverter;

public interface PvInverterSmaSunnyTripower extends SunSpecPvInverter, ManagedSymmetricPvInverter, ElectricityMeter,
		ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		
		COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
				.text("Communication to SMA inverterfailed. "
						+ "Please check the network connection and the status of the inverter")), //		

		/**
		 * Number of Modules (DC-Inputs).
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: None
		 * <li>
		 * </ul>
		 */
		N(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Scale Factor for DC current.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: None
		 * <li>
		 * </ul>
		 */
		DCA_SF(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * Scale Factor for DC voltage.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: None
		 * <li>
		 * </ul>
		 */
		DCV_SF(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * Scale Factor of DC power.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: None
		 * <li>
		 * </ul>
		 */
		DCW_SF(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * Scale Factor for DC energy. Usually 1.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: None
		 * <li>
		 * </ul>
		 */
		DCWH_SF(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * Internal Value String 1 DC-Current. Scale Factor not applied.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>
		 * </ul>
		 */
		ST1_DC_CURRENT_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.LOW)),

		ST2_DC_CURRENT_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.LOW)),

		ST3_DC_CURRENT_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.LOW)),

		ST4_DC_CURRENT_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.LOW)),

		ST5_DC_CURRENT_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.LOW)),

		ST6_DC_CURRENT_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.LOW)),

		ST7_DC_CURRENT_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.LOW)),

		ST8_DC_CURRENT_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.LOW)),

		ST9_DC_CURRENT_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.LOW)),

		ST10_DC_CURRENT_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.LOW)),
		ST11_DC_CURRENT_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.LOW)),

		ST12_DC_CURRENT_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * String 1 DC-Energy.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: WattHours
		 * <li>
		 * </ul>
		 */
		ST1_DC_ENERGY_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),

		ST2_DC_ENERGY_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),

		ST3_DC_ENERGY_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),

		ST4_DC_ENERGY_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),

		ST5_DC_ENERGY_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),

		ST6_DC_ENERGY_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),

		ST7_DC_ENERGY_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),

		ST8_DC_ENERGY_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),

		ST9_DC_ENERGY_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),

		ST10_DC_ENERGY_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),

		ST11_DC_ENERGY_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),

		ST12_DC_ENERGY_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * String 1 DC-Power.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		ST1_DC_POWER_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),

		ST2_DC_POWER_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),

		ST3_DC_POWER_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),
		ST4_DC_POWER_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),
		ST5_DC_POWER_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),
		ST6_DC_POWER_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),
		ST7_DC_POWER_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),
		ST8_DC_POWER_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),
		ST9_DC_POWER_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),
		ST10_DC_POWER_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),
		ST11_DC_POWER_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),
		ST12_DC_POWER_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * String 1 DC-Voltage.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: V
		 * <li>
		 * </ul>
		 */
		ST1_DC_VOLTAGE_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.LOW)),

		ST2_DC_VOLTAGE_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.LOW)),

		ST3_DC_VOLTAGE_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.LOW)),

		ST4_DC_VOLTAGE_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.LOW)),

		ST5_DC_VOLTAGE_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.LOW)),

		ST6_DC_VOLTAGE_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.LOW)),

		ST7_DC_VOLTAGE_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.LOW)),

		ST8_DC_VOLTAGE_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.LOW)),

		ST9_DC_VOLTAGE_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.LOW)),

		ST10_DC_VOLTAGE_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.LOW)),

		ST11_DC_VOLTAGE_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.LOW)),

		ST12_DC_VOLTAGE_INTERNAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * String 1 DC-Current.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>
		 * </ul>
		 */
		ST1_DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 1 DC-Energy.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: WattHours
		 * <li>
		 * </ul>
		 */
		ST1_DC_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 1 DC-Power.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		ST1_DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 1 DC-Voltage.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: V
		 * <li>
		 * </ul>
		 */
		ST1_DC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 2 DC-Current.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>
		 * </ul>
		 */
		ST2_DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 2 DC-Energy.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: WattHours
		 * <li>
		 * </ul>
		 */
		ST2_DC_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 2 DC-Power.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		ST2_DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 2 DC-Voltage.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: V
		 * <li>
		 * </ul>
		 */
		ST2_DC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 3 DC-Current.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>
		 * </ul>
		 */
		ST3_DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 3 DC-Energy.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: WattHours
		 * <li>
		 * </ul>
		 */
		ST3_DC_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 3 DC-Power.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		ST3_DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 3 DC-Voltage.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: V
		 * <li>
		 * </ul>
		 */
		ST3_DC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 4 DC-Current.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>
		 * </ul>
		 */
		ST4_DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 4 DC-Energy.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: WattHours
		 * <li>
		 * </ul>
		 */
		ST4_DC_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 4 DC-Power.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		ST4_DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 4 DC-Voltage.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: V
		 * <li>
		 * </ul>
		 */
		ST4_DC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 5 DC-Current.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>
		 * </ul>
		 */
		ST5_DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 5 DC-Energy.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: WattHours
		 * <li>
		 * </ul>
		 */
		ST5_DC_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 5 DC-Power.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		ST5_DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 5 DC-Voltage.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: V
		 * <li>
		 * </ul>
		 */
		ST5_DC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 6 DC-Current.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>
		 * </ul>
		 */
		ST6_DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 6 DC-Energy.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: WattHours
		 * <li>
		 * </ul>
		 */
		ST6_DC_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 6 DC-Power.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		ST6_DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 6 DC-Voltage.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: V
		 * <li>
		 * </ul>
		 */
		ST6_DC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 7 DC-Current.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>
		 * </ul>
		 */
		ST7_DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 7 DC-Energy.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: WattHours
		 * <li>
		 * </ul>
		 */
		ST7_DC_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 7 DC-Power.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		ST7_DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 7 DC-Voltage.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: V
		 * <li>
		 * </ul>
		 */
		ST7_DC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 8 DC-Current.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>
		 * </ul>
		 */
		ST8_DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 8 DC-Energy.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: WattHours
		 * <li>
		 * </ul>
		 */
		ST8_DC_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 8 DC-Power.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		ST8_DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 8 DC-Voltage.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: V
		 * <li>
		 * </ul>
		 */
		ST8_DC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 9 DC-Current.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>
		 * </ul>
		 */
		ST9_DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 9 DC-Energy.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: WattHours
		 * <li>
		 * </ul>
		 */
		ST9_DC_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 9 DC-Power.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		ST9_DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 9 DC-Voltage.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: V
		 * <li>
		 * </ul>
		 */
		ST9_DC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 10 DC-Current.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>
		 * </ul>
		 */
		ST10_DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 10 DC-Energy.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: WattHours
		 * <li>
		 * </ul>
		 */
		ST10_DC_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 10 DC-Power.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		ST10_DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 10 DC-Voltage.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: V
		 * <li>
		 * </ul>
		 */
		ST10_DC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 11 DC-Current.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>
		 * </ul>
		 */
		ST11_DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 11 DC-Energy.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: WattHours
		 * <li>
		 * </ul>
		 */
		ST11_DC_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 11 DC-Power.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		ST11_DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 11 DC-Voltage.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: V
		 * <li>
		 * </ul>
		 */
		ST11_DC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 12 DC-Current.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>
		 * </ul>
		 */
		ST12_DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 12 DC-Energy.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: WattHours
		 * <li>
		 * </ul>
		 */
		ST12_DC_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 12 DC-Power.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		ST12_DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 12 DC-Voltage.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: V
		 * <li>
		 * </ul>
		 */
		ST12_DC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
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
	 * Gets the Channel for {@link ChannelId#COMMUNICATION_FAILED}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getCommunicationFailedChannel() {
		return this.channel(ChannelId.COMMUNICATION_FAILED);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#USER_ACCESS_DENIED} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setCommunicationFailed(boolean value) {
		this.getCommunicationFailedChannel().setNextValue(value);
	}

}
