package io.openems.edge.battery.soltaro.cluster.versionc;

import java.util.HashMap;
import java.util.Map;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.soltaro.ChannelIdImpl;
import io.openems.edge.battery.soltaro.ChargeIndication;
import io.openems.edge.battery.soltaro.cluster.enums.RackInfo;
import io.openems.edge.battery.soltaro.enums.EmsBaudrate;
import io.openems.edge.battery.soltaro.single.versionc.enums.ClusterRunState;
import io.openems.edge.battery.soltaro.single.versionc.enums.PreChargeControl;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;

/**
 * Helper class that provides channels and channel ids for a multi rack channels
 * and ids are created dynamically depending on system configuration.
 */
public enum SingleRack {
	/*
	 * EnumWriteChannels
	 */
	PRE_CHARGE_CONTROL(Doc.of(PreChargeControl.values()) //
			.accessMode(AccessMode.READ_WRITE)), //
	EMS_BAUDRATE(Doc.of(EmsBaudrate.values()) //
			.accessMode(AccessMode.READ_WRITE)), //
	/*
	 * IntegerWriteChannels
	 */
	EMS_ADDRESS(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.READ_WRITE)), //
	EMS_COMMUNICATION_TIMEOUT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS) //
			.accessMode(AccessMode.READ_WRITE)), //
	SET_SUB_MASTER_ADDRESS(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_WRITE) //
			.text("Starting from 0")),
	SYSTEM_TOTAL_CAPACITY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE_HOURS) //
			.accessMode(AccessMode.READ_WRITE)), //
	VOLTAGE_LOW_PROTECTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT) //
			.accessMode(AccessMode.READ_WRITE) //
			.text("Default: 2650mV")), //
	/*
	 * IntegerReadChannels
	 */
	VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	CURRENT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE)), //
	CHARGE_INDICATION(Doc.of(ChargeIndication.values())), //
	SOC(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT)),
	SOH(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT)), //
	MAX_CELL_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.text("Range: 1 ~ 512")), //
	MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	MIN_CELL_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.text("Range: 1 ~ 512")), //
	MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	MAX_CELL_TEMPERATURE_ID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.text("Range: 1 ~ 512")), //
	MAX_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.text("Range: -400 ~ 1500")), //
	MIN_CELL_TEMPERATURE_ID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.text("Range: 1 ~ 512")), //
	MIN_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS) //
			.text("Range: -400 ~ 1500")), //
	AVERAGE_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	SYSTEM_INSULATION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.KILOOHM)), //
	SYSTEM_MAX_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE)),
	SYSTEM_MAX_DISCHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIAMPERE)),
	POSITIVE_INSULATION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.KILOOHM)),
	NEGATIVE_INSULATION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.KILOOHM)),
	CLUSTER_RUN_STATE(Doc.of(ClusterRunState.values())), //
	AVG_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEZIDEGREE_CELSIUS)), //
	PROJECT_ID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.text("Project Firmware Version")), //
	VERSION_MAJOR(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.text("Project Firmware Version")), //
	VERSION_SUB(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.text("Project Firmware Version")), //
	VERSION_MODIFY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.text("Project Firmware Version")), //

	/*
	 * IntegerWriteChannels for Alarms
	 */
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

	/*
	 * StateChannels
	 */
	// Other Alarm Info
	ALARM_COMMUNICATION_TO_MASTER_BMS(Doc.of(Level.WARNING) //
			.text("Communication Failure to Master BMS")), //
	ALARM_COMMUNICATION_TO_SLAVE_BMS(Doc.of(Level.WARNING) //
			.text("Communication Failure to Slave BMS")), //
	ALARM_COMMUNICATION_SLAVE_BMS_TO_TEMP_SENSORS(Doc.of(Level.WARNING) //
			.text("Communication Failure between Slave BMS and Temperature Sensors")), //
	ALARM_SLAVE_BMS_HARDWARE(Doc.of(Level.WARNING) //
			.text("Slave BMS Hardware Failure")), //
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
	// Alarm Level 1
	LEVEL1_DISCHARGE_TEMP_LOW(Doc.of(Level.WARNING) //
			.text("Discharge Temperature Low Alarm Level 1")), //
	LEVEL1_DISCHARGE_TEMP_HIGH(Doc.of(Level.WARNING) //
			.text("Discharge Temperature High Alarm Level 1")), //
	LEVEL1_TOTAL_VOLTAGE_DIFF_TOO_BIG(Doc.of(Level.WARNING) //
			.text("Total Voltage Difference Too Big Alarm Level 1")), //
	LEVEL1_INSULATION_VALUE(Doc.of(Level.WARNING) //
			.text("Insulation Value Failure Alarm Level 1")), //
	LEVEL1_CELL_VOLTAGE_DIFF_TOO_BIG(Doc.of(Level.WARNING) //
			.text("Cell Voltage Difference Too Big Alarm Level 1")), //
	LEVEL1_POWER_POLE_TEMP_HIGH(Doc.of(Level.WARNING) //
			.text("Power Pole temperature too high Alarm Level 1")), //
	LEVEL1_TEMP_DIFF_TOO_BIG(Doc.of(Level.WARNING) //
			.text("Temperature Difference Too Big Alarm Level 1")), //
	LEVEL1_CHARGE_TEMP_LOW(Doc.of(Level.WARNING) //
			.text("Cell Charge Temperature Low Alarm Level 1")), //
	LEVEL1_SOC_LOW(Doc.of(Level.WARNING) //
			.text("Stage-Of-Charge Low Alarm Level 1")), //
	LEVEL1_CHARGE_TEMP_HIGH(Doc.of(Level.WARNING) //
			.text("Charge Temperature High Alarm Level 1")), //
	LEVEL1_DISCHARGE_CURRENT_HIGH(Doc.of(Level.WARNING) //
			.text("Discharge Current High Alarm Level 1")), //
	LEVEL1_TOTAL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
			.text("Total Voltage Low Alarm Level 1")), //
	LEVEL1_CELL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
			.text("Cell Voltage Low Alarm Level 1")), //
	LEVEL1_CHARGE_CURRENT_HIGH(Doc.of(Level.WARNING) //
			.text("Charge Current High Alarm Level 1")), //
	LEVEL1_TOTAL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
			.text("Total Voltage High Alarm Level 1")), //
	LEVEL1_CELL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
			.text("Cell Voltage High Alarm Level 1")), //
	// Alarm Level 2
	LEVEL2_DISCHARGE_TEMP_LOW(Doc.of(Level.FAULT) //
			.text("Discharge Temperature Low Alarm Level 2")), //
	LEVEL2_DISCHARGE_TEMP_HIGH(Doc.of(Level.FAULT) //
			.text("Discharge Temperature High Alarm Level 2")), //
	LEVEL2_INSULATION_VALUE(Doc.of(Level.FAULT) //
			.text("Insulation Value Failure Alarm Level 2")), //
	LEVEL2_POWER_POLE_TEMP_HIGH(Doc.of(Level.FAULT) //
			.text("Power Pole temperature too high Alarm Level 2")), //
	LEVEL2_CHARGE_TEMP_LOW(Doc.of(Level.FAULT) //
			.text("Cell Charge Temperature Low Alarm Level 2")), //
	LEVEL2_CHARGE_TEMP_HIGH(Doc.of(Level.FAULT) //
			.text("Charge Temperature High Alarm Level 2")), //
	LEVEL2_DISCHARGE_CURRENT_HIGH(Doc.of(Level.FAULT) //
			.text("Discharge Current High Alarm Level 2")), //
	LEVEL2_TOTAL_VOLTAGE_LOW(Doc.of(Level.FAULT) //
			.text("Total Voltage Low Alarm Level 2")), //
	LEVEL2_CELL_VOLTAGE_LOW(Doc.of(Level.FAULT) //
			.text("Cell Voltage Low Alarm Level 2")), //
	LEVEL2_CHARGE_CURRENT_HIGH(Doc.of(Level.FAULT) //
			.text("Charge Current High Alarm Level 2")), //
	LEVEL2_TOTAL_VOLTAGE_HIGH(Doc.of(Level.FAULT) //
			.text("Total Voltage High Alarm Level 2")), //
	LEVEL2_CELL_VOLTAGE_HIGH(Doc.of(Level.FAULT) //
			.text("Cell Voltage High Alarm Level 2")), //
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

