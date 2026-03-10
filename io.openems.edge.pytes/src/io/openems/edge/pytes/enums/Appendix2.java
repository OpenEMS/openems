package io.openems.edge.pytes.enums;

import io.openems.common.types.OptionsEnum;

// ToDo
public enum Appendix2 implements OptionsEnum {
	UNDEFINED(-1, "Undefined"),
	WAITING(0x0000, "Waiting"),
	OPEN_RUN(0x0001, "Open Run"),
	SOFT_RUN(0x0002, "Soft Run"),
	GENERATING(0x0003, "Generating"),
	BYPASS_INVERTING_RUNNING(0x0004, "Bypass Inverting Running"),
	BYPASS_INVERTING_SYNCHRONIZE(0x0005, "Bypass Inverting Synchronize"),
	
	BYPASS_GRID_RUNNING(0x0006, "Bypass Grid Running"),
	NORMAL_RUNNING(0x000F, "Normal"),
	
	GRID_SURGE_WARNING(0xF010, "Grid Surge Warning"),
	FAN_FAULT_WARNING(0xF011, "Fan Fault Warning"),
	FAN_EXTERNAL_FAULT_WARNING(0xF015, "Fan Fault Warning External"),

	GRID_OVERVOLTAGE(0x1010, "Grid Overvoltage"),
	GRID_UNDERVOLTAGE(0x1011, "Grid Undervoltage"),
	GRID_OVERFREQUENCY(0x1012, "Grid Overfrequency"),
	GRID_UNDERFREQUENCY(0x1013, "Grid Underfrequency"),
	GRID_REVERSE_CURRENT(0x1014, "Reverse Current"),
	NO_GRID(0x1015, "No Grid"),
	UNBALANCED_GRID(0x1016, "Unbalanced Grid"),
	GRID_FREQUENCY_FLUCTUATION(0x1017, "Grid Frequency Fluctuation"),
	GRID_OVER_CURRENT(0x1018, "Grid Over Current"),
	GRID_CURRENT_SAMPLING_ERROR(0x1019, "Grid Current Sampling Error"),

	DC_OVER_VOLTAGE(0x1020, "DC Over Voltage"),
	DC_BUS_OVER_VOLTAGE(0x1021, "DC Bus Over Voltage"),
	DC_BUS_UNBALANCE(0x1022, "DC Bus Unbalance"),
	DC_BUS_UNDER_VOLTAGE(0x1023, "DC Bus Under Voltage"),
	DC_BUS_UNBALANCE_2(0x1024, "DC Bus Unbalance 2"),
	DC_CHANNEL_A_OVER_CURRENT(0x1025, "DC Channel A Over Current"),
	DC_CHANNEL_B_OVER_CURRENT(0x1026, "DC Channel B Over Current"),
	DC_INTERFERENCE(0x1027, "DC Interference"),
	DC_REVERSE_CONNECTION(0x1028, "DC Reverse Connection"),
	PV_MIDPOINT_GROUNDING_FAULT(0x1029, "PV Midpoint Grounding Fault"),

	GRID_INTERFERENCE_PROTECTION(0x1030, "Grid Interference Protection"),
	DSP_INITIAL_PROTECTION(0x1031, "DSP Initial Protection"),
	OVER_TEMPERATURE_PROTECTION(0x1032, "Over Temperature Protection"),
	PV_INSULATION_FAULT(0x1033, "PV Insulation Fault"),
	LEAKAGE_CURRENT_PROTECTION(0x1034, "Leakage Current Protection"),
	RELAY_CHECK_PROTECTION(0x1035, "Relay Check Protection"),
	DSP_B_PROTECTION(0x1036, "DSP B Protection"),
	DC_INJECTION_PROTECTION(0x1037, "DC Injection Protection"),
	V12_UNDER_VOLTAGE_FAULT(0x1038, "12V Under Voltage Fault"),
	
	LEAKAGE_CURRENT_CHECK_PROTECTION(0X1039, "Leakage Current Check Protection"),
	UNDER_TEMPERATURE_PROTECTION(0x103A, "Under Temperature Protection"),

	AFCI_CHECK_FAULT(0x1040, "AFCI Check Fault"),
	AFCI_FAULT(0x1041, "AFCI Fault"),
	GRID_INTERFERENCE_02_PROTECTION(0x1046, "Grid Interference 02 Protection"),
	GRID_CURRENT_SAMPLING_ERROR_02(0x1047, "Grid Current Sampling Error 02"),
	IGBT_OVER_CURRENT(0x1048, "IGBT Over Current"),

