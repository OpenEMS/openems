package io.openems.edge.pytes.enums;

import io.openems.common.types.OptionsEnum;

public enum InverterStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"),

	// ---- Operating / running states (0x0000 ..) ----
	NORMAL_OPERATION_WAITING(0x0000, "Normal operation waiting"),
	OPEN_RUN(0x0001, "OpenRun"),
	WAITING_SOFT_RUN(0x0002, "Waiting SoftRun"),
	INITIALIZING(0x0003, "Initializing"),
	BYPASS_INVERTING_RUNNING(0x0004, "Bypass inverting running"),
	BYPASS_INVERTING_SYNCHRONIZE(0x0005, "Bypass inverting synchronize"),
	BYPASS_GRID_RUNNING(0x0006, "Bypass grid running / GridToLoad"),
	NORMAL_RUNNING(0x000F, "Normal running / Normal"),

	GRID_OFF(0x1004, "Grid Off"),

	// ---- Warnings (0xF0xx) ----
	GRID_SURGE_WARN(0xF010, "Grid surge (Warn) / Surge Alarm"),
	FAN_FAULT_WARN(0xF011, "Fan fault (Warn) / Fan Alarm"),
	FAN_FAULT_WARN_EXTERNAL(0xF015, "Fan fault (Warn External) / Fan_H Alarm"),

	// ---- Grid faults (0x101x) ----
	GRID_OVERVOLTAGE(0x1010, "Grid overvoltage (OV-G-V)"),
	GRID_UNDERVOLTAGE(0x1011, "Grid undervoltage (UN-G-V)"),
	GRID_OVERFREQ(0x1012, "Grid overfreq (OV-G-F)"),
	GRID_UNDERFREQ(0x1013, "Grid underfreq (UN-G-F)"),
	REVERSE_CURRENT(0x1014, "Reverse current (Reve-Grid)"),
	NO_GRID(0x1015, "No-grid (NO-Grid)"),
	UNBALANCED_GRID(0x1016, "Unbalanced grid (G-PHASE)"),
	GRID_FREQUENCY_FLUCTUATION(0x1017, "Grid Frequency Fluctuation (G-F-FLU)"),
	GRID_OVER_CURRENT(0x1018, "Grid Over Current (OV-G-I)"),
	GRID_CURRENT_SAMPLING_ERROR(0x1019, "Grid current sampling error (IGFOL-F)"),

	// ---- DC faults (0x102x) ----
	DC_OVER_VOLTAGE(0x1020, "DC Over Voltage (OV-DC)"),
	DC_BUS_OVER_VOLTAGE(0x1021, "DC Bus Over Voltage (OV-BUS)"),
	DC_BUS_UNBALANCE(0x1022, "DC Bus Unbalance (UNB-BUS)"),
	DC_BUS_UNDER_VOLTAGE(0x1023, "DC Bus Under Voltage (UN-BUS)"),
	DC_BUS_UNBALANCE_2(0x1024, "DC Bus Unbalance 2 (UNB2-BUS)"),
	DC_CHANNEL_A_OVER_CURRENT(0x1025, "DC(Channel A) Over Current (OV-DCA-I)"),
	DC_CHANNEL_B_OVER_CURRENT(0x1026, "DC(Channel B) Over Current (OV-DCB-I)"),
	DC_INTERFERENCE(0x1027, "DC interference (DC-INTF)"),
	DC_REVERSE_CONNECTION(0x1028, "DC reverse connection (Reve-DC)"),
	PV_MIDPOINT_GROUNDING_FAULT(0x1029, "PV midpoint grounding fault (PvMidIso)"),

	// ---- Protections (0x103x) ----
	GRID_INTERFERENCE_PROTECTION(0x1030, "Grid Interference Protection (OC-AC-TRANS)"),
	DSP_INITIAL_PROTECTION(0x1031, "DSP Initial Protection (INI-FAULT)"),
	OVER_TEMPERATURE_PROTECTION(0x1032, "Over temperature protection (OV-TEM)"),
	PV_INSULATION_FAULT(0x1033, "PV insulation fault (PV ISO-PRO)"),
	LEAKAGE_CURRENT_PROTECTION(0x1034, "Leakage current Protection (ILeak-PRO)"),
	LEAKAGE_CURRENT_PROTECTION_01(0x1034, "Leakage current Protection 01 (ILeak-PRO01)"), // Subcode lives in 33292
	RELAY_CHECK_PROTECTION(0x1035, "Relay Check Protection (RelayChk-FAIL)"),
	DSP_B_PROTECTION(0x1036, "DSP_B Protection (DSP-B-FAULT)"),
	DC_INJECTION_PROTECTION(0x1037, "DC Injection Protection (DCInj-FAULT)"),
	UNDER_12V_FAULT(0x1038, "12V Under Voltage Faulty (12Power-FAULT)"),
	LEAKAGE_CURRENT_CHECK_PROTECTION(0x1039, "Leakage Current Check Protection (ILeak-Check)"),
	UNDER_TEMPERATURE_PROTECTION(0x103A, "Under temperature protection (UN-TEM)"),

	// ---- AFCI / extra grid faults ----
	AFCI_CHECK_FAULT(0x1040, "AFCI Check Fault (AFCI-Check)"),
	AFCI_FAULT(0x1041, "AFCI Fault (ARC-FAULT)"),
	GRID_INTERFERENCE_02_PROTECTION(0x1046, "Grid Interference 02 Protection (GRID-INTF02)"),
	GRID_CURRENT_SAMPLING_ERROR_IG_AD(0x1047, "Grid Current Sampling Error (IG-AD)"),
	IGBT_OVER_CURRENT(0x1048, "IGBT Over Current (IGBT-OV-I)"),

	// ---- Hardware / battery / backup faults (0x105x) ----
	GRID_TRANSIENT_OVERCURRENT(0x1050, "Grid transient overcurrent (OV-IgTr)"),
	BATTERY_HW_OVERVOLTAGE_FAULT(0x1051, "Battery hardware overvoltage fault (OV-Vbatt-H)"),
	LLC_HW_OVERCURRENT(0x1052, "LLC hardware overcurrent (OV-ILLC)"),
	BATTERY_OVERVOLTAGE(0x1053, "Battery overvoltage (OV-Vbatt)"),
	BATTERY_UNDERVOLTAGE(0x1054, "Battery undervoltage (UN-Vbatt)"),
	BATTERY_NOT_CONNECTED(0x1055, "Battery not connected (NO-Battery)"),
	BACKUP_OVERVOLTAGE(0x1056, "Backup overvoltage (OV-VBackup)"),
	BACKUP_OVERLOAD(0x1057, "Backup overload (Over-Load)"),
	DSP_SELFCHECK_ERROR(0x1058, "DSP Selfcheck error (DspSelfChk)"),
	DSP_DETECTED_BATTERY_OVERCURRENT(0x105B, "DSP Detected Battery Overcurrent (BAT-DOC)"),

	// ---- Parallel / sync faults (0x106x) ----
	SLAVE_SYNC_SIGNAL_LOSS(0x1060, "Slave Sync Signal Loss (SlaveLoseErr)"),
	MASTER_SYNC_SIGNAL_LOSS(0x1061, "Master Sync Signal Loss (MasterLoseErr)"),
	SLAVE_SYNC_SIGNAL_PERIOD_ERROR(0x1062, "Slave Sync Signal Period Error (SlavePrd-Err)"),
	MASTER_SYNC_SIGNAL_PERIOD_ERROR(0x1063, "Master Sync Signal Period Error (MasterPrd-Err)"),
	PHYSICAL_ADDRESS_CONFLICT(0x1064, "Physical Address Conflict (Addr-Conflict)"),
	HEARTBEAT_LOSS(0x1065, "Heartbeat Loss (HeartbeatLose)"),
	DCAN_REGISTER_ERROR(0x1066, "DCAN Register Error (DCanErr)"),
	MULTIPLE_MASTER_ERROR(0x1067, "Multiple Master Error (MulMasterErr)"),
	ON_GRID_OFF_GRID_MODE_CONFLICT(0x1068, "Master/Slave On-grid Off-grid Mode Conflict (ModeConflict)"),
	OFFGRID_SLAVE_CONNECT_VOLTAGE_CONFLICT(0x1069, "Master Off-grid Slave Connect Voltage Conflict (S-PlugVoltErr)"),
	OTHER_DEVICE_FAULT_FLAG(0x106A, "Other Device Fault Flag (Others'Fault)"),

	// ---- More battery/bus faults (0x107x) ----
	BATTERY_HW_OVERVOLTAGE_02(0x1070, "Battery hardware overvoltage 02 (OV-Vbatt2-H)"),
	BATTERY_HW_OVERCURRENT(0x1071, "Battery hardware overcurrent (OV-Ibatt-H)"),
	OFFGRID_BACKUP_UNDERVOLTAGE(0x1072, "Off grid Backup undervoltage (UN-VBackup)"),
	BUS_MIDPOINT_HW_OVERCURRENT(0x1073, "Bus midpoint hardware overcurrent (OV-Ibusmp-H)"),
	BATTERY_STARTUP_FAIL(0x1074, "Battery startup fail (Batt-ON-Fail)"),
	DC3_AVERAGE_OVERCURRENT(0x1075, "DC 3 average overcurrent (OV-DC03-I)"),
	DC4_AVERAGE_OVERCURRENT(0x1076, "DC 4 average overcurrent (OV-DC04-I)"),
	SOFTRUN_TIMEOUT(0x1077, "Softrun timeout (Softrun-Tout)"),
	OFFGRID_TO_GRID_TIMEOUT(0x1078, "Off-grid to Grid Time out (GridConn-Tout)"),

	// ---- COM / selection / misc alarms (0x201x) ----
	FAIL_SAFE(0x2010, "Fail Safe"),
	METER_COM_FAIL(0x2011, "Meter COM fail (MET_Comm_FAIL)"),
	BATTERY_COM_FAIL(0x2012, "Battery COM fail (CAN_Comm_FAIL)"),
	DSP_COM_FAIL(0x2014, "DSP COM fail (DSP_Comm_FAIL)"),
	BMS_ALARM(0x2015, "BMS Alarm (Alarm-BMS)"),
	BATTERY_SELECTION_NOT_SAME(0x2016, "Battery selection not the same (BatName-FAIL)"),
	ALARM2_BMS(0x2017, "Alarm2-BMS"),
	DRM_CONNECT_FAIL(0x2018, "DRM Connect Fail (DRM_LINK_FAIL)"),
	METER_SELECT_FAIL(0x2019, "Meter select fail (MET_SEL_FAIL)"),

	LEAD_ACID_BATTERY_HIGH_TEMPERATURE(0x2020, "Lead-acid battery High temperature (HighTemp.AMB)"),
	LEAD_ACID_BATTERY_LOW_TEMPERATURE(0x2021, "Lead-acid battery Low temperature (LowTemp.AMB)"),

	GRID_BACKUP_OVERLOAD(0x2030, "Grid backup overload (BKAC Overload)"),

	EPM_HARD_LIMIT_PROTECTION(0x2040, "EPM Hard Limit Protection (EPM-HardLimit)"),
	AFCI_COMM_FAIL(0x2041, "AFCI-Comm-Fail"),
	AFCI_CT_MODULE_FAIL(0x2042, "AFCI-CTModule-Fail"),
	G100_OVI_PRO(0x2043, "G100_OVI_PRO"),
	MULMASTER_SET_ERR(0x2044, "MulMaster-Set-Err");

	private final int value;
	private final String name;

	InverterStatus(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override public int getValue() { return this.value; }
	@Override public String getName() { return this.name; }
	@Override public OptionsEnum getUndefined() { return UNDEFINED; }
}
