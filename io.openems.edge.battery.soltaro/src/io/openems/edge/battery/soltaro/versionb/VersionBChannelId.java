package io.openems.edge.battery.soltaro.versionb;

import io.openems.edge.battery.soltaro.versionb.VersionBEnums.AutoSetFunction;
import io.openems.edge.battery.soltaro.versionb.VersionBEnums.ChargeIndication;
import io.openems.edge.battery.soltaro.versionb.VersionBEnums.ClusterRunState;
import io.openems.edge.battery.soltaro.versionb.VersionBEnums.ContactExport;
import io.openems.edge.battery.soltaro.versionb.VersionBEnums.ContactorControl;
import io.openems.edge.battery.soltaro.versionb.VersionBEnums.ContactorState;
import io.openems.edge.battery.soltaro.versionb.VersionBEnums.FanStatus;
import io.openems.edge.battery.soltaro.versionb.VersionBEnums.PreContactorState;
import io.openems.edge.battery.soltaro.versionb.VersionBEnums.ShortCircuitFunction;
import io.openems.edge.battery.soltaro.versionb.VersionBEnums.SystemRunMode;
import io.openems.edge.common.channel.doc.AccessMode;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.Unit;

public enum VersionBChannelId implements io.openems.edge.common.channel.doc.ChannelId {
	STATE_MACHINE(new Doc().level(Level.INFO).text("Current State of State-Machine").options(State.values())), //
	FAN_STATUS(new Doc().options(FanStatus.values())), //
	MAIN_CONTACTOR_STATE(new Doc().options(ContactorState.values())), //
	DRY_CONTACT_1_EXPORT(new Doc().options(ContactExport.values())), //
	DRY_CONTACT_2_EXPORT(new Doc().options(ContactExport.values())), //
	SYSTEM_RESET(new Doc().text("Resets the system").unit(Unit.NONE)), //
	SYSTEM_RUN_MODE(new Doc().options(SystemRunMode.values())), //
	PRE_CONTACTOR_STATUS(new Doc().options(PreContactorState.values())), //

	ALARM_FLAG_STATUS_DISCHARGE_TEMPERATURE_LOW(new Doc().text("Alarm flag status discharge temperature low")), //
	ALARM_FLAG_STATUS_DISCHARGE_TEMPERATURE_HIGH(new Doc().text("Alarm flag status discharge temperature high")), //
	ALARM_FLAG_STATUS_VOLTAGE_DIFFERENCE(new Doc().text("Alarm flag status voltage difference")), //
	ALARM_FLAG_STATUS_INSULATION_LOW(new Doc().text("Alarm flag status insulation low")), //
	ALARM_FLAG_STATUS_CELL_VOLTAGE_DIFFERENCE(new Doc().text("Alarm flag status cell voltage difference")), //
	ALARM_FLAG_STATUS_ELECTRODE_TEMPERATURE_HIGH(new Doc().text("Alarm flag status electrode temperature high")), //
	ALARM_FLAG_STATUS_TEMPERATURE_DIFFERENCE(new Doc().text("Alarm flag status temperature difference")), //
	ALARM_FLAG_STATUS_SOC_LOW(new Doc().text("Alarm flag status soc low")), //
	ALARM_FLAG_STATUS_CELL_OVER_TEMPERATURE(new Doc().text("Alarm flag status cell over temperature")), //
	ALARM_FLAG_STATUS_CELL_LOW_TEMPERATURE(new Doc().text("Alarm flag status cell low temperature")), //
	ALARM_FLAG_STATUS_DISCHARGE_OVER_CURRENT(new Doc().text("Alarm flag status discharge over current")), //
	ALARM_FLAG_STATUS_SYSTEM_LOW_VOLTAGE(new Doc().text("Alarm flag status system low voltage")), //
	ALARM_FLAG_STATUS_CELL_LOW_VOLTAGE(new Doc().text("Alarm flag status cell low voltage")), //
	ALARM_FLAG_STATUS_CHARGE_OVER_CURRENT(new Doc().text("Alarm flag status charge over current")), //
	ALARM_FLAG_STATUS_SYSTEM_OVER_VOLTAGE(new Doc().text("Alarm flag status system over voltage")), //
	ALARM_FLAG_STATUS_CELL_OVER_VOLTAGE(new Doc().text("Alarm flag status cell over voltage")), //

