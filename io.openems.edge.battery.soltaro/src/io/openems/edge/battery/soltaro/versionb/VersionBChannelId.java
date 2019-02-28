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
	
	//TODO What is a real error that causes malfunction that a manual operation is needed?
	FAILURE_INITIALIZATION(new Doc().level(Level.FAULT).text("Initialization failure")), //
	FAILURE_EEPROM(new Doc().level(Level.FAULT).text("EEPROM fault")), //
	FAILURE_INTRANET_COMMUNICATION(new Doc().level(Level.FAULT).text("Intranet communication fault")), //
	FAILURE_TEMP_SAMPLING_LINE(new Doc().level(Level.FAULT).text("Temperature sampling line fault")), //
	FAILURE_BALANCING_MODULE(new Doc().level(Level.INFO).text("Balancing module fault")), //
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