	GRID_TRANSIENT_OVERCURRENT(0x1050, "Grid Transient Overcurrent"),
	BATTERY_HARDWARE_OVERVOLTAGE_FAULT(0x1051, "Battery Hardware Overvoltage Fault"),
	LLC_HARDWARE_OVERCURRENT(0x1052, "LLC Hardware Overcurrent"),
	BATTERY_OVERVOLTAGE(0x1053, "Battery Overvoltage"),
	BATTERY_UNDERVOLTAGE(0x1054, "Battery Undervoltage"),
	BATTERY_NOT_CONNECTED(0x1055, "Battery Not Connected"),
	BACKUP_OVERVOLTAGE(0x1056, "Backup Overvoltage"),
	BACKUP_OVERLOAD(0x1057, "Backup Overload"),
	DSP_SELFCHECK_ERROR(0x1058, "DSP Selfcheck Error"),
	DSP_DETECTED_BATTERY_OVERCURRENT(0x105B, "DSP Detected Battery Overcurrent"),

	SLAVE_SYNC_SIGNAL_LOSS(0x1060, "Slave Sync Signal Loss"),
	MASTER_SYNC_SIGNAL_LOSS(0x1061, "Master Sync Signal Loss"),
	SLAVE_SYNC_SIGNAL_PERIOD_ERROR(0x1062, "Slave Sync Signal Period Error"),
	MASTER_SYNC_SIGNAL_PERIOD_ERROR(0x1063, "Master Sync Signal Period Error"),
	PHYSICAL_ADDRESS_CONFLICT(0x1064, "Physical Address Conflict"),
	HEARTBEAT_LOSS(0x1065, "Heartbeat Loss"),
	DCAN_REGISTER_ERROR(0x1066, "DCAN Register Error"),
	MULTIPLE_MASTER_ERROR(0x1067, "Multiple Master Error"),
	MASTER_SLAVE_MODE_CONFLICT(0x1068, "Master Slave On-grid Off-grid Mode Conflict"),
	MASTER_OFFGRID_SLAVE_CONNECT_VOLTAGE_CONFLICT(0x1069, "Master Off-grid Slave Connect Voltage Conflict"),
	OTHER_DEVICE_FAULT_FLAG(0x106A, "Other Device Fault Flag"),

	BATTERY_HARDWARE_OVERVOLTAGE_02(0x1070, "Battery Hardware Overvoltage 02"),
	BATTERY_HARDWARE_OVERCURRENT(0x1071, "Battery Hardware Overcurrent"),
	OFFGRID_BACKUP_UNDERVOLTAGE(0x1072, "Off Grid Backup Undervoltage"),
	BUS_MIDPOINT_HARDWARE_OVERCURRENT(0x1073, "Bus Midpoint Hardware Overcurrent"),
	BATTERY_STARTUP_FAIL(0x1074, "Battery Startup Fail"),
	DC3_AVERAGE_OVERCURRENT(0x1075, "DC 3 Average Overcurrent"),
	DC4_AVERAGE_OVERCURRENT(0x1076, "DC 4 Average Overcurrent"),
	SOFTRUN_TIMEOUT(0x1077, "Softrun Timeout"),
	OFFGRID_TO_GRID_TIMEOUT(0x1078, "Off-grid To Grid Timeout"),

	FAIL_SAFE(0x2010, "Fail Safe"),
	METER_COM_FAIL(0x2011, "Meter COM Fail"),
	BATTERY_COM_FAIL(0x2012, "Battery COM Fail"),
	DSP_COM_FAIL(0x2014, "DSP COM Fail"),
	BMS_ALARM(0x2015, "BMS Alarm"),
	BATTERY_SELECTION_NOT_THE_SAME(0x2016, "Battery Selection Not The Same"),
	ALARM2_BMS(0x2017, "Alarm2 BMS"),
	DRM_CONNECT_FAIL(0x2018, "DRM Connect Fail"),
	METER_SELECT_FAIL(0x2019, "Meter Select Fail"),

	LEAD_ACID_BATTERY_HIGH_TEMPERATURE(0x2020, "Lead-acid Battery High Temperature"),
	LEAD_ACID_BATTERY_LOW_TEMPERATURE(0x2021, "Lead-acid Battery Low Temperature"),

	GRID_BACKUP_OVERLOAD(0x2030, "Grid Backup Overload"),

	EPM_HARD_LIMIT_PROTECTION(0x2040, "EPM Hard Limit Protection"),
	AFCI_COMM_FAIL(0x2041, "AFCI Comm Fail"),
	AFCI_CT_MODULE_FAIL(0x2042, "AFCI CT Module Fail"),
	G100_OVI_PROTECTION(0x2043, "G100 OVI Protection"),
	MULTIPLE_MASTER_SET_ERROR(0x2044, "Multiple Master Set Error");
	

	private final int value;
	private final String name;

	Appendix2(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override public int getValue() { return this.value; }
	@Override public String getName() { return this.name; }
	@Override public OptionsEnum getUndefined() { return UNDEFINED; }
}