	PROTECT_FLAG_STATUS_DISCHARGE_TEMPERATURE_LOW(new Doc().text("Protect flag status discharge temperature low")), //
	PROTECT_FLAG_STATUS_DISCHARGE_TEMPERATURE_HIGH(
			new Doc().text("Protect flag status discharge temperature high")), //
	PROTECT_FLAG_STATUS_VOLTAGE_DIFFERENCE(new Doc().text("Protect flag status voltage difference")), //
	PROTECT_FLAG_STATUS_INSULATION_LOW(new Doc().text("Protect flag status insulation low")), //
	PROTECT_FLAG_STATUS_CELL_VOLTAGE_DIFFERENCE(new Doc().text("Protect flag status cell voltage difference")), //
	PROTECT_FLAG_STATUS_ELECTRODE_TEMPERATURE_HIGH(
			new Doc().text("Protect flag status electrode temperature high")), //
	PROTECT_FLAG_STATUS_TEMPERATURE_DIFFERENCE(new Doc().text("Protect flag status temperature difference")), //
	PROTECT_FLAG_STATUS_SOC_LOW(new Doc().text("Protect flag status soc low")), //
	PROTECT_FLAG_STATUS_CELL_OVER_TEMPERATURE(new Doc().text("Protect flag status cell over temperature")), //
	PROTECT_FLAG_STATUS_CELL_LOW_TEMPERATURE(new Doc().text("Protect flag status cell low temperature")), //
	PROTECT_FLAG_STATUS_DISCHARGE_OVER_CURRENT(new Doc().text("Protect flag status discharge over current")), //
	PROTECT_FLAG_STATUS_SYSTEM_LOW_VOLTAGE(new Doc().text("Protect flag status system low voltage")), //
	PROTECT_FLAG_STATUS_CELL_LOW_VOLTAGE(new Doc().text("Protect flag status cell low voltage")), //
	PROTECT_FLAG_STATUS_CHARGE_OVER_CURRENT(new Doc().text("Protect flag status charge over current")), //
	PROTECT_FLAG_STATUS_SYSTEM_OVER_VOLTAGE(new Doc().text("Protect flag status system over voltage")), //
	PROTECT_FLAG_STATUS_CELL_OVER_VOLTAGE(new Doc().text("Protect flag status cell over voltage")), //

	ALARM_FLAG_REGISTER_1(new Doc()),
	ALARM_FLAG_REGISTER_1_TEMPERATURE_LOW(new Doc().text("Enable/Disable alarm temperature low")), //
	ALARM_FLAG_REGISTER_1_TEMPERATURE_HIGH(new Doc().text("Enable/Disable alarm temperature high")), //
	ALARM_FLAG_REGISTER_1_DISCHARGE_OVER_CURRENT(new Doc().text("Enable/Disable alarm discharge over current")), //
	ALARM_FLAG_REGISTER_1_SYSTEM_VOLTAGE_LOW(new Doc().text("Enable/Disable alarm system voltage low")), //
	ALARM_FLAG_REGISTER_1_CELL_VOLTAGE_LOW(new Doc().text("Enable/Disable alarm cell voltage low")), //
	ALARM_FLAG_REGISTER_1_CHARGE_OVER_CURRENT(new Doc().text("Enable/Disable alarm charge over current")), //
	ALARM_FLAG_REGISTER_1_SYSTEM_OVER_VOLTAGE(new Doc().text("Enable/Disable alarm system over voltage")), //
	ALARM_FLAG_REGISTER_1_CELL_OVER_VOLTAGE(new Doc().text("Enable/Disable alarm cell over voltage")), //

	ALARM_FLAG_REGISTER_2(new Doc()),
	ALARM_FLAG_REGISTER_2_CELL_VOLTAGE_DIFFERENCE(new Doc().text("Enable/Disable alarm cell voltage difference")), //
	ALARM_FLAG_REGISTER_2_POLE_TEMPERATURE_LOW(new Doc().text("Enable/Disable alarm pole temperature low")), //
	ALARM_FLAG_REGISTER_2_POLE_TEMPERATURE_HIGH(new Doc().text("Enable/Disable alarm pole temperature high")), //
	ALARM_FLAG_REGISTER_2_SOC_HIGH(new Doc().text("Enable/Disable alarm soc high")), //
	ALARM_FLAG_REGISTER_2_SOC_LOW(new Doc().text("Enable/Disable alarm soc low")), //
	
	PROTECT_FLAG_REGISTER_1(new Doc()),
	PROTECT_FLAG_REGISTER_1_TEMPERATURE_LOW(new Doc().text("Enable/Disable protect temperature low")), //
	PROTECT_FLAG_REGISTER_1_TEMPERATURE_HIGH(new Doc().text("Enable/Disable protect temperature high")), //
	PROTECT_FLAG_REGISTER_1_DISCHARGE_OVER_CURRENT(new Doc().text("Enable/Disable protect discharge over current")), //
	PROTECT_FLAG_REGISTER_1_SYSTEM_VOLTAGE_LOW(new Doc().text("Enable/Disable protect system voltage low")), //
	PROTECT_FLAG_REGISTER_1_CELL_VOLTAGE_LOW(new Doc().text("Enable/Disable protect cell voltage low")), //
	PROTECT_FLAG_REGISTER_1_CHARGE_OVER_CURRENT(new Doc().text("Enable/Disable protect charge over current")), //
	PROTECT_FLAG_REGISTER_1_SYSTEM_OVER_VOLTAGE(new Doc().text("Enable/Disable protect system over voltage")), //
	PROTECT_FLAG_REGISTER_1_CELL_OVER_VOLTAGE(new Doc().text("Enable/Disable protect cell over voltage")), //

