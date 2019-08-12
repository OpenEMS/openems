package io.openems.edge.ess.goodwe;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.sum.GridMode;

public enum GoodweChannelId implements ChannelId {
	LOWEST_FEEDING_VOLTAGE_PV(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
	RECONNECT_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)),
	HIGH_LIMIT_GRID_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
	LOW_LIMIT_GRID_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
	LOW_LIMIT_GRID_FREQUENCY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)),
	HIGH_LIMIT_GRID_FREQUENCY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)),
	RTC_YEAR_MONTH(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MONTH_YEAR).accessMode(AccessMode.READ_WRITE)),
	RTC_DATE_HOUR(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HOUR_DATE).accessMode(AccessMode.READ_WRITE)),
	RTC_MINUTE_SECOND(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MINUTE_SECOND).accessMode(AccessMode.READ_WRITE)),
	RANGE_REAL_POWER_ADJUST(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT).accessMode(AccessMode.WRITE_ONLY)),
	RANGE_REACTIVE_POWER_ADJUST(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT).accessMode(AccessMode.WRITE_ONLY)), // need to confirm

	SERIAL_NUMBER_INVERTER(Doc.of(OpenemsType.STRING) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), // ascii unit?
	NOM_VPV(Doc.of(OpenemsType.STRING) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	FIRMWARE_VERSION(Doc.of(OpenemsType.STRING) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	MODEL_NAME_INVERTER(Doc.of(OpenemsType.STRING) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),
	DSP_INTERNAL_FIRMWARE_VERSION(Doc.of(OpenemsType.STRING) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	ARM_INTERNAL_FIRMWARE_VERSION(Doc.of(OpenemsType.STRING) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	MANUFACTURE_INFO(Doc.of(OpenemsType.STRING) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),

	FIRMWARE_VERSION_HEXA(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	ARM_UPDATA_RESULT(Doc.of(UpdateResult.values())), //
	DSP_UPDATA_RESULT(Doc.of(UpdateResult.values())), //

	// Hybrid Inverter
	VPV1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	IPV1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),

	PV1_MODE(Doc.of(WorkModePV.values())), //
	VPV2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	IPV2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
	PV2_MODE(Doc.of(WorkModePV.values())), //
	VBATTERY1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	TBD1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), // TBD1
	BMS_STATUS(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	BMS_PACK_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
	IBATTERY1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
	BMS_CHARGE_IMAX(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
	BMS_DISCHARGE_IMAX(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
	// BMS_ERROR_CODE(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),

	// BMS error codes
	STATE_0(Doc.of(Level.WARNING).text("Battery Over Temperature")), //
	STATE_1(Doc.of(Level.WARNING).text("Battery Under Temperature")), //
	STATE_2(Doc.of(Level.WARNING).text("Battery Cell Voltage Differences")), //
	STATE_3(Doc.of(Level.WARNING).text("Battery Over Total Voltage")), //
	STATE_4(Doc.of(Level.WARNING).text("Battery Discharge Over Current")), //
	STATE_5(Doc.of(Level.WARNING).text("Battery Charge Over Current")), //
	STATE_6(Doc.of(Level.WARNING).text("Battery Under SOC")), //
	STATE_7(Doc.of(Level.WARNING).text("Battery Under Total Voltage")), //
	STATE_8(Doc.of(Level.WARNING).text("Battery Communication Fail")), //
	STATE_9(Doc.of(Level.WARNING).text("Battery Output Short")), //
	STATE_10(Doc.of(Level.WARNING).text("BMS SOC TooHigh")), //
	STATE_11(Doc.of(Level.FAULT).text("BMS Module Fault")), //
	STATE_12(Doc.of(Level.FAULT).text("BMS System Fault")), //
	STATE_13(Doc.of(Level.FAULT).text("BMS Internal Fault")), //

	SOC(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),
	INVERTER_WARNING_CODE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	TBD2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), // TBD2
	BMS_SOH(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),
	BATTERY_MODE(Doc.of(BatteryWorkMode.values())), //
	BMS_WARNING_CODE_H(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	BMS_WARNING_CODE_L(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),

	METER_STATUS(Doc.of(MeterStatus.values())), //
	VGRID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	IGRID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
	PGRID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
	FGRID(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)),
	GRID_MODE(Doc.of(GridMode.values())), //
	VLOAD(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	ILOAD(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
	ONGRID_LOAD_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
	FLOAD(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)),
	LOAD_MODE(Doc.of(LoadMode.values())), //

	INVERTER_WORK_MODE(Doc.of(InverterWorkMode.values())), //
	TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
	ERROR_MESSAGE_H(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	ERROR_MESSAGE_L(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),

	// Failure description for status ‘failure’
	STATE_14(Doc.of(Level.FAULT).text("The GFCI detecting circuit is abnormal")), //
	STATE_15(Doc.of(Level.FAULT).text("The output current sensor is abnormal")), //
	STATE_16(Doc.of(Level.WARNING).text("TBD")), //
	STATE_17(Doc.of(Level.FAULT).text("DCI Consistency Failure")), //
	STATE_18(Doc.of(Level.FAULT).text("GFCI Consistency Failure")), //
	STATE_19(Doc.of(Level.WARNING).text("TBD")), //
	STATE_20(Doc.of(Level.FAULT).text("GFCI Device Failure")), //
	STATE_21(Doc.of(Level.FAULT).text("Relay Device Failure")), //
	STATE_22(Doc.of(Level.FAULT).text("AC HCT Failure")), //
	STATE_23(Doc.of(Level.FAULT).text("Utility Loss")), //
	STATE_24(Doc.of(Level.FAULT).text("Gournd I Failure")), //
	STATE_25(Doc.of(Level.WARNING).text("DC Bus High")), //
	STATE_26(Doc.of(Level.FAULT).text("Internal Fan Failure(Back-Up Over Load for ES)")), //
	STATE_27(Doc.of(Level.WARNING).text("Over Temperature")), //
	STATE_28(Doc.of(Level.FAULT).text("Auto Test Failure")), //
	STATE_29(Doc.of(Level.WARNING).text("PV Over Voltage")), //
	STATE_30(Doc.of(Level.FAULT).text("External Fan Failure")), //
	STATE_31(Doc.of(Level.FAULT).text("Vac Failure")), //
	STATE_32(Doc.of(Level.FAULT).text("Isolation Failure")), //
	STATE_33(Doc.of(Level.WARNING).text("DC Injection High")), //
	STATE_34(Doc.of(Level.WARNING).text("TBD")), //
	STATE_35(Doc.of(Level.WARNING).text("TBD")), //
	STATE_36(Doc.of(Level.FAULT).text("Fac Consistency Failure")), //
	STATE_37(Doc.of(Level.FAULT).text("Vac Consistency Failure")), //
	STATE_38(Doc.of(Level.WARNING).text("TBD")), //
	STATE_39(Doc.of(Level.WARNING).text("Relay Check Failure")), //
	STATE_40(Doc.of(Level.WARNING).text("TBD")), //
	STATE_41(Doc.of(Level.WARNING).text("TBD")), //
	STATE_42(Doc.of(Level.WARNING).text("TBD")), //
	STATE_43(Doc.of(Level.FAULT).text("Fac Failure")), //
	STATE_44(Doc.of(Level.FAULT).text("EEPROM R/W Failure")), //
	STATE_45(Doc.of(Level.FAULT).text("Internal Communication Failure")), //

	E_TOTAL_H(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
	E_TOTAL_L(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
	H_TOTAL_H(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HOUR).accessMode(AccessMode.READ_ONLY)),
	H_TOTAL_L(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HOUR).accessMode(AccessMode.READ_ONLY)),
	E_DAY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
	E_LOAD_DAY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
	E_TOTAL_LOAD_H(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
	E_TOTAL_LOAD_L(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
	TOTAL_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
	E_PV_TOTAL_H(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
	E_PV_TOTAL_L(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
	GRID_IN_OUT_FLAG(Doc.of(GridInOutFlag.values())), //
	BACK_UP_LOAD_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
	METER_POWER_FACTOR(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),
	DIAG_STATUS_H(Doc.of(OpenemsType.INTEGER) // ?
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	DIAG_STATUS_L(Doc.of(OpenemsType.INTEGER) // ?
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),

	// DIAG_STATUS
	STATE_46(Doc.of(Level.WARNING).text("Battery Voltage Low")), //
	STATE_47(Doc.of(Level.WARNING).text("Battery SOC Low")), //
	STATE_48(Doc.of(Level.WARNING).text("Battery SOC In Back")), //
	STATE_49(Doc.of(Level.WARNING).text("BMS Discharge Disable")), //
	STATE_50(Doc.of(Level.WARNING).text("Discharge Time On")), //
	STATE_51(Doc.of(Level.WARNING).text("Charge Time On")), //
	STATE_52(Doc.of(Level.WARNING).text("Discharge Drive On")), //
	STATE_53(Doc.of(Level.WARNING).text("BMS Discharge Current Low")), //
	STATE_54(Doc.of(Level.WARNING).text("Discharge Current Low")), //
	STATE_55(Doc.of(Level.WARNING).text("Meter Communication Loss")), //
	STATE_56(Doc.of(Level.WARNING).text("Meter Connect Reverse")), //
	STATE_57(Doc.of(Level.WARNING).text("Self Use Load Light")), //
	STATE_58(Doc.of(Level.WARNING).text("EMS Discharge Is Zero")), //
	STATE_59(Doc.of(Level.WARNING).text("Discharge BUS High")), //
	STATE_60(Doc.of(Level.WARNING).text("Battery Disconnect")), //
	STATE_61(Doc.of(Level.WARNING).text("Battery Overcharge")), //
	STATE_62(Doc.of(Level.WARNING).text("BMS Over Temperature")), //
	STATE_63(Doc.of(Level.WARNING).text("BMS Overcharge")), //
	STATE_64(Doc.of(Level.WARNING).text("BMS Charge Disable")), //
	STATE_65(Doc.of(Level.WARNING).text("Self Use Off")), //
	STATE_66(Doc.of(Level.WARNING).text("SOC Delta OverRange")), //
	STATE_67(Doc.of(Level.WARNING).text("Battery Self Discharge")), //
	STATE_68(Doc.of(Level.WARNING).text("Offgrid SOC Low")), //
	STATE_69(Doc.of(Level.WARNING).text("Grid Wave Unstable")), //
	STATE_70(Doc.of(Level.WARNING).text("Feed Power Limit")), //
	STATE_71(Doc.of(Level.WARNING).text("PF Value Set")), //
	STATE_72(Doc.of(Level.WARNING).text("Real Power Limit")), //
	STATE_73(Doc.of(Level.WARNING).text("DC Output On")), //
	STATE_74(Doc.of(Level.WARNING).text("SOC Protect Off")), //
	STATE_75(Doc.of(Level.WARNING).text("Discharge mode for BP")), //

	DRM_STATUS(Doc.of(OpenemsType.INTEGER) // ?
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),

	STATE_76(Doc.of(Level.WARNING).text("DRM0")), //
	STATE_77(Doc.of(Level.WARNING).text("DRM1")), //
	STATE_78(Doc.of(Level.WARNING).text("DRM2")), //
	STATE_79(Doc.of(Level.WARNING).text("DRM3")), //
	STATE_80(Doc.of(Level.WARNING).text("DRM4")), //
	STATE_81(Doc.of(Level.WARNING).text("DRM5")), //
	STATE_82(Doc.of(Level.WARNING).text("DRM6")), //
	STATE_83(Doc.of(Level.WARNING).text("DRM7")), //
	STATE_84(Doc.of(Level.WARNING).text("DRM8")), //
	STATE_85(Doc.of(Level.WARNING).text("DRED Connect Status")), //

	E_TOTAL_SELL_H(Doc.of(OpenemsType.FLOAT) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	E_TOTAL_SELL_L(Doc.of(OpenemsType.FLOAT) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	E_TOTAL_BUY_H(Doc.of(OpenemsType.FLOAT) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	E_TOTAL_BUY_L(Doc.of(OpenemsType.FLOAT) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	VPV3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	IPV3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
	PV3_MODE(Doc.of(WorkModePV.values())), //
	VGRID_U0(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	IGRID_U0(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
	VGRID_W0(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	IGRID_W0(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
	E_BATTERY_CHARGE_H(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
	E_BATTERY_CHARGE_L(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
	E_BATTERY_DISCHARGE_H(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
	E_BATTERY_DISCHARGE_L(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
	PPV1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
	PPV2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
	PPV3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
	BATTERY_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
	INT_E_TOTAL_SELL_H(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
	INT_E_TOTAL_SELL_L(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),

	INT_E_TOTAL_BUY_H(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
	INT_E_TOTAL_BUY_L(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
	E_BATTERY_CHARGE_TODAY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
	E_BATTERY_DISCHARGE_TODAY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),

	// Setting register
	CHARGE_TIME_START(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), // HM - Hour Minute
	CHARGE_TIME_END(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), // HM - Hour Minute
	BATTERY_CHARGE_POWER_MAX(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),
	DISCHARGER_TIME_START(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), // HM - Hour Minute
	DISCHARGER_TIME_END(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), // HM - Hour Minute
	BATTERY_DISCHARGE_POWER_SET(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),
	BACKUP_ENABLE(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), // HM - Hour
																									// Minute
	OFF_GRID_AUTO_CHARGE(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), // HM - Hour
																										// Minute

	ENABLE_MPPT4_SHADOW(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),
	FEED_POWER_ENABLE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),
	MANUFACTURE_CODE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	LEAD_BATTERY_CAPACITY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE_HOURS).accessMode(AccessMode.READ_WRITE)),
	BATTERY_CHARGE_VOLT_MAX(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
	BATTERY_CHARGE_CURRENT_MAX(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)),
	BATTERY_DISCHARGE_CURRENT_MAX(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)),
	BATTERY_VOLT_UNDER_MINIMUM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
	BATTERY_SOC_UNDER_MINIMUM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
	BATTERY_ACTIVE_PERIOD(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MINUTE).accessMode(AccessMode.READ_WRITE)),
	RP_CONTROL_PARA(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),
	BATTERY_FLOAT_VOLT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
	BATTERY_FLOAT_CURRENT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)),
	BATTERY_TO_FLOAT_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MINUTE).accessMode(AccessMode.READ_WRITE)),
	BATTERY_TYPE_INDEX(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),
	FEED_POWER_PARA(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)),
	AUTO_START_BACKUP(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),
	STOP_SOC_PROTECT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),
	DC_VOLT_OUTPUT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), // /KeepBattVoltOn

	BATTERY_AVERAGE_CHARGE_VOLT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
	BATTERY_AVERAGE_CHARGE_HOURS(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HOUR).accessMode(AccessMode.READ_WRITE)),
	AS477_PARAMETERS(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),

	STATE_86(Doc.of(Level.WARNING).text("QU Curve")), //
	STATE_87(Doc.of(Level.WARNING).text("PU Curve")), //
	STATE_88(Doc.of(Level.WARNING).text("PFreq Curve")), //
	STATE_89(Doc.of(Level.WARNING).text("TBD")), //
	STATE_90(Doc.of(Level.WARNING).text("TBD")), //
	STATE_91(Doc.of(Level.WARNING).text("TBD")), //
	STATE_92(Doc.of(Level.WARNING).text("TBD")), //
	STATE_93(Doc.of(Level.WARNING).text("TBD")), //
	STATE_94(Doc.of(Level.WARNING).text("TBD")), //
	STATE_95(Doc.of(Level.WARNING).text("TBD")), //
	STATE_96(Doc.of(Level.WARNING).text("TBD")), //
	STATE_97(Doc.of(Level.WARNING).text("TBD")), //
	STATE_98(Doc.of(Level.WARNING).text("FixedQ")), //
	STATE_99(Doc.of(Level.WARNING).text("Fixed PF")), //
	STATE_100(Doc.of(Level.WARNING).text("PQ Curve")), //
	STATE_101(Doc.of(Level.WARNING).text("PF Null")), //

	WG_POWER_MODE(Doc.of(PowerMode.values())), //
	WG_POWER_SET(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)),
	RESERVED(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	NO_GRID_CHARGE_ENABLE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	DISCHARGE_WITH_PV_ENABLE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	RESERVED2(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)),
	APP_MODE_INDEX(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),
	GRID_WAVE_CHECK_LEVEL(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),
	METER_CHECK_VALUE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),
	RAPAID_CUT_OFF(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),
	GRID_VOLT_QUALITY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
	GRID_VOLT_HIGH_S2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
	GRID_VOLT_LOW_S2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
	GRID_VOLT_HIGH_S2_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)),
	GRID_VOLT_LOW_S2_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)),
	GRID_VOLT_HIGH_S1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
	GRID_VOLT_LOW_S1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
	GRID_VOLT_HIGH_S1_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)),
	GRID_VOLT_LOW_S1_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)),
	GRID_FREQUENCY_HIGH_S2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)),
	GRID_FREQUENCY_LOW_S2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)),

	GRID_FREQUENCY_HIGH_S2_TIME(Doc.of(OpenemsType.INTEGER).//
			unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)),
	GRID_FREQUENCY_LOW_S2_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)),
	GRID_FREQUENCY_HIGH_S1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)),
	GRID_FREQUENCY_LOW_S1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)),
	GRID_FREQUENCY_HIGH_S1_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)),
	GRID_FREQUENCY_LOW_S1_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)),
	GRID_VOLT_RECOVER_HIGH(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
	GRID_VOLT_RECOVER_LOW(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
	GRID_VOLT_RECOVER_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)),
	GRID_FREQUENCY_RECOVER_HIGH(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)),
	GRID_FREQUENCY_RECOVER_LOW(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)),
	GRID_FREQUENCY_RECOVER_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)),
	POINT_B_VALUE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),
	POINT_C_VALUE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)), // 0.01 - unit
	GRID_LIMIT_BY_VOLTAGE_START_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
	GRID_LIMIT_BY_VOLTAGE_START_PERCENT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),
	GRID_LIMIT_BY_VOLTAGE_SLOPE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),
	ACTIVE_CURVE_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
	DESACTIVE_CURVE_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),

	ENABLE_CURVE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),
	BACKUP_START_DELAY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MINUTE).accessMode(AccessMode.READ_WRITE)),
	RECOVER_TIME_EE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)), // Percent/minute
	SAFETY_COUNTRY(Doc.of(SafetyCountry.values())), //
	ISO_LIMIT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), // unit? 10ko
	BMS_CURRENT_LIOMIT_COEFFICIENT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),
	WMETER_CONNECT_CHECK_FLAG(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),
	METER_CONNECT_STATUS(Doc.of(MeterConnectStatus.values())), //
	UPS_STANDARD_VOLT_TYPE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),
	FUNCTION_STATUS(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),

	STATE_102(Doc.of(Level.WARNING).text("Anti-Islanding")), //
	STATE_103(Doc.of(Level.WARNING).text("LVRT")), //
	STATE_104(Doc.of(Level.WARNING).text("Burn-in Mode")), //
	STATE_105(Doc.of(Level.WARNING).text("Power Limit Function")), //
	STATE_106(Doc.of(Level.WARNING).text("TBD")), //
	STATE_107(Doc.of(Level.WARNING).text("TBD")), //
	STATE_108(Doc.of(Level.WARNING).text("TBD")), //
	STATE_109(Doc.of(Level.WARNING).text("MPPT for Shadow")), //
	STATE_110(Doc.of(Level.WARNING).text("Auto Mode")), //
	STATE_111(Doc.of(Level.WARNING).text("Meter")), //
	STATE_112(Doc.of(Level.WARNING).text("EMS Mode")), //
	STATE_113(Doc.of(Level.WARNING).text("Feeding enable")), //
	STATE_114(Doc.of(Level.WARNING).text("Battery active")), //
	STATE_115(Doc.of(Level.WARNING).text("Ground Fault Flag")), //
	STATE_116(Doc.of(Level.WARNING).text("High Impedance Flag")), //
	STATE_117(Doc.of(Level.WARNING).text("High Impedance Flag")), //

	BATTERY_OFFLINE_VOLT_UNDER_MIN(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
	BATTERY_OFFLINE_SOC_UNDER_MIN(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),
	ONLY_NIGHT_DISCHARGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),
	BMS_PROTOCOL_CODE(Doc.of(ProtocolCodeBMS.values())), //
	HIGH_VOLTAGE_BATTERY_STRING(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),
	OFFLINE_MPPTS_CAN_ENABLE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),

	// Meter Data Address
	ACR_METER_TYPE(Doc.of(ACR_MeterType.values())), METER_STATUS2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), // repeating
	PHASE_A_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	PHASE_B_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	PHASE_C_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	PHASE_A_CURRENT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
	PHASE_B_CURRENT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
	PHASE_C_CURRENT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
	PHASE_A_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
	PHASE_B_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
	PHASE_C_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
	TOTAL_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),

	PHASE_A_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.READ_ONLY)),
	PHASE_B_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.READ_ONLY)),
	PHASE_C_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.READ_ONLY)),
	TOTAL_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.READ_ONLY)),
	PHASE_A_APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE).accessMode(AccessMode.READ_ONLY)),
	PHASE_B_APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE).accessMode(AccessMode.READ_ONLY)),
	PHASE_C_APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE).accessMode(AccessMode.READ_ONLY)),
	TOTAL_APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE).accessMode(AccessMode.READ_ONLY)),
	PHASE_A_POWER_FACTOR(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	PHASE_B_POWER_FACTOR(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	PHASE_C_POWER_FACTOR(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	TOTAL_POWER_FACTOR(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	FREQUENCY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)),
	E_TOTAL_SELL_H2(Doc.of(OpenemsType.FLOAT) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), // E-TOTAL-SELL-H repeated

	E_TOTAL_SELL_L2(Doc.of(OpenemsType.FLOAT) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), // repeating
	E_TOTAL_BUY_H2(Doc.of(OpenemsType.FLOAT) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), // repeating
	E_TOTAL_BUY_L2(Doc.of(OpenemsType.FLOAT) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), // repeating

	VAC1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	FAC1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)),
	PACL(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
	WORK_MODE2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), // repeating
	ERROR_MESSAGE_H2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), // repeating
	ERROR_MESSAGE_L2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), // repeating
	LINE1_AVERAGE_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	LINE1_AVERAGE_FAULT_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)),

	LINE1_V_HIGH_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	LINE1_V_HIGH_FAULT_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)),
	LINE1_V_LOW_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	LINE1_V_LOW_FAULT_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)),
	LINE1_F_HIGH_FAULT_VALUE_COM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)),
	LINE1_F_HIGH_FAULT_TIME_COM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)),
	LINE1_F_LOW_FAULT_VALUE_COM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)),
	LINE1_F_LOW_FAULT_TIME_COM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)),
	LINE1_F_HIGH_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)),
	LINE1_F_HIGH_FAULT_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)),
	LINE1_F_LOW_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)),
	LINE1_F_LOW_FAULT_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)),
	SIM_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	SIM_FREQUENCY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)),
	TEST_RESULT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
	SELF_TEST_STEP(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),
	START_TEST(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)),

	SET_REMOTE_SAFETY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.WRITE_ONLY));

	private final Doc doc;

	private GoodweChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}