package io.openems.edge.ruhfass.battery.batcon;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.ruhfass.battery.batcon.enums.Balancing;
import io.openems.edge.ruhfass.battery.batcon.enums.BalancingMessage;
import io.openems.edge.ruhfass.battery.batcon.enums.BalancingStatus;
import io.openems.edge.ruhfass.battery.batcon.enums.BatconReset;
import io.openems.edge.ruhfass.battery.batcon.enums.BatteryReset;
import io.openems.edge.ruhfass.battery.batcon.enums.BatteryType;
import io.openems.edge.ruhfass.battery.batcon.enums.BusError;
import io.openems.edge.ruhfass.battery.batcon.enums.CanState;
import io.openems.edge.ruhfass.battery.batcon.enums.CellDifferenceStatus;
import io.openems.edge.ruhfass.battery.batcon.enums.ContactorCommand;
import io.openems.edge.ruhfass.battery.batcon.enums.DcCharge;
import io.openems.edge.ruhfass.battery.batcon.enums.FailureMemoryDelete;
import io.openems.edge.ruhfass.battery.batcon.enums.FailureMemoryDeleteStatus;
import io.openems.edge.ruhfass.battery.batcon.enums.FailureMemoryRead;
import io.openems.edge.ruhfass.battery.batcon.enums.FailureMemoryReadStatus;
import io.openems.edge.ruhfass.battery.batcon.enums.IsolationMearsurementDeactivationModeRead;
import io.openems.edge.ruhfass.battery.batcon.enums.IsolationMearsurementDeactivationModeWrite;
import io.openems.edge.ruhfass.battery.batcon.enums.IsolationResistanceStatus;
import io.openems.edge.ruhfass.battery.batcon.enums.OperationModeBattery;
import io.openems.edge.ruhfass.battery.batcon.enums.RemainingBusSimulationCommand;
import io.openems.edge.ruhfass.battery.batcon.enums.RemainingBusSimulationStatus;
import io.openems.edge.ruhfass.battery.batcon.enums.ResetUserNetworkAddress;
import io.openems.edge.ruhfass.battery.batcon.enums.StatusKL15Can;
import io.openems.edge.ruhfass.battery.batcon.statemachine.StateMachine.State;

public interface Batcon extends Battery, OpenemsComponent, StartStoppable {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		STATE_MACHINE(Doc.of(State.values())//
				.text("Current State of State-Machine").persistencePriority(PersistencePriority.HIGH)), //
		RUN_FAILED(Doc.of(Level.FAULT)//
				.text("Running the Logic failed").persistencePriority(PersistencePriority.HIGH)), //
		ERROR(Doc.of(Level.FAULT)//
				.text("State-Machine in Error-State!").persistencePriority(PersistencePriority.HIGH)), //
		ERROR_BATTERY_TYPE(Doc.of(Level.FAULT)//
				.text("Configuring the Battery Type not successful!").persistencePriority(PersistencePriority.HIGH)), //
		MAX_ALLOWED_START_TIME_FAULT(Doc.of(Level.FAULT)//
				.text("The maximum start time is passed!").persistencePriority(PersistencePriority.HIGH)), //
		MAX_ALLOWED_STOP_TIME_FAULT(Doc.of(Level.FAULT)//
				.text("The maximum stop time is passed!").persistencePriority(PersistencePriority.HIGH)), //

		/*
		 * BATCON-Commands
		 */
		BATCON_RESET(Doc.of(BatconReset.values()).accessMode(AccessMode.WRITE_ONLY)),
		SET_BATTERY_TYPE(Doc.of(BatteryType.values()).accessMode(AccessMode.WRITE_ONLY)),
		WRITE_IP_ADDRESS(Doc.of(OpenemsType.LONG).unit(Unit.NONE).accessMode(AccessMode.WRITE_ONLY)),
		WRITE_SUBNET_MASK(Doc.of(OpenemsType.LONG).unit(Unit.NONE).accessMode(AccessMode.WRITE_ONLY)),
		RESET_USER_NETWORK_ADDRESS(Doc.of(ResetUserNetworkAddress.values()).accessMode(AccessMode.WRITE_ONLY)),