	PROTECT_FLAG_REGISTER_2(new Doc()),
	PROTECT_FLAG_REGISTER_2_CELL_VOLTAGE_DIFFERENCE(
			new Doc().text("Enable/Disable protect cell voltage difference")), //
	PROTECT_FLAG_REGISTER_2_POLE_TEMPERATURE_LOW(new Doc().text("Enable/Disable protect pole temperature low")), //
	PROTECT_FLAG_REGISTER_2_POLE_TEMPERATURE_HIGH(new Doc().text("Enable/Disable protect pole temperature high")), //
	PROTECT_FLAG_REGISTER_2_SOC_HIGH(new Doc().text("Enable/Disable protect soc high")), //
	PROTECT_FLAG_REGISTER_2_SOC_LOW(new Doc().text("Enable/Disable protect soc low")), //

	SHORT_CIRCUIT_FUNCTION(new Doc().options(ShortCircuitFunction.values())), //
	TESTING_IO(new Doc()), //
	SOFT_SHUTDOWN(new Doc()), //
	BMS_CONTACTOR_CONTROL(new Doc().options(ContactorControl.values())), //
	CURRENT_BOX_SELF_CALIBRATION(new Doc()), //
	PCS_ALARM_RESET(new Doc()), //
	INSULATION_SENSOR_FUNCTION(new Doc()), //
	AUTO_SET_SLAVES_ID(new Doc().options(AutoSetFunction.values())), //
	AUTO_SET_SLAVES_TEMPERATURE_ID(new Doc().options(AutoSetFunction.values())), //
	TRANSPARENT_MASTER(new Doc()), //
	SET_EMS_ADDRESS(new Doc()), //
	EMS_COMMUNICATION_TIMEOUT(new Doc().unit(Unit.SECONDS)), //
	SLEEP(new Doc()), //
	VOLTAGE_LOW_PROTECTION(new Doc().unit(Unit.MILLIVOLT)), //