//	VOLTAGE(new IntegerDoc() //
//			.unit(Unit.MILLIVOLT)),
//	CURRENT(new IntegerDoc() //
//			.unit(Unit.MILLIAMPERE)),
//	CHARGE_INDICATION(Doc.of(ChargeIndication.values())), //
//	SOC(new IntegerDoc().unit(Unit.PERCENT)), //
//	SOH(new IntegerDoc().unit(Unit.PERCENT)), //
//	MAX_CELL_VOLTAGE_ID(new IntegerDoc() //
//			.unit(Unit.NONE)),
//	MAX_CELL_VOLTAGE(new IntegerDoc() //
//			.unit(Unit.MILLIVOLT)),
//	MIN_CELL_VOLTAGE_ID(new IntegerDoc() //
//			.unit(Unit.NONE)),
//	MIN_CELL_VOLTAGE(new IntegerDoc() //
//			.unit(Unit.MILLIVOLT)),
//	MAX_CELL_TEMPERATURE_ID(new IntegerDoc() //
//			.unit(Unit.NONE)),
//	MAX_CELL_TEMPERATURE(new IntegerDoc() //
//			.unit(Unit.DEZIDEGREE_CELSIUS)),
//	MIN_CELL_TEMPERATURE_ID(new IntegerDoc() //
//			.unit(Unit.NONE)),
//	MIN_CELL_TEMPERATURE(new IntegerDoc() //
//			.unit(Unit.DEZIDEGREE_CELSIUS)),
//	ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW,
//			Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Cell Discharge Temperature Low Alarm Level 2")); // Bit
//																													// 15
//	this.addEntry(map, KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH,
//			Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Cell Discharge Temperature High Alarm Level 2")); // Bit
//																													// 14
//	this.addEntry(map, KEY_ALARM_LEVEL_2_GR_TEMPERATURE_HIGH,
//			Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " GR Temperature High Alarm Level 2")); // Bit
//																										// 10
//	this.addEntry(map, KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW,
//			Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Cell Charge Temperature Low Alarm Level 2")); // Bit
//																												// 7
//	this.addEntry(map, KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH,
//			Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Cell Charge Temperature High Alarm Level 2")); // Bit
//																													// 6
//	this.addEntry(map, KEY_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH,
//			Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Discharge Current High Alarm Level 2")); // Bit
//																											// 5
//	this.addEntry(map, KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW,
//			Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Total Voltage Low Alarm Level 2")); // Bit
//																										// 4
//	this.addEntry(map, KEY_ALARM_LEVEL_2_CELL_VOLTAGE_LOW,
//			Doc.of(Level.FAULT).text("Cluster 1 Cell Voltage Low Alarm Level 2")); // Bit 3
//	this.addEntry(map, KEY_ALARM_LEVEL_2_CHA_CURRENT_HIGH,
//			Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Charge Current High Alarm Level 2")); // Bit
//																										// 2
//	this.addEntry(map, KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH,
//			Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Total Voltage High Alarm Level 2")); // Bit
//																										// 1
//	this.addEntry(map, KEY_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH,
//			Doc.of(Level.FAULT).text("Rack" + this.rackNumber + " Cell Voltage High Alarm Level 2")); // Bit
//																										// 0
//
//	this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW,
//			Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Cell Discharge Temperature Low Alarm Level 1")); // Bit
//																														// 15
//	this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH, Doc.of(Level.WARNING)
//			.text("Rack" + this.rackNumber + " Cell Discharge Temperature High Alarm Level 1")); // Bit 14
//	this.addEntry(map, KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH,
//			Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Total Voltage Diff High Alarm Level 1")); // Bit
//																												// 13
//	this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH,
//			Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Cell Voltage Diff High Alarm Level 1")); // Bit
//																												// 11
//	this.addEntry(map, KEY_ALARM_LEVEL_1_GR_TEMPERATURE_HIGH,
//			Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " GR Temperature High Alarm Level 1")); // Bit
//																											// 10
//	this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH,
//			Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Cell temperature Diff High Alarm Level 1")); // Bit
//																													// 9
//	this.addEntry(map, KEY_ALARM_LEVEL_1_SOC_LOW,
//			Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " SOC Low Alarm Level 1")); // Bit 8
//	this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW,
//			Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Cell Charge Temperature Low Alarm Level 1")); // Bit
//																													// 7
//	this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH,
//			Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Cell Charge Temperature High Alarm Level 1")); // Bit
//																													// 6
//	this.addEntry(map, KEY_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH,
//			Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Discharge Current High Alarm Level 1")); // Bit
//																												// 5
//	this.addEntry(map, KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW,
//			Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Total Voltage Low Alarm Level 1")); // Bit
//																										// 4
//	this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_VOLTAGE_LOW,
//			Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Cell Voltage Low Alarm Level 1")); // Bit
//																										// 3
//	this.addEntry(map, KEY_ALARM_LEVEL_1_CHA_CURRENT_HIGH,
//			Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Charge Current High Alarm Level 1")); // Bit
//																											// 2
//	this.addEntry(map, KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH,
//			Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Total Voltage High Alarm Level 1")); // Bit
//																											// 1
//	this.addEntry(map, KEY_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH,
//			Doc.of(Level.WARNING).text("Rack" + this.rackNumber + " Cell Voltage High Alarm Level 1")); // Bit
//																										// 0
//	this.addEntry(map, KEY_RUN_STATE, Doc.of(Enums.ClusterRunState.values())); //
//
//	this.addEntry(map, KEY_FAILURE_INITIALIZATION, Doc.of(Level.FAULT).text("Initialization failure")); // Bit
//	// 12
//	this.addEntry(map, KEY_FAILURE_EEPROM, Doc.of(Level.FAULT).text("EEPROM fault")); // Bit 11
//	this.addEntry(map, KEY_FAILURE_INTRANET_COMMUNICATION,
//			Doc.of(Level.FAULT).text("Internal communication fault")); // Bit
//																		// 10
//	this.addEntry(map, KEY_FAILURE_TEMPERATURE_SENSOR_CABLE,
//			Doc.of(Level.FAULT).text("Temperature sensor cable fault")); // Bit
//																			// 9
//	this.addEntry(map, KEY_FAILURE_BALANCING_MODULE, Doc.of(Level.OK).text("Balancing module fault")); // Bit 8
//	this.addEntry(map, KEY_FAILURE_TEMPERATURE_PCB, Doc.of(Level.FAULT).text("Temperature PCB error")); // Bit 7
//	this.addEntry(map, KEY_FAILURE_GR_TEMPERATURE, Doc.of(Level.FAULT).text("GR Temperature error")); // Bit 6
//	this.addEntry(map, KEY_FAILURE_TEMP_SENSOR, Doc.of(Level.FAULT).text("Temperature sensor fault")); // Bit 5
//	this.addEntry(map, KEY_FAILURE_TEMP_SAMPLING, Doc.of(Level.FAULT).text("Temperature sampling fault")); // Bit
//																											// 4
//	this.addEntry(map, KEY_FAILURE_VOLTAGE_SAMPLING, Doc.of(Level.FAULT).text("Voltage sampling fault")); // Bit
//																											// 3
//	this.addEntry(map, KEY_FAILURE_LTC6803, Doc.of(Level.FAULT).text("LTC6803 fault")); // Bit 2
//	this.addEntry(map, KEY_FAILURE_CONNECTOR_WIRE, Doc.of(Level.FAULT).text("connector wire fault")); // Bit 1
//	this.addEntry(map, KEY_FAILURE_SAMPLING_WIRE, Doc.of(Level.FAULT).text("sampling wire fault")); // Bit 0
//
//	this.addEntry(map, KEY_SLEEP, Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE));
//	this.addEntry(map, KEY_RESET, Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE));
//
//	// Cell voltages formatted like: "RACK_1_BATTERY_000_VOLTAGE"
//	for (int i = 0; i < this.numberOfSlaves; i++) {
//		for (int j = i * VOLTAGE_SENSORS_PER_MODULE; j < (i + 1) * VOLTAGE_SENSORS_PER_MODULE; j++) {
//			String key = getSingleCellPrefix(j) + "_" + VOLTAGE;
//			this.addEntry(map, key, new IntegerDoc().unit(Unit.MILLIVOLT));
//		}
//	}
//	// Cell temperatures formatted like : "RACK_1_BATTERY_000_TEMPERATURE"
//	for (int i = 0; i < numberOfSlaves; i++) {
//		for (int j = i * TEMPERATURE_SENSORS_PER_MODULE; j < (i + 1) * TEMPERATURE_SENSORS_PER_MODULE; j++) {
//			String key = getSingleCellPrefix(j) + "_" + TEMPERATURE;
//			this.addEntry(map, key, new IntegerDoc().unit(Unit.DEZIDEGREE_CELSIUS));
//		}
//	}
//	

	private final Doc doc;

	private SingleRack(Doc doc) {
		this.doc = doc;
	}

	/**
	 * Creates a Channel-ID String from the enum and returns it.
	 * 
	 * @param rackInfo the {@link RackInfo}
	 * @return the ChannelId as camel-case String
	 */
	protected String toChannelIdString(RackInfo rackInfo) {
		return ChannelId.channelIdUpperToCamel(this.generateChannelId(rackInfo));
	}

	/**
	 * Creates a new Channel-ID from the enum and returns it.
	 * 
	 * @param rackInfo the {@link RackInfo}
	 * @return the ChannelId
	 */
	protected ChannelId toChannelId(RackInfo rackInfo) {
		return new ChannelIdImpl(this.generateChannelId(rackInfo), this.doc);
	}

	/**
	 * Creates a UPPER_CASE ChannelId.
	 * 
	 * @param rackInfo the {@link RackInfo}
	 * @return the ChannelId as upper-case string
	 */
	private String generateChannelId(RackInfo rackInfo) {
		return rackInfo.getChannelIdPrefix() + this.name();
	}

	protected static final String KEY_CHARGE_INDICATION = "CHARGE_INDICATION";
	protected static final String KEY_SOC = "SOC";
	protected static final String KEY_SOH = "SOH";
	public static final String KEY_MAX_CELL_VOLTAGE_ID = "MAX_CELL_VOLTAGE_ID";
	public static final String KEY_MAX_CELL_VOLTAGE = "MAX_CELL_VOLTAGE";
	public static final String KEY_MIN_CELL_VOLTAGE_ID = "MIN_CELL_VOLTAGE_ID";
	public static final String KEY_MIN_CELL_VOLTAGE = "MIN_CELL_VOLTAGE";
	public static final String KEY_ALARM_LEVEL_1_CELL_VOLTAGE_LOW = "ALARM_LEVEL_1_CELL_VOLTAGE_LOW";
	public static final String KEY_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH = "ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH";

	private static final String KEY_MAX_CELL_TEMPERATURE_ID = "MAX_CELL_TEMPERATURE_ID";
	private static final String KEY_MAX_CELL_TEMPERATURE = "MAX_CELL_TEMPERATURE";
	private static final String KEY_MIN_CELL_TEMPERATURE_ID = "MIN_CELL_TEMPERATURE_ID";
	private static final String KEY_MIN_CELL_TEMPERATURE = "MIN_CELL_TEMPERATURE";
	private static final String KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW = "ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW";
	private static final String KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH = "ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH";
	private static final String KEY_ALARM_LEVEL_2_GR_TEMPERATURE_HIGH = "ALARM_LEVEL_2_GR_TEMPERATURE_HIGH";
	private static final String KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW = "ALARM_LEVEL_2_CELL_CHA_TEMP_LOW";
	private static final String KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH = "ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH";
	private static final String KEY_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH = "ALARM_LEVEL_2_DISCHA_CURRENT_HIGH";
	private static final String KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW = "ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW";
	private static final String KEY_ALARM_LEVEL_2_CELL_VOLTAGE_LOW = "ALARM_LEVEL_2_CELL_VOLTAGE_LOW";
	private static final String KEY_ALARM_LEVEL_2_CHA_CURRENT_HIGH = "ALARM_LEVEL_2_CHA_CURRENT_HIGH";
	private static final String KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH = "ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH";
	private static final String KEY_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH = "ALARM_LEVEL_2_CELL_VOLTAGE_HIGH";
	private static final String KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW = "ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW";
	private static final String KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH = "ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH";
	private static final String KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH = "ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH";
	private static final String KEY_ALARM_LEVEL_1_GR_TEMPERATURE_HIGH = "ALARM_LEVEL_1_GR_TEMPERATURE_HIGH";
	private static final String KEY_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH = "ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH";
	private static final String KEY_ALARM_LEVEL_1_SOC_LOW = "ALARM_LEVEL_1_SOC_LOW";
	private static final String KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW = "ALARM_LEVEL_1_CELL_CHA_TEMP_LOW";
	private static final String KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH = "ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH";
	private static final String KEY_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH = "ALARM_LEVEL_1_DISCHA_CURRENT_HIGH";
	private static final String KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW = "ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW";

	private static final String KEY_ALARM_LEVEL_1_CHA_CURRENT_HIGH = "ALARM_LEVEL_1_CHA_CURRENT_HIGH";
	private static final String KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH = "ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH";
	private static final String KEY_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH = "ALARM_LEVEL_1_CELL_VOLTAGE_HIGH";
	private static final String KEY_RUN_STATE = "RUN_STATE";
	private static final String KEY_FAILURE_INITIALIZATION = "FAILURE_INITIALIZATION";
	private static final String KEY_FAILURE_EEPROM = "FAILURE_EEPROM";
	private static final String KEY_FAILURE_INTRANET_COMMUNICATION = "FAILURE_INTRANET_COMMUNICATION";
	private static final String KEY_FAILURE_TEMPERATURE_SENSOR_CABLE = "FAILURE_TEMPERATURE_SENSOR_CABLE";
	private static final String KEY_FAILURE_BALANCING_MODULE = "FAILURE_BALANCING_MODULE";
	private static final String KEY_FAILURE_TEMPERATURE_PCB = "FAILURE_TEMPERATURE_PCB";
	private static final String KEY_FAILURE_GR_TEMPERATURE = "FAILURE_GR_TEMPERATURE";
	private static final String KEY_FAILURE_TEMP_SENSOR = "FAILURE_TEMP_SENSOR";
	private static final String KEY_FAILURE_TEMP_SAMPLING = "FAILURE_TEMP_SAMPLING";
	private static final String KEY_FAILURE_VOLTAGE_SAMPLING = "FAILURE_VOLTAGE_SAMPLING";
	private static final String KEY_FAILURE_LTC6803 = "FAILURE_LTC6803";
	private static final String KEY_FAILURE_CONNECTOR_WIRE = "FAILURE_CONNECTOR_WIRE";
	private static final String KEY_FAILURE_SAMPLING_WIRE = "FAILURE_SAMPLING_WIRE";
	public static final String KEY_RESET = "RESET";
	public static final String KEY_SLEEP = "SLEEP";

	private static final String BATTERY = "BATTERY";
	private static final String TEMPERATURE = "TEMPERATURE";

	public static final int VOLTAGE_SENSORS_PER_MODULE = 12;
	public static final int TEMPERATURE_SENSORS_PER_MODULE = 12;

	private static final String NUMBER_FORMAT = "%03d"; // creates string number with leading zeros
	private static final int VOLTAGE_ADDRESS_OFFSET = 0x800;
	private static final int TEMPERATURE_ADDRESS_OFFSET = 0xC00;

	private int rackNumber;
	private int numberOfSlaves;
	private int addressOffset;