		/*
		 * BATCON-Data
		 */
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		HARDWARE_VERSION(Doc.of(OpenemsType.STRING).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		SOFTWARE_VERSION(Doc.of(OpenemsType.STRING).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		BOOTLOADER_SOFTWARE_VERSION(Doc.of(OpenemsType.STRING).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),

		/*
		 * Battery 1 Commands
		 */
		REMAINING_BUS_SIMULATION_COMMAND(
				Doc.of(RemainingBusSimulationCommand.values()).accessMode(AccessMode.WRITE_ONLY)),
		CONTACTOR_COMMAND(Doc.of(ContactorCommand.values()).accessMode(AccessMode.WRITE_ONLY)),
		BATTERY_RESET(Doc.of(BatteryReset.values()).accessMode(AccessMode.WRITE_ONLY)),
		STATUS_KL15_CAN(Doc.of(StatusKL15Can.values()).accessMode(AccessMode.WRITE_ONLY)),
		FAILURE_MEMORY_DELETE(Doc.of(FailureMemoryDelete.values()).accessMode(AccessMode.WRITE_ONLY)),
		FAILURE_MEMORY_READ(Doc.of(FailureMemoryRead.values()).accessMode(AccessMode.WRITE_ONLY)),
		BALANCING(Doc.of(Balancing.values()).accessMode(AccessMode.WRITE_ONLY)),
		ISOLATION_MEASUREMENT_DEACTIVATION_MODE_WRITE(
				Doc.of(IsolationMearsurementDeactivationModeWrite.values()).accessMode(AccessMode.WRITE_ONLY)),
		SET_NEW_BATTERY_CAPACITY(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.WRITE_ONLY)),
		DC_CHARGE(Doc.of(DcCharge.values()).accessMode(AccessMode.WRITE_ONLY)),

		/*
		 * METADATA 1
		 */
		BATTERYMANAGER_SOFTWAREVERSION(Doc.of(OpenemsType.STRING).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		VW_ECU_HARDWARE_NUMBER(Doc.of(OpenemsType.STRING).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		ASAM_ODX_FILE_VERSION(Doc.of(OpenemsType.STRING).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		VW_ECU_HARDWARE_VERSION_NUMBER(Doc.of(OpenemsType.STRING).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),

		/*
		 * Status Info 1
		 */
		BATTERY_TYPE(Doc.of(BatteryType.values())),
		ISOLATION_MEASUREMENT_DEACTIVATION_MODE_READ(Doc.of(IsolationMearsurementDeactivationModeRead.values())),
		BUS_ERROR(Doc.of(BusError.values())),
		REMAINING_BUS_SIMULATION_STATUS(Doc.of(RemainingBusSimulationStatus.values())),
		CAN_STATE(Doc.of(CanState.values())),
		DIAGNOSTIC_STATE(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		KL15_STATE(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)),

		/*
		 * CAN Values 1
		 */
		ACTUAL_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		HV_TIMEOUT(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)),
		ACTUAL_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		ACTUAL_CURRENT_WITHOUT_OFFSET(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		ACTUAL_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		ACTUAL_MAX_TEMPERATURE_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		ACTUAL_MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		ACTUAL_MIN_TEMPERATURE_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		ACTUAL_MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT).accessMode(AccessMode.READ_ONLY)),
		ACTUAL_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT).accessMode(AccessMode.READ_ONLY)),
		BAT_CAPACITY(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE_HOURS).accessMode(AccessMode.READ_ONLY)),
		OPEN_CIRCUIT_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		MAX_LIMIT_SOC(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),
		MAX_CHARGE_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		MIN_CHARGE_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		SOC_HI_RES(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),
		TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		TEMPERATURE_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		OPERATION_MODE_BATTERY(Doc.of(OperationModeBattery.values())),
		ENERGY_CONTENT(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
		MIN_LIMIT_SOC(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),
		USEABLE_SOC(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),

		/*
		 * Failure Memory 1
		 */
		FAILURE_MEMORY_DTC(Doc.of(OpenemsType.STRING).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		FAILURE_MEMORY_READ_STATUS(Doc.of(FailureMemoryReadStatus.values())),
		FAILURE_MEMORY_DELETE_STATUS(Doc.of(FailureMemoryDeleteStatus.values())),

		/*
		 * Operation Modes 2ndL Betriebsbereiche 2ndL 1
		 */
		MAX_PERMANENT_DISCHARGE_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		MAX_SHORT_TERM_DISCHARGE_CURRENT(
				Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		MAX_PERMANENT_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		MAX_SHORT_TERM_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		USEABLE_CAPCITY_SOH(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),
		MAX_ALLOWED_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT).accessMode(AccessMode.READ_ONLY)),
		MIN_ALLOWED_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT).accessMode(AccessMode.READ_ONLY)),
		MAX_ALLOWED_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		MIN_ALLOWED_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		MAX_ALLOWED_BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		MIN_ALLOWED_BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		NEW_BATTERY_CAPACITY(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE_HOURS).accessMode(AccessMode.READ_ONLY)),

		/*
		 * Failure Signals 1
		 */

		// Critical Failure 1
		CELL_OVER_CHARGED(Doc.of(Level.FAULT)//
				.text("Cell over charged! (Zell�berladung!)").persistencePriority(PersistencePriority.HIGH)), // ), //
		CELL_DEEP_DISCHARGED(Doc.of(Level.FAULT)//
				.text("Cell deep discharged! (Zelltiefenentladung!)")), //
		MAX_CELL_TEMPERATURE_HIGH_ALARM(Doc.of(Level.FAULT)//
				.text("Maximum Cell Temp too high! (�berschreitung maximale Zelltemperatur!)")), //

		// Failure 2ndL immediatelly reaction is neccessary! 1
		MAX_CELL_VOLTAGE_HIGH_ALARM(Doc.of(Level.FAULT)//
				.text("Maximum Cell Voltage too high! (�berschreitung maximale Zellspannung!)")), //
		MIN_CELL_VOLTAGE_LOW_ALARM(Doc.of(Level.FAULT)//
				.text("Minimum Cell Voltage too low! (Unterschreitung minimale Zellspannung!)")), //
		MAX_SHORT_TERM_CHARGE_CURRENT_HIGH_ALARM(Doc.of(Level.FAULT)//
				.text("Maximum short term charge current too high! (�berschreitung max kurzfristig Ladestrom!)")), //
		MAX_SHORT_TERM_DISCHARGE_CURRENT_HIGH_ALARM(Doc.of(Level.FAULT)//
				.text("Maximum short term discharge current too high! (�berschreitung max kurzfristig Entladestrom!)")), //
		MAX_PERMANENT_CHARGE_CURRENT_HIGH_ALARM(Doc.of(Level.FAULT)//
				.text("Maximum permanent charge current too high! (�berschreitung max dauerhafter Ladestrom!)")), //
		MAX_PERMANENT_DISCHARGE_CURRENT_HIGH_ALARM(Doc.of(Level.FAULT)//
				.text("Maximum permanent discharge current too high! (�berschreitung max dauerhafter Entladestrom!)")), //
		CELL_TEMPERATURE_HIGH_LOW_ALARM_5K(Doc.of(Level.OK)//
				.text("Maximum Cell Temp 5K too high or too low! (�ber-/Unterschreitung Zelltemperatur um 5K!)")), //
		CELL_TEMPERATURE_HIGH_LOW_ALARM(Doc.of(Level.OK)//
				.text("Maximum Cell Temp too high or too low! (�ber-/Unterschreitung Zelltemperatur!)")), //
		BATTERY_OVER_UNDER_VOLTAGE(Doc.of(Level.FAULT)//
				.text("Battery Voltage too high or too low! (Batterie�ber-/unterspannung!)")), //
		DISCHARGE_CURRENT_1NDL_TOO_HIGH(Doc.of(Level.FAULT)//
				.text("Limit for Discharge Current 2ndL too high, BMC Discharge current limit is used! ( Grenzwert f�r Entladestrom aus 2ndL-Tabelle zu hoch, BMC-Entladestromgrenzwert wird benutzt!)")), //
		CHARGE_CURRENT_1NDL_TOO_HIGH(Doc.of(Level.FAULT)//
				.text("Limit for Charge Current 2ndL too high, BMC Charge current limit is used! ( Grenzwert f�r Ladestrom aus 2ndL-Tabelle zu hoch, BMC-Ladestromgrenzwert wird benutzt!)")), //

		// Battery Failures 1
		ISOLATION_FAILURE(Doc.of(Level.OK)//
				.text("Isolation Failure! (Isolationsfehler!)")), //
		FAILURE_PILOT_LINE(Doc.of(Level.FAULT)//
				.text("Failure Pilot Line! (Fehler Pilotlinie (wird f�r CBEV und Q7 nicht gesetzt)!)")), //
		POWER_DERATING(Doc.of(Level.WARNING)//
				.text("Power Derating! (Leistungsreduzierung!")), //
		FAILURE_BATTERY_FUSE(Doc.of(Level.FAULT)//
				.text("Failure Battery Fuse! (Fehler der Batteriesicherung!")), //
		FAILURE_COLD_START_POWER(Doc.of(Level.FAULT)//
				.text("Failure Cold Start Power! (Fehler der Kaltstartleistung!")), //
		CONTACTOR_CLOSING_NOT_POSSIBLE(Doc.of(Level.FAULT)//
				.text("Closing Contactor not possible! (Sch�tz kann nicht mehr geschlossen werden!")), //
		FAILURE_SD(Doc.of(Level.FAULT)//
				.text("Failure SD! (Fehler SD!")), //
		NO_COMPONENT_FUNCTION(Doc.of(Level.WARNING)//
				.text("No Component Function! (keine Komponentenfunktion!")), //
		MAIN_CONTACTOR_WELDED(Doc.of(Level.FAULT)//
				.text("Main Contactor welded! (Hauptsch�tz verschwei�t!")), //
		BATTERY_FAILURE_DIAGNOSTIC_NEEDED(Doc.of(Level.FAULT)//
				.text("Battery Failure! Diagnostic needed! (Batteriefehler_Diagnose notwendig!")), //

		/*
		 * Balancing 1
		 */
		ACTUAL_BALANCING_STATUS(Doc.of(BalancingStatus.values())),
		BALANCING_MONITORING(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		MAX_ALLOWED_CURRENT_BALANCING(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		STATUS_CELL_VOLTAGE_DIFFERENCE(Doc.of(CellDifferenceStatus.values())),
		BALANCING_MESSAGE(Doc.of(BalancingMessage.values())),

		/*
		 * CAN Values High Resolution 1
		 */
		VOLTAGE_HIGH_RES(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		CURRENT_HIGH_RES(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		CURRENT_HIGH_RES_WITHOUT_OFFSET(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		SOC_HI_RES_HIGH_RES(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),
		TEMPERATURE_HIGH_RES(Doc.of(OpenemsType.LONG).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		TEMPERATURE_HIGH_RES_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		CAPACITY_HIGH_RES(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE_HOURS).accessMode(AccessMode.READ_ONLY)),

		/*
		 * Isolation Resistance 1
		 */
		ISOLATION_RESISTANCE_SYSTEM_PLUS(
				Doc.of(OpenemsType.INTEGER).unit(Unit.KILOOHM).accessMode(AccessMode.READ_ONLY)),
		ISOLATION_RESISTANCE_SYSTEM_MINUS(
				Doc.of(OpenemsType.INTEGER).unit(Unit.KILOOHM).accessMode(AccessMode.READ_ONLY)),
		ISOLATION_RESISTANCE_BATTERY_PLUS(
				Doc.of(OpenemsType.INTEGER).unit(Unit.KILOOHM).accessMode(AccessMode.READ_ONLY)),
		ISOLATION_RESISTANCE_BATTERY_MINUS(
				Doc.of(OpenemsType.INTEGER).unit(Unit.KILOOHM).accessMode(AccessMode.READ_ONLY)),

		/*
		 * Temperatues 1
		 */
		BATTERY_TEMPERATURE_SENOSR_1(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_1_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_2(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_2_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_3(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_3_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_4(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_4_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_5(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_5_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_6(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_6_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_7(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_7_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_8(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_8_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_9(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_9_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_10(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_10_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_11(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_11_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_12(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_12_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_13(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_13_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_14(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_14_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_15(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_15_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_16(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_16_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_17(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_17_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_18(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_18_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_19(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_19_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_20(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_20_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_21(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_21_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_22(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_22_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_23(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_23_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_24(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_24_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_25(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_25_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_26(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_26_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_27(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_27_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_28(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_28_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_29(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_29_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_30(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_30_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_31(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_31_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_32(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_32_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_33(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_33_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_34(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_34_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_35(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_35_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_36(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_36_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_37(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_37_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_38(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_38_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_39(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_39_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_40(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_40_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_41(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_41_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_42(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_42_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_43(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_43_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_44(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_44_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_45(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_45_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_46(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_46_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_47(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_47_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_48(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_48_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_49(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_49_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_50(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_50_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_51(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_51_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_52(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_52_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_53(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_53_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_54(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_54_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_55(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_55_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_56(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_56_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_57(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_57_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_58(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_58_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_59(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_59_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_60(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_60_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_61(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_61_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_62(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_62_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_63(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_63_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_64(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_64_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_65(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_65_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_66(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_66_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_67(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_67_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_68(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_68_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_69(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_69_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_70(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_70_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_71(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_71_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE_SENOSR_72(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_SENOSR_72_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		HIGH_VOLTAGE_BATTERY_MAXIMUM_TEMPERATURE(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		HIGH_VOLTAGE_BATTERY_MAXIMUM_TEMPERATURE_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		HIGH_VOLTAGE_BATTERY_MINIMUM_TEMPERATURE(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		HIGH_VOLTAGE_BATTERY_MINIMUM_TEMPERATURE_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		BATTERY_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
		BATTERY_TEMPERATURE_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		/*
		 * State of Charge 1
		 */
		USER_SOC(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),
		MAXIMUM_CELL_STATE_OF_CHARGE(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),
		MINIMUM_CELL_STATE_OF_CHARGE(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),

		/*
		 * Voltages 1
		 */

		VOLTAGE_TERMINAL_30(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		VOLTAGE_TERMINAL_30_WITHOUT_OFFSET(
				Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //

		HIGH_VOLTAGE_SYSTEM_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // High_voltage_system_status
		SUM_OF_CELL_VOLTAGE_INTERN(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Sum_of_Cell_Voltage
		MAXIMUM_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Maximum_Cell_Voltage
		MINIMUM_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Minimum_Cell_Voltage

		/*
		 * Cell Voltages 1
		 */
		VOLTAGE_CELL_1(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_1
		VOLTAGE_CELL_2(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_2
		VOLTAGE_CELL_3(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_3
		VOLTAGE_CELL_4(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_4
		VOLTAGE_CELL_5(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_5
		VOLTAGE_CELL_6(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_6
		VOLTAGE_CELL_7(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_7
		VOLTAGE_CELL_8(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_8
		VOLTAGE_CELL_9(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_9
		VOLTAGE_CELL_10(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_10
		VOLTAGE_CELL_11(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_11
		VOLTAGE_CELL_12(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_12
		VOLTAGE_CELL_13(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_13
		VOLTAGE_CELL_14(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_14
		VOLTAGE_CELL_15(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_15
		VOLTAGE_CELL_16(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_16
		VOLTAGE_CELL_17(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_17
		VOLTAGE_CELL_18(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_18
		VOLTAGE_CELL_19(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_19
		VOLTAGE_CELL_20(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_20
		VOLTAGE_CELL_21(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_21
		VOLTAGE_CELL_22(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_22
		VOLTAGE_CELL_23(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_23
		VOLTAGE_CELL_24(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_24
		VOLTAGE_CELL_25(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_25
		VOLTAGE_CELL_26(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_26
		VOLTAGE_CELL_27(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_27
		VOLTAGE_CELL_28(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_28
		VOLTAGE_CELL_29(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_29
		VOLTAGE_CELL_30(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_30
		VOLTAGE_CELL_31(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_31
		VOLTAGE_CELL_32(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_32
		VOLTAGE_CELL_33(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_33
		VOLTAGE_CELL_34(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_34
		VOLTAGE_CELL_35(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_35
		VOLTAGE_CELL_36(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_36
		VOLTAGE_CELL_37(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_37
		VOLTAGE_CELL_38(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_38
		VOLTAGE_CELL_39(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_39
		VOLTAGE_CELL_40(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_40
		VOLTAGE_CELL_41(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_41
		VOLTAGE_CELL_42(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_42
		VOLTAGE_CELL_43(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_43
		VOLTAGE_CELL_44(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_44
		VOLTAGE_CELL_45(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_45
		VOLTAGE_CELL_46(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_46
		VOLTAGE_CELL_47(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_47
		VOLTAGE_CELL_48(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_48
		VOLTAGE_CELL_49(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_49
		VOLTAGE_CELL_50(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_50
		VOLTAGE_CELL_51(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_51
		VOLTAGE_CELL_52(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_52
		VOLTAGE_CELL_53(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_53
		VOLTAGE_CELL_54(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_54
		VOLTAGE_CELL_55(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_55
		VOLTAGE_CELL_56(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_56
		VOLTAGE_CELL_57(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_57
		VOLTAGE_CELL_58(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_58
		VOLTAGE_CELL_59(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_59
		VOLTAGE_CELL_60(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_60
		VOLTAGE_CELL_61(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_61
		VOLTAGE_CELL_62(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_62
		VOLTAGE_CELL_63(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_63
		VOLTAGE_CELL_64(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_64
		VOLTAGE_CELL_65(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_65
		VOLTAGE_CELL_66(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_66
		VOLTAGE_CELL_67(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_67
		VOLTAGE_CELL_68(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_68
		VOLTAGE_CELL_69(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_69
		VOLTAGE_CELL_70(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_70
		VOLTAGE_CELL_71(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_71
		VOLTAGE_CELL_72(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_72
		VOLTAGE_CELL_73(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_73
		VOLTAGE_CELL_74(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_74
		VOLTAGE_CELL_75(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_75
		VOLTAGE_CELL_76(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_76
		VOLTAGE_CELL_77(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_77
		VOLTAGE_CELL_78(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_78
		VOLTAGE_CELL_79(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_79
		VOLTAGE_CELL_80(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_80
		VOLTAGE_CELL_81(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_81
		VOLTAGE_CELL_82(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_82
		VOLTAGE_CELL_83(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_83
		VOLTAGE_CELL_84(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_84
		VOLTAGE_CELL_85(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_85
		VOLTAGE_CELL_86(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_86
		VOLTAGE_CELL_87(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_87
		VOLTAGE_CELL_88(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_88
		VOLTAGE_CELL_89(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_89
		VOLTAGE_CELL_90(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_90
		VOLTAGE_CELL_91(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_91
		VOLTAGE_CELL_92(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_92
		VOLTAGE_CELL_93(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_93
		VOLTAGE_CELL_94(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_94
		VOLTAGE_CELL_95(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_95
		VOLTAGE_CELL_96(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_96
		VOLTAGE_CELL_97(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_97
		VOLTAGE_CELL_98(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_98
		VOLTAGE_CELL_99(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_99
		VOLTAGE_CELL_100(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_100
		VOLTAGE_CELL_101(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_101
		VOLTAGE_CELL_102(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_102
		VOLTAGE_CELL_103(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_103
		VOLTAGE_CELL_104(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_104
		VOLTAGE_CELL_105(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_105
		VOLTAGE_CELL_106(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_106
		VOLTAGE_CELL_107(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_107
		VOLTAGE_CELL_108(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), // Voltage_Cell_108

		/*
		 * Currents 1
		 */
		PACK_CURRENT_1(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)), // Pack_Current
		PACK_CURRENT_1_WITHOUT_OFFSET(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)), //

		PACK_CURRENT_2(Doc.of(OpenemsType.LONG).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)), // Pack_Current
		PACK_CURRENT_2_WITHOUT_OFFSET(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)), //

		/*
		 * Isolation resistance Status 1
		 */
		INSOLATION_RESISTANCE_SYSTEM_PLUS(Doc.of(IsolationResistanceStatus.values())), // insolation_resistance_system_plus
		INSOLATION_RESISTANCE_SYSTEM_MINUS(Doc.of(IsolationResistanceStatus.values())), // insolation_resistance_system_minus
		INSOLATION_RESISTANCE_BATTERY_PLUS(Doc.of(IsolationResistanceStatus.values())), // insolation_resistance_battery_plus
		INSOLATION_RESISTANCE_BATTERY_MINUS(Doc.of(IsolationResistanceStatus.values())), // insolation_resistance_battery_minus

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

	/**
	 * Gets the Channel for {@link ChannelId#MAX_START_TIME}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getMaxStartTimeChannel() {
		return this.channel(ChannelId.MAX_ALLOWED_START_TIME_FAULT);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_START_TIME}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStartTime() {
		return this.getMaxStartTimeChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_START_TIME}
	 * Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxStartTime(Boolean value) {
		this.getMaxStartTimeChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_STOP_TIME}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getMaxStopTimeChannel() {
		return this.channel(ChannelId.MAX_ALLOWED_STOP_TIME_FAULT);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_STOP_TIME}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStopTime() {
		return this.getMaxStopTimeChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_STOP_TIME}
	 * Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxStopTime(Boolean value) {
		this.getMaxStopTimeChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#KL15_STATE}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default Channel<Integer> getKl15StateChannel() {
		return this.channel(ChannelId.KL15_STATE);
	}

	/**
	 * Gets the {@link ChannelId#KL15_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getKl15State() {
		return this.getKl15StateChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#STATUS_KL15_CAN}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default WriteChannel<Integer> getStatusKl15CanChannel() {
		return this.channel(ChannelId.STATUS_KL15_CAN);
	}

	/**
	 * Gets the {@link ChannelId#STATUS_KL15_CAN}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getStatusKl15Can() {
		return this.getStatusKl15CanChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#STATUS_KL15_CAN}
	 * Channel.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error.
	 */
	public default void setStatusKl15CanChannel(int value) throws OpenemsNamedException {
		this.getStatusKl15CanChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the target Start/Stop mode from config or StartStop-Channel.
	 * 
	 * @return {@link StartStop}
	 */
	public StartStop getStartStopTarget();

	/**
	 * Handle ramaining bus simulation.
	 */
	public void handleRemainingBusSimulation();

	public default EnumReadChannel getRemainingBusSimulationStatusChannel() {
		return this.channel(ChannelId.REMAINING_BUS_SIMULATION_STATUS);
	}

	public default RemainingBusSimulationStatus getRemainingBusSimulationStatus() {
		return this.getRemainingBusSimulationStatusChannel().value().asEnum();
	}

	public default EnumWriteChannel getRemainingBusSimulationCommandChannel() {
		return this.channel(ChannelId.REMAINING_BUS_SIMULATION_COMMAND);
	}

	public default void setRemainingBusSimulationCommand(RemainingBusSimulationCommand remainingBusSimulationCommand) {
		try {
			this.getRemainingBusSimulationCommandChannel().setNextWriteValue(remainingBusSimulationCommand);
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}

	public default void setRemainingBusSimulationOn() {
		this.setRemainingBusSimulationCommand(RemainingBusSimulationCommand.ON);
	}

	public default void setRemainingBusSimulationOff() {
		this.setRemainingBusSimulationCommand(RemainingBusSimulationCommand.OFF);
	}

	public default void setContactorCommand(ContactorCommand contactorCommand) throws OpenemsNamedException {
		EnumWriteChannel setContactorCommandChannel = this.channel(ChannelId.CONTACTOR_COMMAND);
		setContactorCommandChannel.setNextWriteValue(contactorCommand);
	}

	public default EnumReadChannel getOperationModeChannel() {
		return this.channel(ChannelId.OPERATION_MODE_BATTERY);
	}

	public default OperationModeBattery getOperationMode() {
		return this.getOperationModeChannel().value().asEnum();
	}

	public default void setBatteryType(BatteryType batteryType) throws OpenemsNamedException {
		EnumWriteChannel setBatteryTypeChannel = this.channel(ChannelId.SET_BATTERY_TYPE);
		setBatteryTypeChannel.setNextWriteValue(batteryType);
	}

	public default EnumReadChannel getBatteryTypeChannel() {
		return this.channel(ChannelId.BATTERY_TYPE);
	}

	public default BatteryType getBatteryType() {
		return this.getBatteryTypeChannel().value().asEnum();
	}

	public default StateChannel getErrorBatteryTypeChannel() {
		return this.channel(Batcon.ChannelId.ERROR_BATTERY_TYPE);
	}

	public default void setBatconReset() {
		EnumWriteChannel batconResetChannel = this.channel(ChannelId.BATCON_RESET);
		try {
			batconResetChannel.setNextWriteValue(BatconReset.RESET);
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}
}