	STOP_PARAMETER_CELL_OVER_VOLTAGE_PROTECTION(new Doc().unit(Unit.MILLIVOLT)), //
	STOP_PARAMETER_CELL_OVER_VOLTAGE_RECOVER(new Doc().unit(Unit.MILLIVOLT)), //
	STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_PROTECTION(new Doc().unit(Unit.MILLIVOLT)), //
	STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER(new Doc().unit(Unit.MILLIVOLT)), //
	STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION(new Doc().unit(Unit.MILLIAMPERE)), //
	STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER(new Doc().unit(Unit.MILLIAMPERE)), //
	STOP_PARAMETER_CELL_UNDER_VOLTAGE_PROTECTION(new Doc().unit(Unit.MILLIVOLT)), //
	STOP_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER(new Doc().unit(Unit.MILLIVOLT)), //
	STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_PROTECTION(new Doc().unit(Unit.MILLIVOLT)), //
	STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER(new Doc().unit(Unit.VOLT)), //
	STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION(new Doc().unit(Unit.MILLIAMPERE)), //
	STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER(new Doc().unit(Unit.MILLIAMPERE)), //
	STOP_PARAMETER_CELL_OVER_TEMPERATURE_PROTECTION(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	STOP_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	STOP_PARAMETER_CELL_UNDER_TEMPERATURE_PROTECTION(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	STOP_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	STOP_PARAMETER_SOC_LOW_PROTECTION(new Doc().unit(Unit.PERCENT)), //
	STOP_PARAMETER_SOC_LOW_PROTECTION_RECOVER(new Doc().unit(Unit.PERCENT)), //
	STOP_PARAMETER_SOC_HIGH_PROTECTION(new Doc().unit(Unit.PERCENT)), //
	STOP_PARAMETER_SOC_HIGH_PROTECTION_RECOVER(new Doc().unit(Unit.PERCENT)), //
	STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	STOP_PARAMETER_INSULATION_PROTECTION(new Doc().unit(Unit.OHM)), //
	STOP_PARAMETER_INSULATION_PROTECTION_RECOVER(new Doc().unit(Unit.OHM)), //
	STOP_PARAMETER_CELL_VOLTAGE_DIFFERENCE_PROTECTION(new Doc().unit(Unit.MILLIVOLT)), //
	STOP_PARAMETER_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER(new Doc().unit(Unit.MILLIVOLT)), //
	STOP_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION(new Doc().unit(Unit.MILLIVOLT)), //
	STOP_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER(new Doc().unit(Unit.MILLIVOLT)), //
	STOP_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_PROTECTION(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	STOP_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	STOP_PARAMETER_DISCHARGE_TEMPERATURE_LOW_PROTECTION(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	STOP_PARAMETER_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	STOP_PARAMETER_TEMPERATURE_DIFFERENCE_PROTECTION(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	STOP_PARAMETER_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //

	WARN_PARAMETER_CELL_OVER_VOLTAGE_ALARM(new Doc().unit(Unit.MILLIVOLT)), //
	WARN_PARAMETER_CELL_OVER_VOLTAGE_RECOVER(new Doc().unit(Unit.MILLIVOLT)), //
	WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_ALARM(new Doc().unit(Unit.MILLIVOLT)), //
	WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER(new Doc().unit(Unit.MILLIVOLT)), //
	WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_ALARM(new Doc().unit(Unit.MILLIAMPERE)), //
	WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER(new Doc().unit(Unit.MILLIAMPERE)), //
	WARN_PARAMETER_CELL_UNDER_VOLTAGE_ALARM(new Doc().unit(Unit.MILLIVOLT)), //
	WARN_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER(new Doc().unit(Unit.MILLIVOLT)), //
	WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_ALARM(new Doc().unit(Unit.MILLIVOLT)), //
	WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER(new Doc().unit(Unit.MILLIVOLT)), //
	WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_ALARM(new Doc().unit(Unit.MILLIAMPERE)), //
	WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER(new Doc().unit(Unit.MILLIAMPERE)), //
	WARN_PARAMETER_CELL_OVER_TEMPERATURE_ALARM(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	WARN_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	WARN_PARAMETER_CELL_UNDER_TEMPERATURE_ALARM(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	WARN_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	WARN_PARAMETER_SOC_LOW_ALARM(new Doc().unit(Unit.PERCENT)), //
	WARN_PARAMETER_SOC_LOW_ALARM_RECOVER(new Doc().unit(Unit.PERCENT)), //
	WARN_PARAMETER_SOC_HIGH_ALARM(new Doc().unit(Unit.PERCENT)), //
	WARN_PARAMETER_SOC_HIGH_ALARM_RECOVER(new Doc().unit(Unit.PERCENT)), //
	WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM_RECOVER(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	WARN_PARAMETER_INSULATION_ALARM(new Doc().unit(Unit.OHM)), //
	WARN_PARAMETER_INSULATION_ALARM_RECOVER(new Doc().unit(Unit.OHM)), //
	WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM(new Doc().unit(Unit.MILLIVOLT)), //
	WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM_RECOVER(new Doc().unit(Unit.MILLIVOLT)), //
	WARN_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_ALARM(new Doc().unit(Unit.MILLIVOLT)), //
	WARN_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_ALARM_RECOVER(new Doc().unit(Unit.MILLIVOLT)), //
	WARN_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_ALARM(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	WARN_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_ALARM_RECOVER(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	WARN_PARAMETER_DISCHARGE_TEMPERATURE_LOW_ALARM(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	WARN_PARAMETER_DISCHARGE_TEMPERATURE_LOW_ALARM_RECOVER(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM_RECOVER(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //

	WORK_PARAMETER_PCS_MODBUS_ADDRESS(new Doc().unit(Unit.NONE)), //
	WORK_PARAMETER_PCS_COMMUNICATION_RATE(new Doc().unit(Unit.NONE)), //
	WORK_PARAMETER_CURRENT_FIX_COEFFICIENT(new Doc()), // 
	WORK_PARAMETER_CURRENT_FIX_OFFSET(new Doc()), //
	WORK_PARAMETER_SET_CHARGER_OUTPUT_CURRENT(new Doc()), // 
	WORK_PARAMETER_SYSTEM_RTC_TIME(new Doc()), //
	WORK_PARAMETER_SYSTEM_RTC_TIME_HIGH_BITS(new Doc()), //
	WORK_PARAMETER_SYSTEM_RTC_TIME_LOW_BITS(new Doc()), //
	WORK_PARAMETER_CELL_FLOAT_CHARGING(new Doc()), // 
	WORK_PARAMETER_CELL_AVERAGE_CHARGING(new Doc()), //
	WORK_PARAMETER_CELL_STOP_DISCHARGING(new Doc()), // 
	WORK_PARAMETER_SYSTEM_CAPACITY(new Doc()), //
	WORK_PARAMETER_SYSTEM_SOC(new Doc()), //
	WORK_PARAMETER_SYSTEM_SOH_DEFAULT_VALUE(new Doc()), //

	CLUSTER_1_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
	CLUSTER_1_CHARGE_INDICATION(new Doc().options(ChargeIndication.values())), //
	CLUSTER_1_SOH(new Doc().unit(Unit.THOUSANDTH)), //
	CLUSTER_1_MAX_CELL_VOLTAGE_ID(new Doc().unit(Unit.NONE)), //
	CLUSTER_1_MAX_CELL_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_MIN_CELL_VOLTAGE_ID(new Doc().unit(Unit.NONE)), //
	CLUSTER_1_MIN_CELL_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_MAX_CELL_TEMPERATURE_ID(new Doc().unit(Unit.NONE)), //
	CLUSTER_1_MAX_CELL_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_MIN_CELL_TEMPERATURE_ID(new Doc().unit(Unit.NONE)), //
	CLUSTER_1_MIN_CELL_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	MAX_CELL_RESISTANCE_ID(new Doc().unit(Unit.NONE)), //
	MAX_CELL_RESISTANCE(new Doc().unit(Unit.MIKROOHM)), //
	MIN_CELL_RESISTANCE_ID(new Doc().unit(Unit.NONE)), //
	MIN_CELL_RESISTANCE(new Doc().unit(Unit.MIKROOHM)), //
	POSITIVE_INSULATION(new Doc().unit(Unit.KILOOHM)), //
	NEGATIVE_INSULATION(new Doc().unit(Unit.KILOOHM)), //
	MAIN_CONTACTOR_FLAG(new Doc()), // 
	ENVIRONMENT_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	SYSTEM_INSULATION(new Doc().unit(Unit.KILOOHM)), //
	CELL_VOLTAGE_DIFFERENCE(new Doc().unit(Unit.MILLIVOLT)), //
	TOTAL_VOLTAGE_DIFFERENCE(new Doc().unit(Unit.MILLIVOLT)), //
	POWER_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELSIUS)), //
	POWER_SUPPLY_VOLTAGE(new Doc().unit(Unit.NONE)), //

	ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW(
			new Doc().level(Level.FAULT).text("Cell Discharge Temperature Low Alarm Level 2")), //
	ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH(
			new Doc().level(Level.FAULT).text("Cell Discharge Temperature High Alarm Level 2")), //
	ALARM_LEVEL_2_TOTAL_VOLTAGE_DIFFERENCE_HIGH(
			new Doc().level(Level.FAULT).text("Total voltage difference too high Alarm Level 2")), //
	ALARM_LEVEL_2_INSULATION_LOW(new Doc().level(Level.FAULT).text("Insulation Low Alarm Level 2")), //
	ALARM_LEVEL_2_CELL_VOLTAGE_DIFFERENCE_HIGH(
			new Doc().level(Level.FAULT).text("Cell voltage difference is too high Alarm Level 2")), //
	ALARM_LEVEL_2_POLES_TEMPERATURE_DIFFERENCE_HIGH(
			new Doc().level(Level.FAULT).text("Poles temperature difference is too high Alarm Level 2")), //
	ALARM_LEVEL_2_TEMPERATURE_DIFFERENCE_HIGH(
			new Doc().level(Level.FAULT).text("Temperature difference is too high Alarm Level 2")), //
	ALARM_LEVEL_2_SOC_LOW(new Doc().level(Level.FAULT).text("SoC Low Alarm Level 2")), //
	ALARM_LEVEL_2_CELL_CHA_TEMP_LOW(new Doc().level(Level.FAULT).text("Cell Charge Temperature Low Alarm Level 2")), //
	ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH(
			new Doc().level(Level.FAULT).text("Cell Charge Temperature High Alarm Level 2")), //
	ALARM_LEVEL_2_DISCHA_CURRENT_HIGH(new Doc().level(Level.FAULT).text("Discharge Current High Alarm Level 2")), //
	ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW(new Doc().level(Level.FAULT).text("Total Voltage Low Alarm Level 2")), //
	ALARM_LEVEL_2_CELL_VOLTAGE_LOW(new Doc().level(Level.FAULT).text("Cell Voltage Low Alarm Level 2")), //
	ALARM_LEVEL_2_CHA_CURRENT_HIGH(new Doc().level(Level.FAULT).text("Charge Current High Alarm Level 2")), //
	ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH(new Doc().level(Level.FAULT).text("Total Voltage High Alarm Level 2")), //
	ALARM_LEVEL_2_CELL_VOLTAGE_HIGH(new Doc().level(Level.FAULT).text("Cell Voltage High Alarm Level 2")), //

	ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW(
			new Doc().level(Level.WARNING).text("Cell Discharge Temperature Low Alarm Level 1")), //
	ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH(
			new Doc().level(Level.WARNING).text("Cell Discharge Temperature High Alarm Level 1")), //
	ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH(
			new Doc().level(Level.WARNING).text("Total Voltage Diff High Alarm Level 1")), //
	ALARM_LEVEL_1_INSULATION_LOW(new Doc().level(Level.WARNING).text("Insulation Low Alarm Level1")), //
	ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH(
			new Doc().level(Level.WARNING).text("Cell Voltage Diff High Alarm Level 1")), //
	ALARM_LEVEL_1_POLE_TEMPERATURE_TOO_HIGH(
			new Doc().level(Level.WARNING).text("Pole temperature too high Alarm Level 1")), //
	ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH(
			new Doc().level(Level.WARNING).text("Cell temperature Diff High Alarm Level 1")), //
	ALARM_LEVEL_1_SOC_LOW(new Doc().level(Level.WARNING).text("SOC Low Alarm Level 1")), //
	ALARM_LEVEL_1_CELL_CHA_TEMP_LOW(
			new Doc().level(Level.WARNING).text("Cell Charge Temperature Low Alarm Level 1")), //
	ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH(
			new Doc().level(Level.WARNING).text("Cell Charge Temperature High Alarm Level 1")), //
	ALARM_LEVEL_1_DISCHA_CURRENT_HIGH(new Doc().level(Level.WARNING).text("Discharge Current High Alarm Level 1")), //
	ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW(new Doc().level(Level.WARNING).text("Total Voltage Low Alarm Level 1")), //
	ALARM_LEVEL_1_CELL_VOLTAGE_LOW(new Doc().level(Level.WARNING).text("Cell Voltage Low Alarm Level 1")), //
	ALARM_LEVEL_1_CHA_CURRENT_HIGH(new Doc().level(Level.WARNING).text("Charge Current High Alarm Level 1")), //
	ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH(new Doc().level(Level.WARNING).text("Total Voltage High Alarm Level 1")), //
	ALARM_LEVEL_1_CELL_VOLTAGE_HIGH(new Doc().level(Level.WARNING).text("Cell Voltage High Alarm Level 1")), //
	
	CLUSTER_RUN_STATE(new Doc().options(ClusterRunState.values())), //
	
	MAXIMUM_CELL_VOLTAGE_NUMBER_WHEN_ALARM(new Doc().unit(Unit.NONE)), //
	MAXIMUM_CELL_VOLTAGE_WHEN_ALARM(new Doc().unit(Unit.MILLIVOLT)), //
	MAXIMUM_CELL_VOLTAGE_NUMBER_WHEN_STOPPED(new Doc().unit(Unit.NONE)), //
	MAXIMUM_CELL_VOLTAGE_WHEN_STOPPED(new Doc().unit(Unit.MILLIVOLT)), //
	MINIMUM_CELL_VOLTAGE_NUMBER_WHEN_ALARM(new Doc().unit(Unit.NONE)), //
	MINIMUM_CELL_VOLTAGE_WHEN_ALARM(new Doc().unit(Unit.MILLIVOLT)), //
	MINIMUM_CELL_VOLTAGE_NUMBER_WHEN_STOPPED(new Doc().unit(Unit.NONE)), //
	MINIMUM_CELL_VOLTAGE_WHEN_STOPPED(new Doc().unit(Unit.MILLIVOLT)), //			
	OVER_VOLTAGE_VALUE_WHEN_ALARM(new Doc().unit(Unit.MILLIVOLT)), //
	OVER_VOLTAGE_VALUE_WHEN_STOPPED(new Doc().unit(Unit.MILLIVOLT)), //
	UNDER_VOLTAGE_VALUE_WHEN_ALARM(new Doc().unit(Unit.MILLIVOLT)), //
	UNDER_VOLTAGE_VALUE_WHEN_STOPPED(new Doc().unit(Unit.MILLIVOLT)), //
	OVER_CHARGE_CURRENT_WHEN_ALARM(new Doc().unit(Unit.MILLIAMPERE)), //
	OVER_CHARGE_CURRENT_WHEN_STOPPED(new Doc().unit(Unit.MILLIAMPERE)), //
	OVER_DISCHARGE_CURRENT_WHEN_ALARM(new Doc().unit(Unit.MILLIAMPERE)), //
	OVER_DISCHARGE_CURRENT_WHEN_STOPPED(new Doc().unit(Unit.MILLIAMPERE)), //
	NUMBER_OF_TEMPERATURE_WHEN_ALARM(new Doc().unit(Unit.NONE)), //
	OTHER_ALARM_EQUIPMENT_FAILURE(new Doc().level(Level.WARNING).unit(Unit.NONE)), //
	SYSTEM_MAX_CHARGE_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
	SYSTEM_MAX_DISCHARGE_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
	
	CYCLE_TIME(new Doc().unit(Unit.NONE)), //
	TOTAL_CAPACITY_HIGH_BITS(new Doc().unit(Unit.NONE)), //
	TOTAL_CAPACITY_LOW_BITS(new Doc().unit(Unit.NONE)), //
	
	SLAVE_1_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 1 communication error")), //
	SLAVE_2_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 2 communication error")), //
	SLAVE_3_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 3 communication error")), //
	SLAVE_4_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 4 communication error")), //
	SLAVE_5_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 5 communication error")), //
	SLAVE_6_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 6 communication error")), //
	SLAVE_7_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 7 communication error")), //
	SLAVE_8_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 8 communication error")), //
	SLAVE_9_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 9 communication error")), //
	SLAVE_10_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 10 communication error")), //
	SLAVE_11_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 11 communication error")), //
	SLAVE_12_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 12 communication error")), //
	SLAVE_13_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 13 communication error")), //
	SLAVE_14_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 14 communication error")), //
	SLAVE_15_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 15 communication error")), //
	SLAVE_16_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 16 communication error")), //
	SLAVE_17_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 17 communication error")), //
	SLAVE_18_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 18 communication error")), //
	SLAVE_19_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 19 communication error")), //
	SLAVE_20_COMMUNICATION_ERROR(new Doc().level(Level.WARNING).text("Slave 20 communication error")), //
	
	FAILURE_INITIALIZATION(new Doc().level(Level.FAULT).text("Initialization failure")), //
	FAILURE_EEPROM(new Doc().level(Level.FAULT).text("EEPROM fault")), //
	FAILURE_INTRANET_COMMUNICATION(new Doc().level(Level.FAULT).text("Intranet communication fault")), //
	FAILURE_TEMP_SAMPLING_LINE(new Doc().level(Level.FAULT).text("Temperature sampling line fault")), //
	FAILURE_BALANCING_MODULE(new Doc().level(Level.FAULT).text("Balancing module fault")), //
	FAILURE_PCB(new Doc().level(Level.FAULT).text("PCB error")), //
	FAILURE_GR_T(new Doc().level(Level.FAULT).text("GR T error")), //
	FAILURE_TEMP_SENSOR(new Doc().level(Level.FAULT).text("Temperature sensor fault")), //
	FAILURE_TEMP_SAMPLING(new Doc().level(Level.FAULT).text("Temperature sampling fault")), //
	FAILURE_VOLTAGE_SAMPLING(new Doc().level(Level.FAULT).text("Voltage sampling fault")), //
	FAILURE_LTC6803(new Doc().level(Level.FAULT).text("LTC6803 fault")), //
	FAILURE_CONNECTOR_WIRE(new Doc().level(Level.FAULT).text("connector wire fault")), //
	FAILURE_SAMPLING_WIRE(new Doc().level(Level.FAULT).text("sampling wire fault")), //
	PRECHARGE_TAKING_TOO_LONG(new Doc().level(Level.FAULT).text("precharge time was too long")), //

	SYSTEM_TIME_HIGH(new Doc().unit(Unit.NONE)), //
	SYSTEM_TIME_LOW(new Doc().unit(Unit.NONE)), //
	LAST_TIME_CHARGE_CAPACITY_LOW_BITS(new Doc().unit(Unit.MILLIAMPERE_HOUR)), //		
	LAST_TIME_CHARGE_END_TIME_HIGH_BITS(new Doc().unit(Unit.NONE)), //
	LAST_TIME_CHARGE_END_TIME_LOW_BITS(new Doc().unit(Unit.NONE)), //
	
	LAST_TIME_DISCHARGE_CAPACITY_LOW_BITS(new Doc().unit(Unit.MILLIAMPERE_HOUR)), //
	LAST_TIME_DISCHARGE_END_TIME_HIGH_BITS(new Doc().unit(Unit.NONE)), //
	LAST_TIME_DISCHARGE_END_TIME_LOW_BITS(new Doc().unit(Unit.NONE)), //
	
	CELL_OVER_VOLTAGE_STOP_TIMES(new Doc().unit(Unit.NONE)), //
	BATTERY_OVER_VOLTAGE_STOP_TIMES(new Doc().unit(Unit.NONE)), //
	BATTERY_CHARGE_OVER_CURRENT_STOP_TIMES(new Doc().unit(Unit.NONE)), //
	CELL_VOLTAGE_LOW_STOP_TIMES(new Doc().unit(Unit.NONE)), //
	BATTERY_VOLTAGE_LOW_STOP_TIMES(new Doc().unit(Unit.NONE)), //
	BATTERY_DISCHARGE_OVER_CURRENT_STOP_TIMES(new Doc().unit(Unit.NONE)), //
	BATTERY_OVER_TEMPERATURE_STOP_TIMES(new Doc().unit(Unit.NONE)), //
	BATTERY_TEMPERATURE_LOW_STOP_TIMES(new Doc().unit(Unit.NONE)), //
	
	CELL_OVER_VOLTAGE_ALARM_TIMES(new Doc().unit(Unit.NONE)), //
	BATTERY_OVER_VOLTAGE_ALARM_TIMES(new Doc().unit(Unit.NONE)), //
	BATTERY_CHARGE_OVER_CURRENT_ALARM_TIMES(new Doc().unit(Unit.NONE)), //
	CELL_VOLTAGE_LOW_ALARM_TIMES(new Doc().unit(Unit.NONE)), //
	BATTERY_VOLTAGE_LOW_ALARM_TIMES(new Doc().unit(Unit.NONE)), //
	BATTERY_DISCHARGE_OVER_CURRENT_ALARM_TIMES(new Doc().unit(Unit.NONE)), //
	BATTERY_OVER_TEMPERATURE_ALARM_TIMES(new Doc().unit(Unit.NONE)), //
	BATTERY_TEMPERATURE_LOW_ALARM_TIMES(new Doc().unit(Unit.NONE)), //
	SYSTEM_SHORT_CIRCUIT_PROTECTION_TIMES(new Doc().unit(Unit.NONE)), //
	SYSTEM_GR_OVER_TEMPERATURE_STOP_TIMES(new Doc().unit(Unit.NONE)), //
	SYSTEM_GR_OVER_TEMPERATURE_ALARM_TIMES(new Doc().unit(Unit.NONE)), //
	BATTERY_VOLTAGE_DIFFERENCE_ALARM_TIMES(new Doc().unit(Unit.NONE)), //
	BATTERY_VOLTAGE_DIFFERENCE_STOP_TIMES(new Doc().unit(Unit.NONE)), //
	
	SLAVE_TEMPERATURE_COMMUNICATION_ERROR_HIGH(new Doc().unit(Unit.NONE)), //
	SLAVE_TEMPERATURE_COMMUNICATION_ERROR_LOW(new Doc().unit(Unit.NONE)), //
	
	CELL_VOLTAGE_PROTECT(new Doc().accessMode(AccessMode.READ_WRITE).unit(Unit.MILLIVOLT)), //
	CELL_VOLTAGE_RECOVER(new Doc().accessMode(AccessMode.READ_WRITE).unit(Unit.MILLIVOLT)), //

	CLUSTER_1_BATTERY_000_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_001_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_002_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_003_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_004_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_005_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_006_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_007_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_008_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_009_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_010_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_011_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_012_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_013_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_014_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_015_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_016_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_017_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_018_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_019_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_020_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_021_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_022_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_023_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_024_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_025_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_026_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_027_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_028_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_029_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_030_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_031_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_032_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_033_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_034_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_035_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_036_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_037_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_038_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_039_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_040_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_041_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_042_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_043_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_044_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_045_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_046_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_047_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_048_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_049_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_050_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_051_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_052_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_053_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_054_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_055_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_056_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_057_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_058_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_059_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_060_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_061_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_062_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_063_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_064_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_065_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_066_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_067_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_068_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_069_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_070_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_071_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_072_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_073_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_074_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_075_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_076_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_077_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_078_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_079_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_080_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_081_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_082_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_083_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_084_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_085_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_086_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_087_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_088_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_089_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_090_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_091_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_092_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_093_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_094_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_095_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_096_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_097_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_098_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_099_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_100_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_101_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_102_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_103_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_104_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_105_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_106_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_107_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_108_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_109_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_110_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_111_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_112_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_113_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_114_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_115_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_116_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_117_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_118_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_119_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_120_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_121_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_122_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_123_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_124_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_125_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_126_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_127_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_128_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_129_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_130_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_131_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_132_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_133_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_134_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_135_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_136_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_137_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_138_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_139_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_140_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_141_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_142_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_143_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_144_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_145_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_146_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_147_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_148_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_149_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_150_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_151_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_152_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_153_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_154_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_155_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_156_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_157_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_158_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_159_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_160_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_161_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_162_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_163_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_164_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_165_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_166_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_167_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_168_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_169_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_170_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_171_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_172_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_173_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_174_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_175_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_176_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_177_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_178_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_179_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_180_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_181_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_182_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_183_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_184_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_185_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_186_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_187_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_188_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_189_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_190_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_191_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_192_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_193_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_194_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_195_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_196_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_197_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_198_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_199_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_200_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_201_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_202_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_203_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_204_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_205_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_206_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_207_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_208_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_209_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_210_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_211_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_212_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_213_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_214_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_215_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_216_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_217_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_218_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_219_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_220_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_221_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_222_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_223_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_224_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_225_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_226_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_227_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_228_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_229_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_230_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_231_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_232_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_233_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_234_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_235_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_236_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_237_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_238_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	CLUSTER_1_BATTERY_239_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //

	CLUSTER_1_BATTERY_000_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_001_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_002_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_003_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_004_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_005_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_006_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_007_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_008_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_009_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_010_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_011_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_012_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_013_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_014_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_015_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_016_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_017_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_018_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_019_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_020_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_021_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_022_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_023_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_024_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_025_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_026_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_027_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_028_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_029_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_030_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_031_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_032_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_033_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_034_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_035_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_036_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_037_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_038_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_039_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_040_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_041_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_042_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_043_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_044_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_045_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_046_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_047_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_048_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_049_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_050_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_051_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_052_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_053_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_054_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_055_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_056_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_057_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_058_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_059_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_060_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_061_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_062_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_063_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_064_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_065_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_066_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_067_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_068_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_069_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_070_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_071_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_072_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_073_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_074_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_075_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_076_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_077_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_078_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_079_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_080_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_081_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_082_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_083_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_084_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_085_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_086_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_087_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_088_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_089_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_090_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_091_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_092_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_093_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_094_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_095_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_096_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_097_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_098_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_099_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_100_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_101_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_102_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_103_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_104_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_105_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_106_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_107_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_108_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_109_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_110_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_111_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_112_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_113_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_114_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_115_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_116_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_117_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_118_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_119_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_120_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_121_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_122_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_123_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_124_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_125_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_126_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_127_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_128_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_129_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_130_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_131_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_132_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_133_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_134_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_135_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_136_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_137_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_138_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_139_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_140_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_141_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_142_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_143_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_144_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_145_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_146_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_147_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_148_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_149_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_150_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_151_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_152_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_153_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_154_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_155_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_156_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_157_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_158_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_159_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_160_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_161_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_162_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_163_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_164_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_165_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_166_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_167_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_168_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_169_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_170_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_171_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_172_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_173_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_174_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_175_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_176_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_177_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_178_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_179_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_180_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_181_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_182_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_183_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_184_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_185_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_186_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_187_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_188_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_189_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_190_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_191_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_192_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_193_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_194_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_195_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_196_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_197_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_198_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_199_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_200_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_201_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_202_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_203_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_204_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_205_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_206_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_207_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_208_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_209_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_210_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_211_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_212_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_213_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_214_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_215_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_216_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_217_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_218_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_219_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_220_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_221_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_222_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_223_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_224_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_225_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_226_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_227_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_228_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_229_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_230_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_231_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_232_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_233_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_234_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_235_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_236_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_237_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_238_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_BATTERY_239_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	;
	private final Doc doc;

	private VersionBChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}