//	private final Map<String, ChannelId> channelIds;
//	private final Map<String, Channel<?>> channelMap;

//	private SingleRack(int racknumber, int numberOfSlaves, int addressOffset) {
//		this.rackNumber = racknumber;
//		this.numberOfSlaves = numberOfSlaves;
//		this.addressOffset = addressOffset;
//		channelIds = createChannelIdMap();
//		channelMap = createChannelMap();
//	}

//	public Collection<Channel<?>> getChannels() {
//		return channelMap.values();
//	}
//
//	public Channel<?> getChannel(String key) {
//		return channelMap.get(key);
//	}
//
//	public int getSoC() {
//		return getIntFromChannel(KEY_SOC, 0);
//	}
//
//	public int getMinimalCellVoltage() {
//		return getIntFromChannel(KEY_MIN_CELL_VOLTAGE, -1);
//	}
//
//	public int getMaximalCellVoltage() {
//		return getIntFromChannel(KEY_MAX_CELL_VOLTAGE, -1);
//	}
//
//	public int getMinimalCellTemperature() {
//		return getIntFromChannel(KEY_MIN_CELL_TEMPERATURE, -1);
//	}
//
//	public int getMaximalCellTemperature() {
//		return getIntFromChannel(KEY_MAX_CELL_TEMPERATURE, -1);
//	}

