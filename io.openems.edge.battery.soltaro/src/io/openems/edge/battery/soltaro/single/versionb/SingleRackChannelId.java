package io.openems.edge.battery.soltaro.single.versionb;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.soltaro.State;
import io.openems.edge.battery.soltaro.single.versionb.Enums.AutoSetFunction;
import io.openems.edge.battery.soltaro.single.versionb.Enums.ClusterRunState;
import io.openems.edge.battery.soltaro.single.versionb.Enums.ContactExport;
import io.openems.edge.battery.soltaro.single.versionb.Enums.ContactorControl;
import io.openems.edge.battery.soltaro.single.versionb.Enums.ContactorState;
import io.openems.edge.battery.soltaro.single.versionb.Enums.FanStatus;
import io.openems.edge.battery.soltaro.single.versionb.Enums.PreContactorState;
import io.openems.edge.battery.soltaro.single.versionb.Enums.ShortCircuitFunction;
import io.openems.edge.battery.soltaro.single.versionb.Enums.SystemRunMode;
import io.openems.edge.common.channel.Doc;

public enum SingleRackChannelId implements io.openems.edge.common.channel.ChannelId {
	// IntegerWriteChannels
	BMS_CONTACTOR_CONTROL(Doc.of(ContactorControl.values()) //
			.accessMode(AccessMode.READ_WRITE)), //
	CELL_VOLTAGE_PROTECT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	CELL_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	EMS_COMMUNICATION_TIMEOUT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS) //
			.accessMode(AccessMode.READ_WRITE)), //
	WORK_PARAMETER_PCS_COMMUNICATION_RATE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_WRITE)), //
	AUTO_SET_SLAVES_ID(Doc.of(AutoSetFunction.values()) //
			.accessMode(AccessMode.READ_WRITE)), //
	AUTO_SET_SLAVES_TEMPERATURE_ID(Doc.of(AutoSetFunction.values()) //
			.accessMode(AccessMode.READ_WRITE)), //
	SYSTEM_RESET(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.text("Resets the system") //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_CELL_OVER_TEMPERATURE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_CELL_OVER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_CELL_OVER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_CELL_UNDER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_CELL_UNDER_TEMPERATURE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_SOC_LOW_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_SOC_LOW_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_SOC_HIGH_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_SOC_HIGH_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_INSULATION_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.OHM) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_INSULATION_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.OHM) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_CELL_VOLTAGE_DIFFERENCE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_DISCHARGE_TEMPERATURE_LOW_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_TEMPERATURE_DIFFERENCE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	STOP_PARAMETER_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_CELL_OVER_VOLTAGE_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_CELL_OVER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_CELL_UNDER_VOLTAGE_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_CELL_OVER_TEMPERATURE_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_CELL_UNDER_TEMPERATURE_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_SOC_LOW_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_SOC_LOW_ALARM_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_SOC_HIGH_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_SOC_HIGH_ALARM_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_INSULATION_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.OHM) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_INSULATION_ALARM_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.OHM) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_ALARM_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_ALARM_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_DISCHARGE_TEMPERATURE_LOW_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_DISCHARGE_TEMPERATURE_LOW_ALARM_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //
	WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM_RECOVER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.accessMode(AccessMode.READ_WRITE)), //

	// EnumReadChannels
	STATE_MACHINE(Doc.of(State.values()) //
			.text("Current State of State-Machine")), //
	FAN_STATUS(Doc.of(FanStatus.values())), //
	MAIN_CONTACTOR_STATE(Doc.of(ContactorState.values())), //
	DRY_CONTACT_1_EXPORT(Doc.of(ContactExport.values())), //
	DRY_CONTACT_2_EXPORT(Doc.of(ContactExport.values())), //
	SYSTEM_RUN_MODE(Doc.of(SystemRunMode.values())), //
	PRE_CONTACTOR_STATUS(Doc.of(PreContactorState.values())), //
	SHORT_CIRCUIT_FUNCTION(Doc.of(ShortCircuitFunction.values())), //
