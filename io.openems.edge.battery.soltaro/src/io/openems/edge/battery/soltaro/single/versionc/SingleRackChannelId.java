package io.openems.edge.battery.soltaro.single.versionc;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.soltaro.State;
import io.openems.edge.common.channel.Doc;

public enum SingleRackChannelId implements io.openems.edge.common.channel.ChannelId {
	// EnumWriteChannels
	PRE_CHARGE_CONTROL(Doc.of(PreChargeControl.values()) //
			.accessMode(AccessMode.WRITE_ONLY)), //
	EMS_BAUDRATE(Doc.of(EmsBaudrate.values()) //
			.accessMode(AccessMode.READ_WRITE)), //

	// IntegerWriteChannels
	EMS_ADDRESS(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.READ_WRITE)), //
	EMS_COMMUNICATION_TIMEOUT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS) //
			.accessMode(AccessMode.READ_WRITE)), //
	SYSTEM_TOTAL_CAPACITY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE_HOURS) //
			.accessMode(AccessMode.READ_WRITE)), //
	SET_SUB_MASTER_ADDRESS(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_WRITE) //
			.text("Starting from 0")), //
	VOLTAGE_LOW_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE) //
			.text("Default: 2650mV")), //

//	CELL_VOLTAGE_PROTECT(Doc.of(OpenemsType.INTEGER) //
//			.unit(Unit.MILLIVOLT) //
//			.accessMode(AccessMode.READ_WRITE)), //
//	CELL_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
//			.unit(Unit.MILLIVOLT) //
//			.accessMode(AccessMode.READ_WRITE)), //
//	WORK_PARAMETER_PCS_COMMUNICATION_RATE(Doc.of(OpenemsType.INTEGER) //
//			.unit(Unit.NONE) //
//			.accessMode(AccessMode.READ_WRITE)), //
//	AUTO_SET_SLAVES_ID(Doc.of(AutoSetFunction.values()) //
//			.accessMode(AccessMode.READ_WRITE)), //
//	AUTO_SET_SLAVES_TEMPERATURE_ID(Doc.of(AutoSetFunction.values()) //
//			.accessMode(AccessMode.READ_WRITE)), //
//	SYSTEM_RESET(Doc.of(OpenemsType.INTEGER) //
//			.unit(Unit.NONE) //
//			.text("Resets the system") //
//			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_CELL_OVER_TEMPERATURE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_CELL_OVER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_CELL_OVER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_SYSTEM_OVER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_SYSTEM_OVER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_SYSTEM_CHARGE_OVER_CURRENT_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_CELL_UNDER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_CELL_UNDER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_SYSTEM_UNDER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_SYSTEM_UNDER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_CELL_OVER_TEMPERATURE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_CELL_UNDER_TEMPERATURE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_CELL_UNDER_TEMPERATURE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_SOC_LOW_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_SOC_LOW_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_CONNECTOR_TEMPERATURE_HIGH_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_INSULATION_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.OHM) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_INSULATION_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.OHM) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_CELL_VOLTAGE_DIFFERENCE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_DISCHARGE_TEMPERATURE_HIGH_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_DISCHARGE_TEMPERATURE_LOW_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_TEMPERATURE_DIFFERENCE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL2_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_CELL_OVER_TEMPERATURE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_CELL_OVER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_CELL_OVER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_SYSTEM_OVER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_SYSTEM_OVER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_SYSTEM_CHARGE_OVER_CURRENT_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_CELL_UNDER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_CELL_UNDER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_SYSTEM_UNDER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_SYSTEM_UNDER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_CELL_OVER_TEMPERATURE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_CELL_UNDER_TEMPERATURE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_CELL_UNDER_TEMPERATURE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_SOC_LOW_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_SOC_LOW_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_CONNECTOR_TEMPERATURE_HIGH_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_INSULATION_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.OHM) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_INSULATION_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.OHM) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_CELL_VOLTAGE_DIFFERENCE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_DISCHARGE_TEMPERATURE_HIGH_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_DISCHARGE_TEMPERATURE_LOW_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_TEMPERATURE_DIFFERENCE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	LEVEL1_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_CELL_OVER_VOLTAGE_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_CELL_OVER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_SYSTEM_OVER_VOLTAGE_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_SYSTEM_OVER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_SYSTEM_CHARGE_OVER_CURRENT_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_SYSTEM_CHARGE_OVER_CURRENT_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_CELL_UNDER_VOLTAGE_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_CELL_UNDER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_SYSTEM_UNDER_VOLTAGE_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_SYSTEM_UNDER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_SYSTEM_DISCHARGE_OVER_CURRENT_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_CELL_OVER_TEMPERATURE_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_CELL_OVER_TEMPERATURE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_CELL_UNDER_TEMPERATURE_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_CELL_UNDER_TEMPERATURE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_SOC_LOW_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_SOC_LOW_ALARM_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_CONNECTOR_TEMPERATURE_HIGH_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_CONNECTOR_TEMPERATURE_HIGH_ALARM_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_INSULATION_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.OHM) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_INSULATION_ALARM_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.OHM) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_CELL_VOLTAGE_DIFFERENCE_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_CELL_VOLTAGE_DIFFERENCE_ALARM_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_TOTAL_VOLTAGE_DIFFERENCE_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_TOTAL_VOLTAGE_DIFFERENCE_ALARM_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_DISCHARGE_TEMPERATURE_HIGH_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_DISCHARGE_TEMPERATURE_HIGH_ALARM_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_DISCHARGE_TEMPERATURE_LOW_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_DISCHARGE_TEMPERATURE_LOW_ALARM_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_TEMPERATURE_DIFFERENCE_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	PRE_ALARM_TEMPERATURE_DIFFERENCE_ALARM_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //

	// EnumReadChannels
	STATE_MACHINE(Doc.of(State.values()) //
			.text("Current State of State-Machine")), //