//	private int getIntFromChannel(String key, int defaultValue) {
//		@SuppressWarnings("unchecked")
//		Optional<Integer> opt = (Optional<Integer>) this.channelMap.get(key).value().asOptional();
//		int value = defaultValue;
//		if (opt.isPresent()) {
//			value = opt.get();
//		}
//		return value;
//	}

//	private Map<String, Channel<?>> createChannelMap() {
//		Map<String, Channel<?>> channels = new HashMap<>();
//
//		channels.put(KEY_VOLTAGE, parent.addChannel(channelIds.get(KEY_VOLTAGE)));
//		channels.put(KEY_CURRENT, parent.addChannel(channelIds.get(KEY_CURRENT)));
//		channels.put(KEY_CHARGE_INDICATION, parent.addChannel(channelIds.get(KEY_CHARGE_INDICATION)));
//		channels.put(KEY_SOC, parent.addChannel(channelIds.get(KEY_SOC)));
//		channels.put(KEY_SOH, parent.addChannel(channelIds.get(KEY_SOH)));
//		channels.put(KEY_MAX_CELL_VOLTAGE_ID, parent.addChannel(channelIds.get(KEY_MAX_CELL_VOLTAGE_ID)));
//		channels.put(KEY_MAX_CELL_VOLTAGE, parent.addChannel(channelIds.get(KEY_MAX_CELL_VOLTAGE)));
//		channels.put(KEY_MIN_CELL_VOLTAGE_ID, parent.addChannel(channelIds.get(KEY_MIN_CELL_VOLTAGE_ID)));
//		channels.put(KEY_MIN_CELL_VOLTAGE, parent.addChannel(channelIds.get(KEY_MIN_CELL_VOLTAGE)));
//		channels.put(KEY_MAX_CELL_TEMPERATURE_ID, parent.addChannel(channelIds.get(KEY_MAX_CELL_TEMPERATURE_ID)));
//		channels.put(KEY_MAX_CELL_TEMPERATURE, parent.addChannel(channelIds.get(KEY_MAX_CELL_TEMPERATURE)));
//		channels.put(KEY_MIN_CELL_TEMPERATURE_ID, parent.addChannel(channelIds.get(KEY_MIN_CELL_TEMPERATURE_ID)));
//		channels.put(KEY_MIN_CELL_TEMPERATURE, parent.addChannel(channelIds.get(KEY_MIN_CELL_TEMPERATURE)));
//
//		channels.put(KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW)));
//		channels.put(KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH)));
//		channels.put(KEY_ALARM_LEVEL_2_GR_TEMPERATURE_HIGH,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_GR_TEMPERATURE_HIGH)));
//		channels.put(KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW)));
//		channels.put(KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH)));
//		channels.put(KEY_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH)));
//		channels.put(KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW)));
//		channels.put(KEY_ALARM_LEVEL_2_CELL_VOLTAGE_LOW,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_CELL_VOLTAGE_LOW)));
//		channels.put(KEY_ALARM_LEVEL_2_CHA_CURRENT_HIGH,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_CHA_CURRENT_HIGH)));
//		channels.put(KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH)));
//		channels.put(KEY_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH)));
//
//		channels.put(KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW)));
//		channels.put(KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH)));
//		channels.put(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH)));
//		channels.put(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH)));
//		channels.put(KEY_ALARM_LEVEL_1_GR_TEMPERATURE_HIGH,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_GR_TEMPERATURE_HIGH)));
//		channels.put(KEY_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH)));
//		channels.put(KEY_ALARM_LEVEL_1_SOC_LOW, parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_SOC_LOW)));
//		channels.put(KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW)));
//		channels.put(KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH)));
//		channels.put(KEY_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH)));
//		channels.put(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW)));
//		channels.put(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_LOW,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_LOW)));
//		channels.put(KEY_ALARM_LEVEL_1_CHA_CURRENT_HIGH,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_CHA_CURRENT_HIGH)));
//		channels.put(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH)));
//		channels.put(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH,
//				parent.addChannel(channelIds.get(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH)));
//
//		channels.put(KEY_RUN_STATE, parent.addChannel(channelIds.get(KEY_RUN_STATE)));
//
//		channels.put(KEY_FAILURE_INITIALIZATION, parent.addChannel(channelIds.get(KEY_FAILURE_INITIALIZATION)));
//		channels.put(KEY_FAILURE_EEPROM, parent.addChannel(channelIds.get(KEY_FAILURE_EEPROM)));
//		channels.put(KEY_FAILURE_INTRANET_COMMUNICATION,
//				parent.addChannel(channelIds.get(KEY_FAILURE_INTRANET_COMMUNICATION)));
//		channels.put(KEY_FAILURE_TEMPERATURE_SENSOR_CABLE,
//				parent.addChannel(channelIds.get(KEY_FAILURE_TEMPERATURE_SENSOR_CABLE)));
//		channels.put(KEY_FAILURE_BALANCING_MODULE, parent.addChannel(channelIds.get(KEY_FAILURE_BALANCING_MODULE)));
//		channels.put(KEY_FAILURE_TEMPERATURE_PCB, parent.addChannel(channelIds.get(KEY_FAILURE_TEMPERATURE_PCB)));
//		channels.put(KEY_FAILURE_GR_TEMPERATURE, parent.addChannel(channelIds.get(KEY_FAILURE_GR_TEMPERATURE)));
//		channels.put(KEY_FAILURE_TEMP_SENSOR, parent.addChannel(channelIds.get(KEY_FAILURE_TEMP_SENSOR)));
//		channels.put(KEY_FAILURE_TEMP_SAMPLING, parent.addChannel(channelIds.get(KEY_FAILURE_TEMP_SAMPLING)));
//		channels.put(KEY_FAILURE_VOLTAGE_SAMPLING, parent.addChannel(channelIds.get(KEY_FAILURE_VOLTAGE_SAMPLING)));
//		channels.put(KEY_FAILURE_LTC6803, parent.addChannel(channelIds.get(KEY_FAILURE_LTC6803)));
//		channels.put(KEY_FAILURE_CONNECTOR_WIRE, parent.addChannel(channelIds.get(KEY_FAILURE_CONNECTOR_WIRE)));
//		channels.put(KEY_FAILURE_SAMPLING_WIRE, parent.addChannel(channelIds.get(KEY_FAILURE_SAMPLING_WIRE)));
//
//		channels.put(KEY_RESET, parent.addChannel(channelIds.get(KEY_RESET)));
//		channels.put(KEY_SLEEP, parent.addChannel(channelIds.get(KEY_SLEEP)));
//
//		// Cell voltages
//		for (int i = 0; i < this.numberOfSlaves; i++) {
//			for (int j = i * VOLTAGE_SENSORS_PER_MODULE; j < (i + 1) * VOLTAGE_SENSORS_PER_MODULE; j++) {
//				String key = this.getSingleCellPrefix(j) + "_" + VOLTAGE;
//				channels.put(key, parent.addChannel(channelIds.get(key)));
//			}
//		}
//
//		// Cell temperatures
//		for (int i = 0; i < this.numberOfSlaves; i++) {
//			for (int j = i * TEMPERATURE_SENSORS_PER_MODULE; j < (i + 1) * TEMPERATURE_SENSORS_PER_MODULE; j++) {
//				String key = this.getSingleCellPrefix(j) + "_" + TEMPERATURE;
//				channels.put(key, parent.addChannel(channelIds.get(key)));
//			}
//		}
//
//		return channels;
//	}

	private Map<String, ChannelId> createChannelIdMap() {
		Map<String, ChannelId> map = new HashMap<String, ChannelId>();

		return map;
	}