//	CLUSTER_1_CHARGE_INDICATION(Doc.of(ChargeIndication.values())), //
	CLUSTER_RUN_STATE(Doc.of(ClusterRunState.values())), //

	// IntegerReadChannels
	MAXIMUM_CELL_VOLTAGE_NUMBER_WHEN_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	MAXIMUM_CELL_VOLTAGE_WHEN_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	MAXIMUM_CELL_VOLTAGE_NUMBER_WHEN_STOPPED(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	MAXIMUM_CELL_VOLTAGE_WHEN_STOPPED(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	MINIMUM_CELL_VOLTAGE_NUMBER_WHEN_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	MINIMUM_CELL_VOLTAGE_WHEN_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	MINIMUM_CELL_VOLTAGE_NUMBER_WHEN_STOPPED(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	MINIMUM_CELL_VOLTAGE_WHEN_STOPPED(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	OVER_VOLTAGE_VALUE_WHEN_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	OVER_VOLTAGE_VALUE_WHEN_STOPPED(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	UNDER_VOLTAGE_VALUE_WHEN_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	UNDER_VOLTAGE_VALUE_WHEN_STOPPED(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	OVER_CHARGE_CURRENT_WHEN_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE)), //
	OVER_CHARGE_CURRENT_WHEN_STOPPED(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE)), //
	OVER_DISCHARGE_CURRENT_WHEN_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE)), //
	OVER_DISCHARGE_CURRENT_WHEN_STOPPED(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE)), //
	NUMBER_OF_TEMPERATURE_WHEN_ALARM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	SYSTEM_MAX_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE)), //
	SYSTEM_MAX_DISCHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE)), //
	CYCLE_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	TOTAL_CAPACITY_HIGH_BITS(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	TOTAL_CAPACITY_LOW_BITS(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	ALARM_FLAG_REGISTER_1(Doc.of(OpenemsType.INTEGER)), //
	ALARM_FLAG_REGISTER_2(Doc.of(OpenemsType.INTEGER)), PROTECT_FLAG_REGISTER_1(Doc.of(OpenemsType.INTEGER)),
	TESTING_IO(Doc.of(OpenemsType.INTEGER)), //
	SOFT_SHUTDOWN(Doc.of(OpenemsType.INTEGER)), //
	CURRENT_BOX_SELF_CALIBRATION(Doc.of(OpenemsType.INTEGER)), //
	PCS_ALARM_RESET(Doc.of(OpenemsType.INTEGER)), //
	INSULATION_SENSOR_FUNCTION(Doc.of(OpenemsType.INTEGER)), //
	TRANSPARENT_MASTER(Doc.of(OpenemsType.INTEGER)), //
	SET_EMS_ADDRESS(Doc.of(OpenemsType.INTEGER)), //
	SLEEP(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)), //), //
	VOLTAGE_LOW_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	WORK_PARAMETER_CURRENT_FIX_COEFFICIENT(Doc.of(OpenemsType.INTEGER)), //
	WORK_PARAMETER_CURRENT_FIX_OFFSET(Doc.of(OpenemsType.INTEGER)), //
	WORK_PARAMETER_SET_CHARGER_OUTPUT_CURRENT(Doc.of(OpenemsType.INTEGER)), //
	WORK_PARAMETER_SYSTEM_RTC_TIME(Doc.of(OpenemsType.INTEGER)), //
	WORK_PARAMETER_SYSTEM_RTC_TIME_HIGH_BITS(Doc.of(OpenemsType.INTEGER)), //
	WORK_PARAMETER_SYSTEM_RTC_TIME_LOW_BITS(Doc.of(OpenemsType.INTEGER)), //
	WORK_PARAMETER_CELL_FLOAT_CHARGING(Doc.of(OpenemsType.INTEGER)), //
	WORK_PARAMETER_CELL_AVERAGE_CHARGING(Doc.of(OpenemsType.INTEGER)), //
	WORK_PARAMETER_CELL_STOP_DISCHARGING(Doc.of(OpenemsType.INTEGER)), //
	WORK_PARAMETER_SYSTEM_CAPACITY(Doc.of(OpenemsType.INTEGER)), //
	WORK_PARAMETER_SYSTEM_SOC(Doc.of(OpenemsType.INTEGER)), //
	WORK_PARAMETER_SYSTEM_SOH_DEFAULT_VALUE(Doc.of(OpenemsType.INTEGER)), //
	CLUSTER_1_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	CLUSTER_1_CURRENT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE)), //
	CLUSTER_1_SOH(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.THOUSANDTH)), //
	CLUSTER_1_MAX_CELL_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	CLUSTER_1_MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	CLUSTER_1_MIN_CELL_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	CLUSTER_1_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	CLUSTER_1_MAX_CELL_TEMPERATURE_ID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	CLUSTER_1_MAX_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS)), //
	CLUSTER_1_MIN_CELL_TEMPERATURE_ID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	CLUSTER_1_MIN_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS)), //
	MAX_CELL_RESISTANCE_ID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	MAX_CELL_RESISTANCE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MICROOHM)), //
	MIN_CELL_RESISTANCE_ID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	MIN_CELL_RESISTANCE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MICROOHM)), //
	POSITIVE_INSULATION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.KILOOHM)), //
	NEGATIVE_INSULATION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.KILOOHM)), //
	MAIN_CONTACTOR_FLAG(Doc.of(OpenemsType.INTEGER)), //
	ENVIRONMENT_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS)), //
	SYSTEM_INSULATION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.KILOOHM)), //
	CELL_VOLTAGE_DIFFERENCE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	TOTAL_VOLTAGE_DIFFERENCE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	POWER_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEGREE_CELSIUS)), //
	POWER_SUPPLY_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //

	SYSTEM_TIME_HIGH(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	SYSTEM_TIME_LOW(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	LAST_TIME_CHARGE_CAPACITY_LOW_BITS(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE_HOURS)), //
	LAST_TIME_CHARGE_END_TIME_HIGH_BITS(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	LAST_TIME_CHARGE_END_TIME_LOW_BITS(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //

	LAST_TIME_DISCHARGE_CAPACITY_LOW_BITS(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE_HOURS)), //
	LAST_TIME_DISCHARGE_END_TIME_HIGH_BITS(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	LAST_TIME_DISCHARGE_END_TIME_LOW_BITS(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //

	CELL_OVER_VOLTAGE_STOP_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	BATTERY_OVER_VOLTAGE_STOP_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	BATTERY_CHARGE_OVER_CURRENT_STOP_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	CELL_VOLTAGE_LOW_STOP_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	BATTERY_VOLTAGE_LOW_STOP_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	BATTERY_DISCHARGE_OVER_CURRENT_STOP_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	BATTERY_OVER_TEMPERATURE_STOP_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	BATTERY_TEMPERATURE_LOW_STOP_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //

	CELL_OVER_VOLTAGE_ALARM_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	BATTERY_OVER_VOLTAGE_ALARM_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	BATTERY_CHARGE_OVER_CURRENT_ALARM_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	CELL_VOLTAGE_LOW_ALARM_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	BATTERY_VOLTAGE_LOW_ALARM_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	BATTERY_DISCHARGE_OVER_CURRENT_ALARM_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	BATTERY_OVER_TEMPERATURE_ALARM_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	BATTERY_TEMPERATURE_LOW_ALARM_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	SYSTEM_SHORT_CIRCUIT_PROTECTION_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	SYSTEM_GR_OVER_TEMPERATURE_STOP_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	SYSTEM_GR_OVER_TEMPERATURE_ALARM_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	BATTERY_VOLTAGE_DIFFERENCE_ALARM_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	BATTERY_VOLTAGE_DIFFERENCE_STOP_TIMES(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //

	SLAVE_TEMPERATURE_COMMUNICATION_ERROR_HIGH(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	SLAVE_TEMPERATURE_COMMUNICATION_ERROR_LOW(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //

	// Config Channels
	ALARM_FLAG_STATUS_DISCHARGE_TEMPERATURE_LOW(Doc.of(OpenemsType.BOOLEAN) //
			.text("Alarm flag status discharge temperature low")), //
	ALARM_FLAG_STATUS_DISCHARGE_TEMPERATURE_HIGH(Doc.of(OpenemsType.BOOLEAN) //
			.text("Alarm flag status discharge temperature high")), //
	ALARM_FLAG_STATUS_VOLTAGE_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Alarm flag status voltage difference")), //
	ALARM_FLAG_STATUS_INSULATION_LOW(Doc.of(OpenemsType.BOOLEAN) //
			.text("Alarm flag status insulation low")), //
	ALARM_FLAG_STATUS_CELL_VOLTAGE_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Alarm flag status cell voltage difference")), //
	ALARM_FLAG_STATUS_ELECTRODE_TEMPERATURE_HIGH(Doc.of(OpenemsType.BOOLEAN) //
			.text("Alarm flag status electrode temperature high")), //
	ALARM_FLAG_STATUS_TEMPERATURE_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Alarm flag status temperature difference")), //
	ALARM_FLAG_STATUS_SOC_LOW(Doc.of(OpenemsType.BOOLEAN) //
			.text("Alarm flag status soc low")), //
	ALARM_FLAG_STATUS_CELL_OVER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Alarm flag status cell over temperature")), //
	ALARM_FLAG_STATUS_CELL_LOW_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Alarm flag status cell low temperature")), //
	ALARM_FLAG_STATUS_DISCHARGE_OVER_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
			.text("Alarm flag status discharge over current")), //
	ALARM_FLAG_STATUS_SYSTEM_LOW_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Alarm flag status system low voltage")), //
	ALARM_FLAG_STATUS_CELL_LOW_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Alarm flag status cell low voltage")), //
	ALARM_FLAG_STATUS_CHARGE_OVER_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
			.text("Alarm flag status charge over current")), //
	ALARM_FLAG_STATUS_SYSTEM_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Alarm flag status system over voltage")), //
	ALARM_FLAG_STATUS_CELL_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Alarm flag status cell over voltage")), //
	PROTECT_FLAG_STATUS_DISCHARGE_TEMPERATURE_LOW(Doc.of(OpenemsType.BOOLEAN) //
			.text("Protect flag status discharge temperature low")), //
	PROTECT_FLAG_STATUS_DISCHARGE_TEMPERATURE_HIGH(Doc.of(OpenemsType.BOOLEAN) //
			.text("Protect flag status discharge temperature high")), //
	PROTECT_FLAG_STATUS_VOLTAGE_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Protect flag status voltage difference")), //
	PROTECT_FLAG_STATUS_INSULATION_LOW(Doc.of(OpenemsType.BOOLEAN) //
			.text("Protect flag status insulation low")), //
	PROTECT_FLAG_STATUS_CELL_VOLTAGE_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Protect flag status cell voltage difference")), //
	PROTECT_FLAG_STATUS_ELECTRODE_TEMPERATURE_HIGH(Doc.of(OpenemsType.BOOLEAN) //
			.text("Protect flag status electrode temperature high")), //
	PROTECT_FLAG_STATUS_TEMPERATURE_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Protect flag status temperature difference")), //
	PROTECT_FLAG_STATUS_SOC_LOW(Doc.of(OpenemsType.BOOLEAN) //
			.text("Protect flag status soc low")), //
	PROTECT_FLAG_STATUS_CELL_OVER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Protect flag status cell over temperature")), //
	PROTECT_FLAG_STATUS_CELL_LOW_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Protect flag status cell low temperature")), //
	PROTECT_FLAG_STATUS_DISCHARGE_OVER_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
			.text("Protect flag status discharge over current")), //
	PROTECT_FLAG_STATUS_SYSTEM_LOW_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Protect flag status system low voltage")), //
	PROTECT_FLAG_STATUS_CELL_LOW_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Protect flag status cell low voltage")), //
	PROTECT_FLAG_STATUS_CHARGE_OVER_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
			.text("Protect flag status charge over current")), //
	PROTECT_FLAG_STATUS_SYSTEM_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Protect flag status system over voltage")), //
	PROTECT_FLAG_STATUS_CELL_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Protect flag status cell over voltage")), //
	ALARM_FLAG_REGISTER_1_TEMPERATURE_LOW(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable alarm temperature low")), //
	ALARM_FLAG_REGISTER_1_TEMPERATURE_HIGH(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable alarm temperature high")), //
	ALARM_FLAG_REGISTER_1_DISCHARGE_OVER_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable alarm discharge over current")), //
	ALARM_FLAG_REGISTER_1_SYSTEM_VOLTAGE_LOW(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable alarm system voltage low")), //
	ALARM_FLAG_REGISTER_1_CELL_VOLTAGE_LOW(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable alarm cell voltage low")), //
	ALARM_FLAG_REGISTER_1_CHARGE_OVER_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable alarm charge over current")), //
	ALARM_FLAG_REGISTER_1_SYSTEM_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable alarm system over voltage")), //
	ALARM_FLAG_REGISTER_1_CELL_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable alarm cell over voltage")), //
	ALARM_FLAG_REGISTER_2_CELL_VOLTAGE_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable alarm cell voltage difference")), //
	ALARM_FLAG_REGISTER_2_POLE_TEMPERATURE_LOW(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable alarm pole temperature low")), //
	ALARM_FLAG_REGISTER_2_POLE_TEMPERATURE_HIGH(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable alarm pole temperature high")), //
	ALARM_FLAG_REGISTER_2_SOC_HIGH(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable alarm soc high")), //
	ALARM_FLAG_REGISTER_2_SOC_LOW(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable alarm soc low")), //
	PROTECT_FLAG_REGISTER_1_TEMPERATURE_LOW(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable protect temperature low")), //
	PROTECT_FLAG_REGISTER_1_TEMPERATURE_HIGH(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable protect temperature high")), //
	PROTECT_FLAG_REGISTER_1_DISCHARGE_OVER_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable protect discharge over current")), //
	PROTECT_FLAG_REGISTER_1_SYSTEM_VOLTAGE_LOW(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable protect system voltage low")), //
	PROTECT_FLAG_REGISTER_1_CELL_VOLTAGE_LOW(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable protect cell voltage low")), //
	PROTECT_FLAG_REGISTER_1_CHARGE_OVER_CURRENT(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable protect charge over current")), //
	PROTECT_FLAG_REGISTER_1_SYSTEM_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable protect system over voltage")), //
	PROTECT_FLAG_REGISTER_1_CELL_OVER_VOLTAGE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable protect cell over voltage")), //
	PROTECT_FLAG_REGISTER_2(Doc.of(OpenemsType.BOOLEAN)),
	PROTECT_FLAG_REGISTER_2_CELL_VOLTAGE_DIFFERENCE(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable protect cell voltage difference")), //
	PROTECT_FLAG_REGISTER_2_POLE_TEMPERATURE_LOW(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable protect pole temperature low")), //
	PROTECT_FLAG_REGISTER_2_POLE_TEMPERATURE_HIGH(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable protect pole temperature high")), //
	PROTECT_FLAG_REGISTER_2_SOC_HIGH(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable protect soc high")), //
	PROTECT_FLAG_REGISTER_2_SOC_LOW(Doc.of(OpenemsType.BOOLEAN) //
			.text("Enable/Disable protect soc low")), //
	
	//Faults and warnings
	ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW(Doc.of(Level.FAULT) //
			.text("Cell Discharge Temperature Low Alarm Level 2")), //
	ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH(Doc.of(Level.FAULT) //
			.text("Cell Discharge Temperature High Alarm Level 2")), //
	ALARM_LEVEL_2_TOTAL_VOLTAGE_DIFFERENCE_HIGH(Doc.of(Level.FAULT) //
			.text("Total voltage difference too high Alarm Level 2")), //
	ALARM_LEVEL_2_INSULATION_LOW(Doc.of(Level.FAULT) //
			.text("Insulation Low Alarm Level 2")), //
	ALARM_LEVEL_2_CELL_VOLTAGE_DIFFERENCE_HIGH(Doc.of(Level.FAULT) //
			.text("Cell voltage difference is too high Alarm Level 2")), //
	ALARM_LEVEL_2_POLES_TEMPERATURE_DIFFERENCE_HIGH(Doc.of(Level.FAULT) //
			.text("Poles temperature difference is too high Alarm Level 2")), //
	ALARM_LEVEL_2_TEMPERATURE_DIFFERENCE_HIGH(Doc.of(Level.FAULT) //
			.text("Temperature difference is too high Alarm Level 2")), //
	ALARM_LEVEL_2_SOC_LOW(Doc.of(Level.FAULT) //
			.text("SoC Low Alarm Level 2")), //
	ALARM_LEVEL_2_CELL_CHA_TEMP_LOW(Doc.of(Level.FAULT) //
			.text("Cell Charge Temperature Low Alarm Level 2")), //
	ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH(Doc.of(Level.FAULT) //
			.text("Cell Charge Temperature High Alarm Level 2")), //
	ALARM_LEVEL_2_DISCHA_CURRENT_HIGH(Doc.of(Level.FAULT) //
			.text("Discharge Current High Alarm Level 2")), //
	ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW(Doc.of(Level.FAULT) //
			.text("Total Voltage Low Alarm Level 2")), //
	ALARM_LEVEL_2_CELL_VOLTAGE_LOW(Doc.of(Level.FAULT) //
			.text("Cell Voltage Low Alarm Level 2")), //
	ALARM_LEVEL_2_CHA_CURRENT_HIGH(Doc.of(Level.FAULT) //
			.text("Charge Current High Alarm Level 2")), //
	ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH(Doc.of(Level.FAULT) //
			.text("Total Voltage High Alarm Level 2")), //
	ALARM_LEVEL_2_CELL_VOLTAGE_HIGH(Doc.of(Level.FAULT) //
			.text("Cell Voltage High Alarm Level 2")), //
	ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW(Doc.of(Level.WARNING) //
			.text("Cell Discharge Temperature Low Alarm Level 1")), //
	ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH(Doc.of(Level.WARNING) //
			.text("Cell Discharge Temperature High Alarm Level 1")), //
	ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH(Doc.of(Level.WARNING) //
			.text("Total Voltage Diff High Alarm Level 1")), //
	ALARM_LEVEL_1_INSULATION_LOW(Doc.of(Level.WARNING) //
			.text("Insulation Low Alarm Level1")), //
	ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH(Doc.of(Level.WARNING) //
			.text("Cell Voltage Diff High Alarm Level 1")), //
	ALARM_LEVEL_1_POLE_TEMPERATURE_TOO_HIGH(Doc.of(Level.WARNING) //
			.text("Pole temperature too high Alarm Level 1")), //
	ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH(Doc.of(Level.WARNING) //
			.text("Cell temperature Diff High Alarm Level 1")), //
	ALARM_LEVEL_1_SOC_LOW(Doc.of(Level.WARNING) //
			.text("SOC Low Alarm Level 1")), //
	ALARM_LEVEL_1_CELL_CHA_TEMP_LOW(Doc.of(Level.WARNING) //
			.text("Cell Charge Temperature Low Alarm Level 1")), //
	ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH(Doc.of(Level.WARNING) //
			.text("Cell Charge Temperature High Alarm Level 1")), //
	ALARM_LEVEL_1_DISCHA_CURRENT_HIGH(Doc.of(Level.WARNING) //
			.text("Discharge Current High Alarm Level 1")), //
	ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
			.text("Total Voltage Low Alarm Level 1")), //
	ALARM_LEVEL_1_CELL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
			.text("Cell Voltage Low Alarm Level 1")), //
	ALARM_LEVEL_1_CHA_CURRENT_HIGH(Doc.of(Level.WARNING) //
			.text("Charge Current High Alarm Level 1")), //
	ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
			.text("Total Voltage High Alarm Level 1")), //
	ALARM_LEVEL_1_CELL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
			.text("Cell Voltage High Alarm Level 1")), //
	OTHER_ALARM_EQUIPMENT_FAILURE(Doc.of(Level.WARNING)), //
	SLAVE_1_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 1 communication error")), //
	SLAVE_2_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 2 communication error")), //
	SLAVE_3_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 3 communication error")), //
	SLAVE_4_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 4 communication error")), //
	SLAVE_5_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 5 communication error")), //
	SLAVE_6_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 6 communication error")), //
	SLAVE_7_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 7 communication error")), //
	SLAVE_8_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 8 communication error")), //
	SLAVE_9_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 9 communication error")), //
	SLAVE_10_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 10 communication error")), //
	SLAVE_11_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 11 communication error")), //
	SLAVE_12_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 12 communication error")), //
	SLAVE_13_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 13 communication error")), //
	SLAVE_14_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 14 communication error")), //
	SLAVE_15_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 15 communication error")), //
	SLAVE_16_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 16 communication error")), //
	SLAVE_17_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 17 communication error")), //
	SLAVE_18_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 18 communication error")), //
	SLAVE_19_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 19 communication error")), //
	SLAVE_20_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
			.text("Slave 20 communication error")), //
	// TODO What is a real error that causes malfunction that a manual operation is
	// needed?
	FAILURE_INITIALIZATION(Doc.of(Level.FAULT) //
			.text("Initialization failure")), //
	FAILURE_EEPROM(Doc.of(Level.FAULT) //
			.text("EEPROM fault")), //
	FAILURE_INTRANET_COMMUNICATION(Doc.of(Level.FAULT) //
			.text("Intranet communication fault")), //
	FAILURE_TEMP_SAMPLING_LINE(Doc.of(Level.FAULT) //
			.text("Temperature sampling line fault")), //
	FAILURE_BALANCING_MODULE(Doc.of(Level.OK) //
			.text("Balancing module fault")), //
	FAILURE_PCB(Doc.of(Level.FAULT) //
			.text("PCB error")), //
	FAILURE_GR_T(Doc.of(Level.FAULT) //
			.text("GR T error")), //
	FAILURE_TEMP_SENSOR(Doc.of(Level.FAULT) //
			.text("Temperature sensor fault")), //
	FAILURE_TEMP_SAMPLING(Doc.of(Level.FAULT) //
			.text("Temperature sampling fault")), //
	FAILURE_VOLTAGE_SAMPLING(Doc.of(Level.FAULT) //
			.text("Voltage sampling fault")), //
	FAILURE_LTC6803(Doc.of(Level.FAULT) //
			.text("LTC6803 fault")), //
	FAILURE_CONNECTOR_WIRE(Doc.of(Level.FAULT) //
			.text("connector wire fault")), //
	FAILURE_SAMPLING_WIRE(Doc.of(Level.FAULT) //
			.text("sampling wire fault")), //
	PRECHARGE_TAKING_TOO_LONG(Doc.of(Level.FAULT) //
			.text("precharge time was too long"));

	private final Doc doc;

	private SingleRackChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}