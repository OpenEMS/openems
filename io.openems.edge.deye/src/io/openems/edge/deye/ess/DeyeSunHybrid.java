package io.openems.edge.deye.ess;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.deye.enums.EmsPowerMode;
import io.openems.edge.deye.enums.EnableDisable;
import io.openems.edge.deye.enums.EnergyManagementModel;
import io.openems.edge.deye.enums.GridStandard;
import io.openems.edge.deye.enums.InverterRunState;
import io.openems.edge.deye.enums.LimitControlFunction;
import io.openems.edge.deye.enums.RemoteLockState;
import io.openems.edge.deye.enums.WorkState;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.timedata.api.TimedataProvider;

public interface DeyeSunHybrid
		extends ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler, ModbusSlave, TimedataProvider {

	/**
	 * Gets the Modbus Unit-ID.
	 *
	 * @return the Unit-ID
	 */
	public Integer getUnitId();

	/**
	 * Gets the Modbus-Bridge Component-ID, i.e. "modbus0".
	 *
	 * @return the Component-ID
	 */
	public String getModbusBridgeId();

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// Internal Channels
		MAX_AC_EXPORT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		MAX_AC_IMPORT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //

		EMS_POWER_MODE(Doc.of(EmsPowerMode.values()) //
				.accessMode(AccessMode.READ_WRITE)), //

		TARGET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).persistencePriority(PersistencePriority.HIGH)), //

		TARGET_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) // mrdomek: OpenEMS best-practice: current in mA as Integer
				.persistencePriority(PersistencePriority.HIGH)), //

		// EnumReadChannels
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.HIGH) //
				.accessMode(AccessMode.READ_ONLY)),
		SURPLUS_FEED_IN_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //

		ACTIVE_POWER_REGULATION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)), //

		REACTIVE_POWER_REGULATION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)), //

		APPARENT_POWER_REGULATION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)), //

		RATED_POWER(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //

		REMOTE_LOCK_STATE(Doc.of(RemoteLockState.values()) //
				.accessMode(AccessMode.READ_WRITE)), //

		// Gen Port Use Channels
		// AC 1/28/2024
		SET_GRID_LOAD_OFF_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT) //
				.accessMode(AccessMode.WRITE_ONLY)), //

		ENABLE_SWITCH_STATE(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_WRITE)), //

		SWITCH_STATE(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_ONLY)), //

		FACTORY_RESET_STATE(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_WRITE)), //

		SELF_CHECKING_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_ONLY)), //

		ISLAND_PROTECTION_ENABLE(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_ONLY)), //

		SET_MPPT_NUMBER(Doc.of(EnableDisable.values()) // Enables or disables MPPT tracker
				.accessMode(AccessMode.WRITE_ONLY)), //
		MPPT_NUMBER(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_ONLY)), //

		// GFDI (Ground Fault Detection Interrupter)
		GFDI_STATE(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_WRITE)), //

		// RISO (Residual Insulation Monitoring)
		RISO_STATE(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_WRITE)), //

		SET_GRID_STANDARD(Doc.of(GridStandard.values()) // what is this?
				.accessMode(AccessMode.WRITE_ONLY)), //
		GRID_STANDARD(Doc.of(GridStandard.values()) // what is this?
				.accessMode(AccessMode.READ_ONLY)), //

		SELL_MODE_TIME_POINT_1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_4(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_5(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_6(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		// Sell Mode Power Limits (W)
		SELL_MODE_TIME_POINT_1_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_2_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_3_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_4_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_5_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_6_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //

		// Sell Mode Voltage Limits (V)
		SELL_MODE_TIME_POINT_1_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_2_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_3_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_4_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_5_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_6_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE)), //

		// Sell Mode Capacity Limits (Ah)
		SELL_MODE_TIME_POINT_1_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE_HOURS) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_2_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE_HOURS) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_3_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE_HOURS) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_4_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE_HOURS) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_5_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE_HOURS) //
				.accessMode(AccessMode.READ_WRITE)), //
		SELL_MODE_TIME_POINT_6_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE_HOURS) //
				.accessMode(AccessMode.READ_WRITE)), //

		// Bit 0 -> Charge from grid enabled, Bit 1 -> Charge from generator
		CHARGE_MODE_TIME_POINT_1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		CHARGE_MODE_TIME_POINT_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		CHARGE_MODE_TIME_POINT_3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		CHARGE_MODE_TIME_POINT_4(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		CHARGE_MODE_TIME_POINT_5(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		CHARGE_MODE_TIME_POINT_6(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		// BMS Metrics (read registers from inverter)
		BATTERY_TEMPERATURE(Doc.of(OpenemsType.INTEGER) // register 586
				.unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER) // register 587
				.unit(Unit.MILLIVOLT) // mrdomek: OpenEMS best-practice: voltage in mV as Integer
				.accessMode(AccessMode.READ_ONLY)),
		BATTERY_SOC(Doc.of(OpenemsType.INTEGER) // register 588
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),
		BATTERY_OUTPUT_POWER(Doc.of(OpenemsType.INTEGER) // register 590
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
		BATTERY_OUTPUT_CURRENT(Doc.of(OpenemsType.INTEGER) // register 591
				.unit(Unit.MILLIAMPERE) // mrdomek: OpenEMS best-practice: current in mA as Integer
				.accessMode(AccessMode.READ_ONLY)),
		BATTERY_CORRECTED_AH(Doc.of(OpenemsType.INTEGER) // register 592
				.unit(Unit.AMPERE_HOURS).accessMode(AccessMode.READ_ONLY)), //

		// Generator / grid charging settings (read-only)
		GENERATOR_MAX_OPERATING_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)), //
		GENERATOR_COOLING_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)), //
		GENERATOR_CHARGING_START_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT).accessMode(AccessMode.READ_ONLY)), //
		GENERATOR_CHARGING_START_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)), //
		GENERATOR_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) // mrdomek: OpenEMS best-practice: current in mA as Integer
				.accessMode(AccessMode.READ_ONLY)), //
		GRID_CHARGING_START_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) // mrdomek: OpenEMS best-practice: voltage in mV as Integer
				.accessMode(AccessMode.READ_ONLY)), //
		GRID_CHARGING_START_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)), //
		GRID_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) // mrdomek: OpenEMS best-practice: current in mA as Integer
				.accessMode(AccessMode.READ_ONLY)), //
		SET_GRID_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) // mrdomek: OpenEMS best-practice: current in mA as Integer
				.accessMode(AccessMode.WRITE_ONLY)), //
		GENERATOR_CHARGING_ENABLE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		SET_GENERATOR_CHARGING_ENABLE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		GRID_CHARGING_ENABLE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		SET_GRID_CHARGING_ENABLE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.WRITE_ONLY)), //

		// Power management & sell-mode settings (read-only)
		AC_COUPLE_FREQUENCY_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIHERTZ) // mrdomek: OpenEMS best-practice: frequency in mHz as Integer
				.accessMode(AccessMode.READ_ONLY)), //
		FORCE_GENERATOR_AS_LOAD(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		GENERATOR_INPUT_AS_LOAD_ENABLE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		SMARTLOAD_OFF_BATT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT).accessMode(AccessMode.READ_ONLY)), //
		SMARTLOAD_OFF_BATT_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)), //
		SMARTLOAD_ON_BATT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT).accessMode(AccessMode.READ_ONLY)), //
		SMARTLOAD_ON_BATT_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)), //
		OUTPUT_VOLTAGE_LEVEL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		MIN_SOLAR_POWER_TO_START_GENERATOR(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		GEN_GRID_SIGNAL_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		ENERGY_MANAGEMENT_MODEL(Doc.of(EnergyManagementModel.values()) //
				.accessMode(AccessMode.READ_WRITE)), //

		LIMIT_CONTROL_FUNCTION(Doc.of(LimitControlFunction.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		SET_LIMIT_CONTROL_FUNCTION(Doc.of(LimitControlFunction.values()) //
				.accessMode(AccessMode.WRITE_ONLY)), //

		LIMIT_MAX_GRID_OUTPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //

		EXTERNAL_CURRENT_SENSOR_CLAMP_PHASE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		SOLAR_SELL_MODE(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_WRITE)), //

		TIME_OF_USE_SELLING_ENABLED(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		TIME_OF_USE_MONDAY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		TIME_OF_USE_TUESDAY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		TIME_OF_USE_WEDNESDAY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		TIME_OF_USE_THURSDAY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		TIME_OF_USE_FRIDAY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		TIME_OF_USE_SATURDAY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		TIME_OF_USE_SUNDAY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //

		SET_TIME_OF_USE_SELLING_ENABLED(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_TIME_OF_USE_MONDAY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_TIME_OF_USE_TUESDAY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_TIME_OF_USE_WEDNESDAY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_TIME_OF_USE_THURSDAY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_TIME_OF_USE_FRIDAY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_TIME_OF_USE_SATURDAY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_TIME_OF_USE_SUNDAY(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.WRITE_ONLY)), //

		GRID_PHASE_SEQUENCE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		// Error / Warnings
		FLASH_CHIP_ERROR(Doc.of(Level.WARNING).text("Flash chip Error")), //
		TIME_ERROR(Doc.of(Level.WARNING).text("Time Error")), //
		EEPROM_ERROR(Doc.of(Level.WARNING).text("EEPROM Error")), //
		ARC_COMMUNICATION_STATE(Doc.of(OpenemsType.BOOLEAN).text("Arc pull communication sign")), //
		CAN_COMMUNICATION_ERROR(Doc.of(Level.WARNING).text("Parallel CAN communication")), //
		LITHIUM_BATTERY_RS485(Doc.of(Level.WARNING).text("Lithium battery interface RS485")), //
		LITHIUM_BATTERY_CAN(Doc.of(Level.INFO).text("Lithium battery interface CAN")), //
		KEY1234_ERROR(Doc.of(Level.WARNING).text("Key 1234 Error")), //
		LCD_INTERRUPT_STATE(Doc.of(OpenemsType.BOOLEAN).text("LCD interrupt status")), //
		FAN_FAILURE(Doc.of(Level.WARNING).text("Fan Error")), //
		GRID_PHASE_ERROR(Doc.of(Level.WARNING).text("Grid phase Error")), //

		ERROR_MESSAGE_1(new IntegerDoc().text("Fault information word 1").onChannelUpdate((self, newValue) -> {
			updateErrorState(self);
		})), //
		ERROR_MESSAGE_2(new IntegerDoc().text("Fault information word 2").onChannelUpdate((self, newValue) -> {
			updateErrorState(self);
		})), //
		ERROR_MESSAGE_3(new IntegerDoc().text("Fault information word 3").onChannelUpdate((self, newValue) -> {
			updateErrorState(self);
		})), //
		ERROR_MESSAGE_4(new IntegerDoc().text("Fault information word 4").onChannelUpdate((self, newValue) -> {
			updateErrorState(self);
		})), //

		F7(Doc.of(Level.FAULT).text("DC soft start error")), //
		F10(Doc.of(Level.FAULT).text("AUX power board error")), //
		F13(Doc.of(Level.FAULT).text("Working mode change")), //
		F18(Doc.of(Level.FAULT).text("Hardware AC overcurrent")), //
		F20(Doc.of(Level.FAULT).text("Hardware DC overcurrent")), //
		F22(Doc.of(Level.FAULT).text("Tz_EmergSStop_Fault. Emergency stop fault")), //
		F23(Doc.of(Level.FAULT).text("Instantaneous leakage current fault")), //
		F24(Doc.of(Level.FAULT).text("Phalanx insulation resistance fault")), //
		F26(Doc.of(Level.FAULT).text("Parallel CAN-Bus communication failure")), //
		F29(Doc.of(Level.FAULT).text("No AC grid")), //
		F35(Doc.of(Level.FAULT).text("Parallel system shutdown failure")), //
		F41(Doc.of(Level.FAULT).text("Parallel system stop")), //
		F42(Doc.of(Level.FAULT).text("AC line low voltage fault")), //
		F46(Doc.of(Level.FAULT).text("Backup battery failure")), //
		F47(Doc.of(Level.FAULT).text("AC overfrequency")), //
		F48(Doc.of(Level.FAULT).text("AC underfrequency")), //
		F49(Doc.of(Level.FAULT).text("Backup battery failure")), //
		F56(Doc.of(Level.FAULT).text("Bus voltage too low")), //
		F58(Doc.of(Level.FAULT).text("BMS communication failure")), //
		F63(Doc.of(Level.FAULT).text("Arc fault")), //
		F64(Doc.of(Level.FAULT).text("Heat sink temperature too high")), //

		// EnumWriteChannels
		WORK_STATE(Doc.of(WorkState.values()).accessMode(AccessMode.READ_WRITE)),

		// IntegerWriteChannel
		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.WRITE_ONLY)), //
		SET_REACTIVE_POWER(
				Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.WRITE_ONLY)), //

		POWER_TO_GRID_TARGET(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)),

		SET_GEN_PEAK_SHAVING_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.WRITE_ONLY)), //
		SET_GRID_PEAK_SHAVING_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.WRITE_ONLY)), //
		CT_RATIO(Doc.of(OpenemsType.INTEGER)), //
		INVERTER_RUN_STATE(Doc.of(InverterRunState.values()).accessMode(AccessMode.READ_ONLY)), //

		// LongReadChannel
		ORIGINAL_ACTIVE_CHARGE_ENERGY(Doc.of(OpenemsType.LONG).unit(Unit.WATT_HOURS)),
		ORIGINAL_ACTIVE_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG).unit(Unit.WATT_HOURS)),

		// Inverter Output includes external generator?
		GRID_OUTPUT_ACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		GRID_OUTPUT_ACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		GRID_OUTPUT_ACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		GRID_OUTPUT_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		GRID_OUTPUT_VOLTAGE_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT).accessMode(AccessMode.READ_ONLY)), //
		GRID_OUTPUT_VOLTAGE_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT).accessMode(AccessMode.READ_ONLY)), //
		GRID_OUTPUT_VOLTAGE_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT).accessMode(AccessMode.READ_ONLY)), //

		GRID_OUTPUT_CURRENT_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).accessMode(AccessMode.READ_ONLY)), //
		GRID_OUTPUT_CURRENT_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).accessMode(AccessMode.READ_ONLY)), //
		GRID_OUTPUT_CURRENT_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).accessMode(AccessMode.READ_ONLY)), //

		// Inverter Output without external generator??
		POWER_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		POWER_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		POWER_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //

		SET_REMOTE_MODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)), //
		SET_CONTROL_MODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)), //
		SET_BATTERY_CONTROL_MODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)), //
		SET_3P_CONTROL_MODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)), //

		SET_BATTERY_POWER_DECI_PERCENT(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)), //
		SET_BATTERY_POWER_SOC(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)), //
		SET_AC_SETPOINT_3P_PERCENT(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)), //
		SET_REMOTE_WATCHDOG_TIME(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)), //

		// ToDo: Set right units and scaling
		SET_BATTERY_CONSTANT_VOLTAGE(
				Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT).accessMode(AccessMode.READ_WRITE)),
		SET_BATTERY_CONSTANT_CURRENT(
				Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).accessMode(AccessMode.READ_WRITE)),

		APPARENT_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE)), //

		PLACEHOLDER_1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //

		PLACEHOLDER_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //

		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

	}

	// -----------------------------------------------------------------------------
	// Helpers: WorkState (internal state)
	// -----------------------------------------------------------------------------

	public default Channel<WorkState> getWorkStateChannel() {
		return this.channel(ChannelId.WORK_STATE);
	}

	public default WorkState getWorkState() {
		return this.getWorkStateChannel().value().asEnum();
	}

	/**
	 * Internal method to set {@link WorkState} of this component.
	 *
	 * <p>
	 * The value is written to the corresponding Channel using
	 * {@link io.openems.edge.common.channel.Channel#setNextValue(Object)} and will
	 * be applied in the next processing cycle.
	 * </p>
	 *
	 * @param value the new {@link WorkState} to set
	 */
	public default void _setWorkState(WorkState value) {
		this.getWorkStateChannel().setNextValue(value);
	}

	// --- Sell mode time points (used in DeyeSunHybridImpl) ---

	public default IntegerReadChannel getSellModeTimePoint1Channel() {
		return this.channel(ChannelId.SELL_MODE_TIME_POINT_1);
	}

	public default Value<Integer> getSellModeTimePoint1() {
		return this.getSellModeTimePoint1Channel().value();
	}

	public default IntegerWriteChannel getSetSellModeTimePoint1Channel() {
		return this.channel(ChannelId.SELL_MODE_TIME_POINT_1);
	}

	public default void setSellModeTimePoint1(int value) throws OpenemsNamedException {
		this.getSetSellModeTimePoint1Channel().setNextWriteValue(value);
	}

	public default IntegerReadChannel getSellModeTimePoint2Channel() {
		return this.channel(ChannelId.SELL_MODE_TIME_POINT_2);
	}

	public default Value<Integer> getSellModeTimePoint2() {
		return this.getSellModeTimePoint2Channel().value();
	}

	public default IntegerWriteChannel getSetSellModeTimePoint2Channel() {
		return this.channel(ChannelId.SELL_MODE_TIME_POINT_2);
	}

	public default void setSellModeTimePoint2(int value) throws OpenemsNamedException {
		this.getSetSellModeTimePoint2Channel().setNextWriteValue(value);
	}

	public default IntegerReadChannel getSellModeTimePoint1CapacityChannel() {
		return this.channel(ChannelId.SELL_MODE_TIME_POINT_1_CAPACITY);
	}

	public default Value<Integer> getSellModeTimePoint1Capacity() {
		return this.getSellModeTimePoint1CapacityChannel().value();
	}

	public default IntegerWriteChannel getSetSellModeTimePoint1CapacityChannel() {
		return this.channel(ChannelId.SELL_MODE_TIME_POINT_1_CAPACITY);
	}

	public default void setSellModeTimePoint1Capacity(int value) throws OpenemsNamedException {
		this.getSetSellModeTimePoint1CapacityChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetSellModeTimePoint1PowerChannel() {
		return this.channel(ChannelId.SELL_MODE_TIME_POINT_1_POWER);
	}

	public default void setSellModeTimePoint1Power(int value) throws OpenemsNamedException {
		this.getSetSellModeTimePoint1PowerChannel().setNextWriteValue(value);
	}

	// --- Charge mode time points (used in DeyeSunHybridImpl) ---

	public default IntegerReadChannel getChargeModeTimePoint1Channel() {
		return this.channel(ChannelId.CHARGE_MODE_TIME_POINT_1);
	}

	public default Value<Integer> getChargeModeTimePoint1() {
		return this.getChargeModeTimePoint1Channel().value();
	}

	public default IntegerWriteChannel getSetChargeModeTimePoint1Channel() {
		return this.channel(ChannelId.CHARGE_MODE_TIME_POINT_1);
	}

	public default void setChargeModeTimePoint1(int value) throws OpenemsNamedException {
		this.getSetChargeModeTimePoint1Channel().setNextWriteValue(value);
	}

	public default IntegerReadChannel getChargeModeTimePoint2Channel() {
		return this.channel(ChannelId.CHARGE_MODE_TIME_POINT_2);
	}

	public default Value<Integer> getChargeModeTimePoint2() {
		return this.getChargeModeTimePoint2Channel().value();
	}

	public default IntegerWriteChannel getSetChargeModeTimePoint2Channel() {
		return this.channel(ChannelId.CHARGE_MODE_TIME_POINT_2);
	}

	public default void setChargeModeTimePoint2(int value) throws OpenemsNamedException {
		this.getSetChargeModeTimePoint2Channel().setNextWriteValue(value);
	}

	// --- Remote mode + control registers (used in ApplyPowerHandler) ---

	public default IntegerWriteChannel getSetRemoteModeChannel() {
		return this.channel(ChannelId.SET_REMOTE_MODE);
	}

	public default void setSetRemoteMode(int value) throws OpenemsNamedException {
		this.getSetRemoteModeChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetRemoteWatchdogTimeChannel() {
		return this.channel(ChannelId.SET_REMOTE_WATCHDOG_TIME);
	}

	public default void setSetRemoteWatchdogTime(int value) throws OpenemsNamedException {
		this.getSetRemoteWatchdogTimeChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetControlModeChannel() {
		return this.channel(ChannelId.SET_CONTROL_MODE);
	}

	public default void setSetControlMode(int value) throws OpenemsNamedException {
		this.getSetControlModeChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetBatteryControlModeChannel() {
		return this.channel(ChannelId.SET_BATTERY_CONTROL_MODE);
	}

	public default void setSetBatteryControlMode(int value) throws OpenemsNamedException {
		this.getSetBatteryControlModeChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSet3PControlModeChannel() {
		return this.channel(ChannelId.SET_3P_CONTROL_MODE);
	}

	public default void setSet3PControlMode(int value) throws OpenemsNamedException {
		this.getSet3PControlModeChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetBatteryConstantVoltageChannel() {
		return this.channel(ChannelId.SET_BATTERY_CONSTANT_VOLTAGE);
	}

	public default void setBatteryConstantVoltage(int value) throws OpenemsNamedException {
		this.getSetBatteryConstantVoltageChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetBatteryConstantCurrentChannel() {
		return this.channel(ChannelId.SET_BATTERY_CONSTANT_CURRENT);
	}

	public default void setBatteryConstantCurrent(int value) throws OpenemsNamedException {
		this.getSetBatteryConstantCurrentChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetBatteryPowerDeciPercentChannel() {
		return this.channel(ChannelId.SET_BATTERY_POWER_DECI_PERCENT);
	}

	public default void setSetBatteryPowerDeciPercent(int value) throws OpenemsNamedException {
		this.getSetBatteryPowerDeciPercentChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetBatteryPowerSocChannel() {
		return this.channel(ChannelId.SET_BATTERY_POWER_SOC);
	}

	public default void setSetBatteryPowerSoc(int value) throws OpenemsNamedException {
		this.getSetBatteryPowerSocChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetAcSetpoint3pPercentChannel() {
		return this.channel(ChannelId.SET_AC_SETPOINT_3P_PERCENT);
	}

	public default void setSetAcSetpoint3pPercent(int value) throws OpenemsNamedException {
		this.getSetAcSetpoint3pPercentChannel().setNextWriteValue(value);
	}

	// -----------------------------------------------------------------------------
	// Existing helpers (as you had them)
	// -----------------------------------------------------------------------------

	public default IntegerReadChannel getLimitMaxPowerOutputChannel() {
		return this.channel(ChannelId.LIMIT_MAX_GRID_OUTPUT_POWER);
	}

	public default Value<Integer> getLimitMaxPowerOutput() {
		return this.getLimitMaxPowerOutputChannel().value();
	}

	public default Channel<LimitControlFunction> getLimitControlFunctionChannel() {
		return this.channel(ChannelId.LIMIT_CONTROL_FUNCTION);
	}

	public default WriteChannel<LimitControlFunction> getSetLimitControlFunctionChannel() {
		return this.channel(ChannelId.LIMIT_CONTROL_FUNCTION);
	}

	public default LimitControlFunction getLimitControlFunction() {
		return this.getLimitControlFunctionChannel().value().asEnum();
	}

	public default void setLimitControlFunction(LimitControlFunction value) throws OpenemsNamedException {
		this.getSetLimitControlFunctionChannel().setNextWriteValue(value);
	}

	public default Channel<EnableDisable> getSolarSellModeChannel() {
		return this.channel(ChannelId.SOLAR_SELL_MODE);
	}

	public default WriteChannel<EnableDisable> setSolarSellModeChannel() {
		return this.channel(ChannelId.SOLAR_SELL_MODE);
	}

	public default EnableDisable getSolarSellMode() {
		return this.getSolarSellModeChannel().value().asEnum();
	}

	public default void setSolarSellMode(EnableDisable value) throws OpenemsNamedException {
		this.setSolarSellModeChannel().setNextWriteValue(value);
	}

	public default Channel<EnergyManagementModel> getEnergyManagementModelChannel() {
		return this.channel(ChannelId.ENERGY_MANAGEMENT_MODEL);
	}

	public default WriteChannel<EnergyManagementModel> setEnergyManagementModelChannel() {
		return this.channel(ChannelId.ENERGY_MANAGEMENT_MODEL);
	}

	public default EnergyManagementModel getEnergyManagementModel() {
		return this.getEnergyManagementModelChannel().value().asEnum();
	}

	public default void setEnergyManagementModel(EnergyManagementModel value) throws OpenemsNamedException {
		this.setEnergyManagementModelChannel().setNextWriteValue(value);
	}

	public default Channel<EnableDisable> getMpptStatusChannel() {
		return this.channel(ChannelId.MPPT_NUMBER);
	}

	public default EnumWriteChannel getSetMpptChannel() {
		return this.channel(ChannelId.SET_MPPT_NUMBER);
	}

	public default Value<Integer> getMpptStatus() {
		return this.getMpptStatusChannel().value().asEnum();
	}

	public default void setMpptMode(EnableDisable value) throws OpenemsNamedException {
		this.getSetMpptChannel().setNextWriteValue(value);
	}

	public default Channel<RemoteLockState> getRemoteLockChannel() {
		return this.channel(ChannelId.REMOTE_LOCK_STATE);
	}

	public default WriteChannel<RemoteLockState> getSetRemoteLockChannel() {
		return this.channel(ChannelId.REMOTE_LOCK_STATE);
	}

	public default RemoteLockState getRemoteLockState() {
		return this.getRemoteLockChannel().value().asEnum();
	}

	public default void setRemoteLock(RemoteLockState value) throws OpenemsNamedException {
		this.getSetRemoteLockChannel().setNextWriteValue(value);
	}

	public default Channel<EnableDisable> getEnableSwitchChannel() {
		return this.channel(ChannelId.ENABLE_SWITCH_STATE);
	}

	public default WriteChannel<EnableDisable> getSetEnableSwitchChannel() {
		return this.channel(ChannelId.ENABLE_SWITCH_STATE);
	}

	public default EnableDisable getEnableSwitchState() {
		return this.getEnableSwitchChannel().value().asEnum();
	}

	public default void setEnableSwitch(EnableDisable value) throws OpenemsNamedException {
		this.getSetEnableSwitchChannel().setNextWriteValue(value);
	}

	public default Channel<EnableDisable> getEnableGridChargeChannel() {
		return this.channel(ChannelId.GRID_CHARGING_ENABLE);
	}

	public default WriteChannel<EnableDisable> getSetEnableGridChargeChannel() {
		return this.channel(ChannelId.SET_GRID_CHARGING_ENABLE);
	}

	public default EnableDisable getEnableGridChargeState() {
		return this.getEnableGridChargeChannel().value().asEnum();
	}

	public default void setEnableGridCharge(EnableDisable value) throws OpenemsNamedException {
		this.getSetEnableGridChargeChannel().setNextWriteValue(value);
	}

	public default IntegerReadChannel getGridChargeCurrentChannel() {
		return this.channel(ChannelId.GRID_CHARGE_CURRENT);
	}

	public default IntegerWriteChannel getSetGridChargeCurrentChannel() {
		return this.channel(ChannelId.SET_GRID_CHARGE_CURRENT);
	}

	public default Value<Integer> getGridChargeCurrent() {
		return this.getGridChargeCurrentChannel().value();
	}

	public default void setGridChargeCurrent(int value) throws OpenemsNamedException {
		this.getSetGridChargeCurrentChannel().setNextWriteValue(value);
	}

	public default BooleanWriteChannel getSetGeneratorCharingEnabledChannel() {
		return this.channel(ChannelId.SET_GENERATOR_CHARGING_ENABLE);
	}

	public default BooleanReadChannel getGeneratorCharingEnabledChannel() {
		return this.channel(ChannelId.GENERATOR_CHARGING_ENABLE);
	}

	public default Boolean getGeneratorCharingEnabled() {
		return this.getGeneratorCharingEnabledChannel().value().get();
	}

	public default void setGeneratorCharingEnabled(Boolean value) throws OpenemsNamedException {
		this.getSetGeneratorCharingEnabledChannel().setNextWriteValue(value);
	}

	public default BooleanWriteChannel getSetGridCharingEnabledChannel() {
		return this.channel(ChannelId.SET_GRID_CHARGING_ENABLE);
	}

	public default BooleanReadChannel getGridCharingEnabledChannel() {
		return this.channel(ChannelId.GRID_CHARGING_ENABLE);
	}

	public default Boolean getGridCharingEnabled() {
		return this.getGridCharingEnabledChannel().value().get();
	}

	public default void setGridCharingEnabled(Boolean value) throws OpenemsNamedException {
		this.getSetGridCharingEnabledChannel().setNextWriteValue(value);
	}

	public default BooleanWriteChannel getSetTimeOfUseSellingEnabledChannel() {
		return this.channel(ChannelId.SET_TIME_OF_USE_SELLING_ENABLED);
	}

	public default BooleanWriteChannel getSetTimeOfUseMondayChannel() {
		return this.channel(ChannelId.SET_TIME_OF_USE_MONDAY);
	}

	public default BooleanWriteChannel getSetTimeOfUseTuesdayChannel() {
		return this.channel(ChannelId.SET_TIME_OF_USE_TUESDAY);
	}

	public default BooleanWriteChannel getSetTimeOfUseWednesdayChannel() {
		return this.channel(ChannelId.SET_TIME_OF_USE_WEDNESDAY);
	}

	public default BooleanWriteChannel getSetTimeOfUseThursdayChannel() {
		return this.channel(ChannelId.SET_TIME_OF_USE_THURSDAY);
	}

	public default BooleanWriteChannel getSetTimeOfUseFridayChannel() {
		return this.channel(ChannelId.SET_TIME_OF_USE_FRIDAY);
	}

	public default BooleanWriteChannel getSetTimeOfUseSaturdayChannel() {
		return this.channel(ChannelId.SET_TIME_OF_USE_SATURDAY);
	}

	public default BooleanWriteChannel getSetTimeOfUseSundayChannel() {
		return this.channel(ChannelId.SET_TIME_OF_USE_SUNDAY);
	}

	public default BooleanReadChannel getTimeOfUseSellingEnabledChannel() {
		return this.channel(ChannelId.TIME_OF_USE_SELLING_ENABLED);
	}

	public default BooleanReadChannel getTimeOfUseMondayChannel() {
		return this.channel(ChannelId.TIME_OF_USE_MONDAY);
	}

	public default BooleanReadChannel getTimeOfUseTuesdayChannel() {
		return this.channel(ChannelId.TIME_OF_USE_TUESDAY);
	}

	public default BooleanReadChannel getTimeOfUseWednesdayChannel() {
		return this.channel(ChannelId.TIME_OF_USE_WEDNESDAY);
	}

	public default BooleanReadChannel getTimeOfUseThursdayChannel() {
		return this.channel(ChannelId.TIME_OF_USE_THURSDAY);
	}

	public default BooleanReadChannel getTimeOfUseFridayChannel() {
		return this.channel(ChannelId.TIME_OF_USE_FRIDAY);
	}

	public default BooleanReadChannel getTimeOfUseSaturdayChannel() {
		return this.channel(ChannelId.TIME_OF_USE_SATURDAY);
	}

	public default BooleanReadChannel getTimeOfUseSundayChannel() {
		return this.channel(ChannelId.TIME_OF_USE_SUNDAY);
	}

	public default Boolean getTimeOfUseSellingEnabled() {
		return this.getTimeOfUseSellingEnabledChannel().value().get();
	}

	public default Boolean getTimeOfUseMonday() {
		return this.getTimeOfUseMondayChannel().value().get();
	}

	public default Boolean getTimeOfUseTuesday() {
		return this.getTimeOfUseTuesdayChannel().value().get();
	}

	public default Boolean getTimeOfUseWednesday() {
		return this.getTimeOfUseWednesdayChannel().value().get();
	}

	public default Boolean getTimeOfUseThursday() {
		return this.getTimeOfUseThursdayChannel().value().get();
	}

	public default Boolean getTimeOfUseFriday() {
		return this.getTimeOfUseFridayChannel().value().get();
	}

	public default Boolean getTimeOfUseSaturday() {
		return this.getTimeOfUseSaturdayChannel().value().get();
	}

	public default Boolean getTimeOfUseSunday() {
		return this.getTimeOfUseSundayChannel().value().get();
	}

	public default void setTimeOfUseSellingEnabled(Boolean value) throws OpenemsNamedException {
		this.getSetTimeOfUseSellingEnabledChannel().setNextWriteValue(value);
	}

	public default void setTimeOfUseMonday(Boolean value) throws OpenemsNamedException {
		this.getSetTimeOfUseMondayChannel().setNextWriteValue(value);
	}

	public default void setTimeOfUseTuesday(Boolean value) throws OpenemsNamedException {
		this.getSetTimeOfUseTuesdayChannel().setNextWriteValue(value);
	}

	public default void setTimeOfUseWednesday(Boolean value) throws OpenemsNamedException {
		this.getSetTimeOfUseWednesdayChannel().setNextWriteValue(value);
	}

	public default void setTimeOfUseThursday(Boolean value) throws OpenemsNamedException {
		this.getSetTimeOfUseThursdayChannel().setNextWriteValue(value);
	}

	public default void setTimeOfUseFriday(Boolean value) throws OpenemsNamedException {
		this.getSetTimeOfUseFridayChannel().setNextWriteValue(value);
	}

	public default void setTimeOfUseSaturday(Boolean value) throws OpenemsNamedException {
		this.getSetTimeOfUseSaturdayChannel().setNextWriteValue(value);
	}

	public default void setTimeOfUseSunday(Boolean value) throws OpenemsNamedException {
		this.getSetTimeOfUseSundayChannel().setNextWriteValue(value);
	}

	public default IntegerReadChannel getMaxAcExportChannel() {
		return this.channel(ChannelId.MAX_AC_EXPORT);
	}

	public default Value<Integer> getMaxAcExport() {
		return this.getMaxAcExportChannel().value();
	}

	/**
	 * Internal method to set {@link MAX_AC_EXPORT} of this component.
	 *
	 * @param value the new {@link MAX_AC_EXPORT} to set
	 */
	public default void _setMaxAcExport(Integer value) {
		this.getMaxAcExportChannel().setNextValue(value);
	}

	public default IntegerReadChannel getMaxAcImportChannel() {
		return this.channel(ChannelId.MAX_AC_IMPORT);
	}

	public default Value<Integer> getMaxAcImport() {
		return this.getMaxAcImportChannel().value();
	}

	/**
	 * Internal method to set {@link MAX_AC_IMPORT} of this component.
	 *
	 * @param value the new {@link MAX_AC_IMPORT} to set
	 */
	public default void _setMaxAcImport(Integer value) {
		this.getMaxAcImportChannel().setNextValue(value);
	}

	public default IntegerWriteChannel getSetPowerToGridTargetChannel() {
		return this.channel(ChannelId.POWER_TO_GRID_TARGET);
	}

	public default IntegerReadChannel getPowerToGridTargetChannel() {
		return this.channel(ChannelId.POWER_TO_GRID_TARGET);
	}

	/**
	 * Internal method to set {@link POWER_TO_GRID_TARGET} of this component.
	 * Register 143.
	 *
	 * @param value the new {@link POWER_TO_GRID_TARGET} to set
	 */
	public default void setPowerToGridTarget(int value) throws OpenemsNamedException {
		this.getSetPowerToGridTargetChannel().setNextWriteValue(value);
	}

	public default Value<Integer> getPowerToGridTarget() {
		return this.getPowerToGridTargetChannel().value();
	}

	// Placeholder
	public default IntegerWriteChannel getPlaceholder1Channel() {
		return this.channel(ChannelId.PLACEHOLDER_1);
	}

	public default void setPlaceholder1(int value) throws OpenemsNamedException {
		this.getPlaceholder1Channel().setNextWriteValue(value);
	}
	
	// Placeholder
	public default IntegerWriteChannel getPlaceholder2Channel() {
		return this.channel(ChannelId.PLACEHOLDER_2);
	}

	public default void setPlaceholder2(int value) throws OpenemsNamedException {
		this.getPlaceholder2Channel().setNextWriteValue(value);
	}	



	// -----------------------------------------------------------------------------
	// Error handling helpers (unchanged)
	// -----------------------------------------------------------------------------

	private static Set<Integer> readErrorCodes(OpenemsComponent self) {
		Set<Integer> result = new HashSet<>();
		addErrorBits(result, self.channel(ChannelId.ERROR_MESSAGE_1).value().asOptional(), 1);
		addErrorBits(result, self.channel(ChannelId.ERROR_MESSAGE_2).value().asOptional(), 17);
		addErrorBits(result, self.channel(ChannelId.ERROR_MESSAGE_3).value().asOptional(), 33);
		addErrorBits(result, self.channel(ChannelId.ERROR_MESSAGE_4).value().asOptional(), 49);
		return result;
	}

	private static void addErrorBits(Set<Integer> errors, Optional<?> opt, int bitOffset) {
		if (opt.isPresent() && opt.get() instanceof Integer) {
			int value = (Integer) opt.get();
			for (int i = 0; i < 16; i++) {
				if ((value & (1 << i)) != 0) {
					errors.add(bitOffset + i);
				}
			}
		}
	}

	/**
	 * Updates the error-state Channels (Fxx) of this component based on the currently active error codes.
	 *
	 * <p>The active error codes are obtained via {@code readErrorCodes(self)} (e.g. derived from Modbus register 143).</p>
	 *
	 * @param self the {@link OpenemsComponent} to update
	 */
	public static void updateErrorState(OpenemsComponent self) {
	    Set<Integer> activeErrors = readErrorCodes(self);

	    for (ChannelId channelId : ChannelId.values()) {
	        if (!channelId.name().matches("F\\d+")) {
	            continue; // Only Error Channels
	        }
	        int code = Integer.parseInt(channelId.name().substring(1)); // "F22" → 22
	        boolean isActive = activeErrors.contains(code);
	        self.channel(channelId).setNextValue(isActive); // true = error active
	    }
	}

	/**
	 * Checks whether the BMS communication error is currently active.
	 *
	 * <p>The active error codes are obtained via {@code readErrorCodes(self)} (e.g. derived from Modbus register 143).
	 * This method returns {@code true} if error code {@code 58} is present.</p>
	 *
	 * @param self the {@link OpenemsComponent} to query
	 * @return {@code true} if the BMS communication error is active; otherwise {@code false}
	 */
	public static boolean isBmsCommError(OpenemsComponent self) {
	    Set<Integer> activeErrors = readErrorCodes(self);
	    return activeErrors.contains(58);
	}

}