//	FAN_STATUS(Doc.of(FanStatus.values())), //
//	MAIN_CONTACTOR_STATE(Doc.of(ContactorState.values())), //
//	DRY_CONTACT_1_EXPORT(Doc.of(ContactExport.values())), //
//	DRY_CONTACT_2_EXPORT(Doc.of(ContactExport.values())), //
//	SYSTEM_RUN_MODE(Doc.of(SystemRunMode.values())), //
//	PRE_CONTACTOR_STATUS(Doc.of(PreContactorState.values())), //
//	SHORT_CIRCUIT_FUNCTION(Doc.of(ShortCircuitFunction.values())), //
////	CLUSTER_1_CHARGE_INDICATION(Doc.of(ChargeIndication.values())), //

	CLUSTER_1_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	CLUSTER_1_CURRENT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE)), // FS
	CLUSTER_1_SOH(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT)), //
	CLUSTER_1_MAX_CELL_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.text("Range: 1 ~ 512")), //
	CLUSTER_1_MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	CLUSTER_1_MIN_CELL_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.text("Range: 1 ~ 512")), //
	CLUSTER_1_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	CLUSTER_1_MAX_CELL_TEMPERATURE_ID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.text("Range: 1 ~ 512")), //
	CLUSTER_1_MAX_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.text("Range: -400 ~ 1500")), //
	CLUSTER_1_MIN_CELL_TEMPERATURE_ID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.text("Range: 1 ~ 512")), //
	CLUSTER_1_MIN_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.text("Range: -400 ~ 1500")), //
	CLUSTER_1_AVERAGE_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	CLUSTER_1_SYSTEM_INSULATION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.KILOOHM)), //
	SYSTEM_MAX_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE)),
	SYSTEM_MAX_DISCHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE)),
	POSITIVE_INSULATION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.KILOOHM)),
	NEGATIVE_INSULATION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.KILOOHM)),
	CLUSTER_RUN_STATE(Doc.of(ClusterRunState.values())), // TODO rename Cluster_1
	CLUSTER_1_AVG_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_PROJECT_ID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.text("Project Firmware Version")), //
	CLUSTER_1_VERSION_MAJOR(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.text("Project Firmware Version")), //
	CLUSTER_1_VERSION_SUB(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.text("Project Firmware Version")), //
	CLUSTER_1_VERSION_MODIFY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.text("Project Firmware Version")), //

	// Faults and warnings
	// Alarm Level 2
	ALARM_LEVEL_2_DISCHARGE_TEMP_LOW(Doc.of(Level.FAULT) //
			.text("Discharge Temperature Low Alarm Level 2")), //
	ALARM_LEVEL_2_DISCHARGE_TEMP_HIGH(Doc.of(Level.FAULT) //
			.text("Discharge Temperature High Alarm Level 2")), //
	ALARM_LEVEL_2_INSULATION_VALUE(Doc.of(Level.FAULT) //
			.text("Insulation Value Failure Alarm Level 2")), //
	ALARM_LEVEL_2_POWER_POLE_TEMP_HIGH(Doc.of(Level.FAULT) //
			.text("Power Pole temperature too high Alarm Level 2")), //
	ALARM_LEVEL_2_CHARGE_TEMP_LOW(Doc.of(Level.FAULT) //
			.text("Cell Charge Temperature Low Alarm Level 2")), //
	ALARM_LEVEL_2_CHARGE_TEMP_HIGH(Doc.of(Level.FAULT) //
			.text("Charge Temperature High Alarm Level 2")), //
	ALARM_LEVEL_2_DISCHARGE_CURRENT_HIGH(Doc.of(Level.FAULT) //
			.text("Discharge Current High Alarm Level 2")), //
	ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW(Doc.of(Level.FAULT) //
			.text("Total Voltage Low Alarm Level 2")), //
	ALARM_LEVEL_2_CELL_VOLTAGE_LOW(Doc.of(Level.FAULT) //
			.text("Cell Voltage Low Alarm Level 2")), //
	ALARM_LEVEL_2_CHARGE_CURRENT_HIGH(Doc.of(Level.FAULT) //
			.text("Charge Current High Alarm Level 2")), //
	ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH(Doc.of(Level.FAULT) //
			.text("Total Voltage High Alarm Level 2")), //
	ALARM_LEVEL_2_CELL_VOLTAGE_HIGH(Doc.of(Level.FAULT) //
			.text("Cell Voltage High Alarm Level 2")), //

	// Alarm Level 1
	ALARM_LEVEL_1_DISCHARGE_TEMP_LOW(Doc.of(Level.WARNING) //
			.text("Discharge Temperature Low Alarm Level 1")), //
	ALARM_LEVEL_1_DISCHARGE_TEMP_HIGH(Doc.of(Level.WARNING) //
			.text("Discharge Temperature High Alarm Level 1")), //
	ALARM_LEVEL_1_INSULATION_VALUE(Doc.of(Level.WARNING) //
			.text("Insulation Value Failure Alarm Level 1")), //
	ALARM_LEVEL_1_POWER_POLE_TEMP_HIGH(Doc.of(Level.WARNING) //
			.text("Power Pole temperature too high Alarm Level 1")), //
	ALARM_LEVEL_1_CHARGE_TEMP_LOW(Doc.of(Level.WARNING) //
			.text("Cell Charge Temperature Low Alarm Level 1")), //
	ALARM_LEVEL_1_CHARGE_TEMP_HIGH(Doc.of(Level.WARNING) //
			.text("Charge Temperature High Alarm Level 1")), //
	ALARM_LEVEL_1_DISCHARGE_CURRENT_HIGH(Doc.of(Level.WARNING) //
			.text("Discharge Current High Alarm Level 1")), //
	ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
			.text("Total Voltage Low Alarm Level 1")), //
	ALARM_LEVEL_1_CELL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
			.text("Cell Voltage Low Alarm Level 1")), //
	ALARM_LEVEL_1_CHARGE_CURRENT_HIGH(Doc.of(Level.WARNING) //
			.text("Charge Current High Alarm Level 1")), //
	ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
			.text("Total Voltage High Alarm Level 1")), //
	ALARM_LEVEL_1_CELL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
			.text("Cell Voltage High Alarm Level 1")), //

	// Pre-Alarm
	PRE_ALARM_CELL_VOLTAGE_HIGH(Doc.of(Level.INFO) //
			.text("Cell Voltage High Pre-Alarm")), //
	PRE_ALARM_TOTAL_VOLTAGE_HIGH(Doc.of(Level.INFO) //
			.text("Total Voltage High Pre-Alarm")), //
	PRE_ALARM_CHARGE_CURRENT_HIGH(Doc.of(Level.INFO) //
			.text("Charge Current High Pre-Alarm")), //
	PRE_ALARM_CELL_VOLTAGE_LOW(Doc.of(Level.INFO) //
			.text("Cell Voltage Low Pre-Alarm")), //
	PRE_ALARM_TOTAL_VOLTAGE_LOW(Doc.of(Level.INFO) //
			.text("Total Voltage Low Pre-Alarm")), //
	PRE_ALARM_DISCHARGE_CURRENT_HIGH(Doc.of(Level.INFO) //
			.text("Discharge Current High Pre-Alarm")), //
	PRE_ALARM_CHARGE_TEMP_HIGH(Doc.of(Level.INFO) //
			.text("Charge Temperature High Pre-Alarm")), //
	PRE_ALARM_CHARGE_TEMP_LOW(Doc.of(Level.INFO) //
			.text("Charge Temperature Low Pre-Alarm")), //
	PRE_ALARM_SOC_LOW(Doc.of(Level.INFO) //
			.text("State-Of-Charge Low Pre-Alarm")), //
	PRE_ALARM_TEMP_DIFF_TOO_BIG(Doc.of(Level.INFO) //
			.text("Temperature Difference Too Big Pre-Alarm")), //
	PRE_ALARM_POWER_POLE_HIGH(Doc.of(Level.INFO) //
			.text("Power Pole Temperature High Pre-Alarm")), //
	PRE_ALARM_CELL_VOLTAGE_DIFF_TOO_BIG(Doc.of(Level.INFO) //
			.text("Cell Voltage Difference Too Big Pre-Alarm")), //
	PRE_ALARM_INSULATION_FAIL(Doc.of(Level.INFO) //
			.text("Insulation Failure Pre-Alarm")), //
	PRE_ALARM_TOTAL_VOLTAGE_DIFF_TOO_BIG(Doc.of(Level.INFO) //
			.text("Total Voltage Difference Too Big Pre-Alarm")), //
	PRE_ALARM_DISCHARGE_TEMP_HIGH(Doc.of(Level.INFO) //
			.text("Discharge Temperature High Pre-Alarm")), //
	PRE_ALARM_DISCHARGE_TEMP_LOW(Doc.of(Level.INFO) //
			.text("Discharge Temperature Low Pre-Alarm")), //

	// Other Alarm Info
	ALARM_COMMUNICATION_TO_MASTER_BMS(Doc.of(Level.WARNING) //
			.text("Communication Failure to Master BMS")), //
	ALARM_COMMUNICATION_TO_SLAVE_BMS(Doc.of(Level.WARNING) //
			.text("Communication Failure to Slave BMS")), //
	ALARM_COMMUNICATION_SLAVE_BMS_TO_TEMP_SENSORS(Doc.of(Level.WARNING) //
			.text("Communication Failure between Slave BMS and Temperature Sensors")), //
	ALARM_SLAVE_BMS_HARDWARE(Doc.of(Level.WARNING) //
			.text("Slave BMS Hardware Failure")), //

	// Slave BMS Fault Message Registers
	SLAVE_BMS_VOLTAGE_SENSOR_CABLES(Doc.of(Level.WARNING) //
			.text("Slave BMS Hardware: Voltage Sensor Cables Fault")), //
	SLAVE_BMS_POWER_CABLE(Doc.of(Level.WARNING) //
			.text("Slave BMS Hardware: Power Cable Fault")), //
	SLAVE_BMS_LTC6803(Doc.of(Level.WARNING) //
			.text("Slave BMS Hardware: LTC6803 Fault")), //
	SLAVE_BMS_VOLTAGE_SENSORS(Doc.of(Level.WARNING) //
			.text("Slave BMS Hardware: Voltage Sensors Fault")), //
	SLAVE_BMS_TEMP_SENSOR_CABLES(Doc.of(Level.WARNING) //
			.text("Slave BMS Hardware: Temperature Sensor Cables Fault")), //
	SLAVE_BMS_TEMP_SENSORS(Doc.of(Level.WARNING) //
			.text("Slave BMS Hardware: Temperature Sensors Fault")), //
	SLAVE_BMS_POWER_POLE_TEMP_SENSOR(Doc.of(Level.WARNING) //
			.text("Slave BMS Hardware: Power Pole Temperature Sensor Fault")), //
	SLAVE_BMS_TEMP_BOARD_COM(Doc.of(Level.WARNING) //
			.text("Slave BMS Hardware: Temperature Board COM Fault")), //
	SLAVE_BMS_BALANCE_MODULE(Doc.of(Level.WARNING) //
			.text("Slave BMS Hardware: Balance Module Fault")), //
	SLAVE_BMS_TEMP_SENSORS2(Doc.of(Level.WARNING) //
			.text("Slave BMS Hardware: Temperature Sensors Fault2")), //
	SLAVE_BMS_INTERNAL_COM(Doc.of(Level.WARNING) //
			.text("Slave BMS Hardware: Internal COM Fault")), //
	SLAVE_BMS_EEPROM(Doc.of(Level.WARNING) //
			.text("Slave BMS Hardware: EEPROM Fault")), //
	SLAVE_BMS_INIT(Doc.of(Level.WARNING) //
			.text("Slave BMS Hardware: Slave BMS Initialization Failure")), //
	;

	private final Doc doc;

	private SingleRackChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}