//	public Collection<Task> getTasks() {
//		Collection<Task> tasks = new ArrayList<>();
//
//		// Alarm levels
//		tasks.add(new FC3ReadRegistersTask(this.addressOffset + 0x140, Priority.LOW, //
//				parent.map(getBWE(0x140, parent) //
//						.bit(0, channelIds.get(KEY_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH)) //
//						.bit(1, channelIds.get(KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH)) //
//						.bit(2, channelIds.get(KEY_ALARM_LEVEL_2_CHA_CURRENT_HIGH)) //
//						.bit(3, channelIds.get(KEY_ALARM_LEVEL_2_CELL_VOLTAGE_LOW)) //
//						.bit(4, channelIds.get(KEY_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW)) //
//						.bit(5, channelIds.get(KEY_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH)) //
//						.bit(6, channelIds.get(KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH)) //
//						.bit(7, channelIds.get(KEY_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW)) //
//						.bit(10, channelIds.get(KEY_ALARM_LEVEL_2_GR_TEMPERATURE_HIGH)) //
//						.bit(14, channelIds.get(KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH)) //
//						.bit(15, channelIds.get(KEY_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW)) //
//				), //
//				parent.map(getBWE(0x141, parent) //
//						.bit(0, channelIds.get(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH)) //
//						.bit(1, channelIds.get(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH)) //
//						.bit(2, channelIds.get(KEY_ALARM_LEVEL_1_CHA_CURRENT_HIGH)) //
//						.bit(3, channelIds.get(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_LOW)) //
//						.bit(4, channelIds.get(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW)) //
//						.bit(5, channelIds.get(KEY_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH)) //
//						.bit(6, channelIds.get(KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH)) //
//						.bit(7, channelIds.get(KEY_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW)) //
//						.bit(8, channelIds.get(KEY_ALARM_LEVEL_1_SOC_LOW)) //
//						.bit(9, channelIds.get(KEY_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH)) //
//						.bit(10, channelIds.get(KEY_ALARM_LEVEL_1_GR_TEMPERATURE_HIGH)) //
//						.bit(11, channelIds.get(KEY_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH)) //
//						.bit(13, channelIds.get(KEY_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH)) //
//						.bit(14, channelIds.get(KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH)) //
//						.bit(15, channelIds.get(KEY_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW)) //
//				), //
//				parent.map(channelIds.get(KEY_RUN_STATE), getUWE(0x142)) //
//		) //
//		);
//
//		// Error Codes
//		tasks.add(new FC3ReadRegistersTask(this.addressOffset + 0x185, Priority.LOW, //
//				parent.map(getBWE(0x185, parent) //
//						.bit(0, channelIds.get(KEY_FAILURE_SAMPLING_WIRE))//
//						.bit(1, channelIds.get(KEY_FAILURE_CONNECTOR_WIRE))//
//						.bit(2, channelIds.get(KEY_FAILURE_LTC6803))//
//						.bit(3, channelIds.get(KEY_FAILURE_VOLTAGE_SAMPLING))//
//						.bit(4, channelIds.get(KEY_FAILURE_TEMP_SAMPLING))//
//						.bit(5, channelIds.get(KEY_FAILURE_TEMP_SENSOR))//
//						.bit(6, channelIds.get(KEY_FAILURE_GR_TEMPERATURE))//
//						.bit(7, channelIds.get(KEY_FAILURE_TEMPERATURE_PCB))//
//						.bit(8, channelIds.get(KEY_FAILURE_BALANCING_MODULE))//
//						.bit(9, channelIds.get(KEY_FAILURE_TEMPERATURE_SENSOR_CABLE))//
//						.bit(10, channelIds.get(KEY_FAILURE_INTRANET_COMMUNICATION))//
//						.bit(11, channelIds.get(KEY_FAILURE_EEPROM))//
//						.bit(12, channelIds.get(KEY_FAILURE_INITIALIZATION))//
//				) //
//		));
//
//		// Reset and sleep
//		tasks.add(new FC6WriteRegisterTask(this.addressOffset + 0x0004, //
//				parent.map(channelIds.get(KEY_RESET), getUWE(0x0004))));
//		tasks.add(new FC6WriteRegisterTask(this.addressOffset + 0x001D, //
//				parent.map(channelIds.get(KEY_SLEEP), getUWE(0x001D))));
//
//		int MAX_ELEMENTS_PER_TASK = 100;
//
//		// Cell voltages
//		for (int i = 0; i < this.numberOfSlaves; i++) {
//			List<AbstractModbusElement<?>> elements = new ArrayList<>();
//			for (int j = i * VOLTAGE_SENSORS_PER_MODULE; j < (i + 1) * VOLTAGE_SENSORS_PER_MODULE; j++) {
//				String key = getSingleCellPrefix(j) + "_" + VOLTAGE;
//				UnsignedWordElement uwe = getUWE(VOLTAGE_ADDRESS_OFFSET + j);
//				AbstractModbusElement<?> ame = parent.map(channelIds.get(key), uwe);
//				elements.add(ame);
//			}
//
//			// not more than 100 elements per task, because it can cause problems..
//			int taskCount = (elements.size() / MAX_ELEMENTS_PER_TASK) + 1;
//
//			for (int x = 0; x < taskCount; x++) {
//				List<AbstractModbusElement<?>> subElements = elements.subList(x * MAX_ELEMENTS_PER_TASK,
//						Math.min(((x + 1) * MAX_ELEMENTS_PER_TASK), elements.size()));
//				AbstractModbusElement<?>[] taskElements = subElements.toArray(new AbstractModbusElement<?>[0]);
//				tasks.add(new FC3ReadRegistersTask(taskElements[0].getStartAddress(), Priority.LOW, taskElements));
//			}
//
//		}
//
//		// Cell temperatures
//		for (int i = 0; i < this.numberOfSlaves; i++) {
//			List<AbstractModbusElement<?>> elements = new ArrayList<>();
//			for (int j = i * TEMPERATURE_SENSORS_PER_MODULE; j < (i + 1) * TEMPERATURE_SENSORS_PER_MODULE; j++) {
//				String key = getSingleCellPrefix(j) + "_" + TEMPERATURE;
//
//				SignedWordElement swe = getSWE(TEMPERATURE_ADDRESS_OFFSET + j);
//				AbstractModbusElement<?> ame = parent.map(channelIds.get(key), swe);
//				elements.add(ame);
//			}
//
//			// not more than 100 elements per task, because it can cause problems..
//			int taskCount = (elements.size() / MAX_ELEMENTS_PER_TASK) + 1;
//
//			for (int x = 0; x < taskCount; x++) {
//				List<AbstractModbusElement<?>> subElements = elements.subList(x * MAX_ELEMENTS_PER_TASK,
//						Math.min(((x + 1) * MAX_ELEMENTS_PER_TASK), elements.size()));
//				AbstractModbusElement<?>[] taskElements = subElements.toArray(new AbstractModbusElement<?>[0]);
//				tasks.add(new FC3ReadRegistersTask(taskElements[0].getStartAddress(), Priority.LOW, taskElements));
//			}
//		}
//
//		return tasks;
//	}

	public int getRackNumber() {
		return rackNumber;
	}

	public int getAddressOffset() {
		return addressOffset;
	}

	private String getSingleCellPrefix(int num) {
		return BATTERY + "_" + String.format(NUMBER_FORMAT, num);
	}

	private BitsWordElement getBWE(int addressWithoutOffset, AbstractOpenemsModbusComponent component) {
		return new BitsWordElement(this.addressOffset + addressWithoutOffset, component);
	}

	private UnsignedWordElement getUWE(int addressWithoutOffset) {
		return new UnsignedWordElement(this.addressOffset + addressWithoutOffset);
	}

	private SignedWordElement getSWE(int addressWithoutOffset) {
		return new SignedWordElement(this.addressOffset + addressWithoutOffset);
	}
}
