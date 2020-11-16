package io.openems.edge.goodwe.ess;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.goodwe.charger.AbstractGoodWeEtCharger;
import io.openems.edge.goodwe.charger.GoodWeChargerPv1;
import io.openems.edge.goodwe.charger.GoodWeChargerPv2;
import io.openems.edge.goodwe.ess.applypower.ApplyPowerStateMachine;
import io.openems.edge.goodwe.ess.enums.AppModeIndex;
import io.openems.edge.goodwe.ess.enums.BatteryMode;
import io.openems.edge.goodwe.ess.enums.GoodweType;
import io.openems.edge.goodwe.ess.enums.LoadMode;
import io.openems.edge.goodwe.ess.enums.MeterCommunicateStatus;
import io.openems.edge.goodwe.ess.enums.MeterConnectCheckFlag;
import io.openems.edge.goodwe.ess.enums.MeterConnectStatus;
import io.openems.edge.goodwe.ess.enums.OperationMode;
import io.openems.edge.goodwe.ess.enums.OutputTypeAC;
import io.openems.edge.goodwe.ess.enums.PowerModeEms;
import io.openems.edge.goodwe.ess.enums.SafetyCountry;
import io.openems.edge.goodwe.ess.enums.WorkMode;

public interface GoodWeEss extends SymmetricEss, OpenemsComponent {

	/**
	 * Registers a Charger with the ESS.
	 * 
	 * @param charger either {@link GoodWeChargerPv1} or {@link GoodWeChargerPv2}
	 */
	public void addCharger(AbstractGoodWeEtCharger charger);

	/**
	 * Unregisters a Charger from the ESS.
	 * 
	 * @param charger either {@link GoodWeChargerPv1} or {@link GoodWeChargerPv2}
	 */
	public void removeCharger(AbstractGoodWeEtCharger charger);

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		APPLY_POWER_STATE_MACHINE(Doc.of(ApplyPowerStateMachine.State.values())),

		MODBUS_PROTOCOL_VERSION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		RATED_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		AC_OUTPUT_TYPE(Doc.of(OutputTypeAC.values())), //
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		GOODWE_TYPE(Doc.of(GoodweType.values()) //
				.accessMode(AccessMode.READ_ONLY)),
		DSP1_SOFTWARE_VERSION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		DSP2_SOFTWARE_VERSION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		DSP_SPN_VERSION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		ARM_SOFTWARE_VERSION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		ARM_SVN_VERSION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		DSP_INTERNAL_FIRMWARE_VERSION(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		ARM_INTERNAL_FIRMWARE_VERSION(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		SIMCCID(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),

		// Running Data
		RTC_YEAR_MONTH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		RTC_DATE_HOUR(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		RTC_MINUTE_SECOND(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		V_PV3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		I_PV3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		P_PV3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
		V_PV4(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		I_PV4(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		P_PV4(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
		PV_MODE(Doc.of(WorkMode.values())), //
		TOTAL_INV_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		AC_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		AC_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.READ_ONLY)), //
		AC_APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE).accessMode(AccessMode.READ_ONLY)), //
		BACK_UP_V_LOAD_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		BACK_UP_I_LOAD_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		BACK_UP_F_LOAD_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)),
		LOAD_MODE_R(Doc.of(LoadMode.values())), //
		BACK_UP_P_LOAD_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
		BACK_UP_V_LOAD_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		BACK_UP_I_LOAD_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		BACK_UP_F_LOAD_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)),
		LOAD_MODE_S(Doc.of(LoadMode.values())), //
		BACK_UP_P_LOAD_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
		BACK_UP_V_LOAD_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
		BACK_UP_I_LOAD_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		BACK_UP_F_LOAD_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)),
		LOAD_MODE_T(Doc.of(LoadMode.values())), //
		BACK_UP_P_LOAD_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
		P_LOAD_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		P_LOAD_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		P_LOAD_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		TOTAL_BACK_UP_LOAD(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		TOTAL_LOAD_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		UPS_LOAD_PERCENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)), //
		AIR_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)), //
		MODULE_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)), //
		RADIATOR_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)), //
		FUNCTION_BIT_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		BUS_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		NBUS_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		V_BATTERY1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		I_BATTERY1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		P_BATTERY1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		BATTERY_MODE(Doc.of(BatteryMode.values())), //
		WARNING_CODE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		SAFETY_COUNTRY(Doc.of(SafetyCountry.values())), // .
		WORK_MODE(Doc.of(WorkMode.values())), //
		OPERATION_MODE(Doc.of(OperationMode.values())), //

		// Error Message
		STATE_0(Doc.of(Level.FAULT).text("The GFCI detecting circuit is abnormal")), //
		STATE_1(Doc.of(Level.FAULT).text("The output current sensor is abnormal")), //
		STATE_2(Doc.of(Level.WARNING).text("TBD")), //
		STATE_3(Doc.of(Level.FAULT).text("DCI Consistency Failure")), //
		STATE_4(Doc.of(Level.FAULT).text("GFCI Consistency Failure")), //
		STATE_5(Doc.of(Level.WARNING).text("TBD")), //
		STATE_6(Doc.of(Level.FAULT).text("GFCI Device Failure")), //
		STATE_7(Doc.of(Level.FAULT).text("Relay Device Failure")), //
		STATE_8(Doc.of(Level.FAULT).text("AC HCT Failure")), //
		STATE_9(Doc.of(Level.FAULT).text("Utility Loss")), //
		STATE_10(Doc.of(Level.FAULT).text("Gournd I Failure")), //
		STATE_11(Doc.of(Level.WARNING).text("DC Bus High")), //
		STATE_12(Doc.of(Level.FAULT).text("Internal Fan Failure(Back-Up Over Load for ES)")), //
		STATE_13(Doc.of(Level.WARNING).text("Over Temperature")), //
		STATE_14(Doc.of(Level.FAULT).text("Auto Test Failure")), //
		STATE_15(Doc.of(Level.WARNING).text("PV Over Voltage")), //
		STATE_16(Doc.of(Level.FAULT).text("External Fan Failure")), //
		STATE_17(Doc.of(Level.FAULT).text("Vac Failure")), //
		STATE_18(Doc.of(Level.FAULT).text("Isolation Failure")), //
		STATE_19(Doc.of(Level.WARNING).text("DC Injection High")), //
		STATE_20(Doc.of(Level.WARNING).text("TBD")), //
		STATE_21(Doc.of(Level.WARNING).text("TBD")), //
		STATE_22(Doc.of(Level.FAULT).text("Fac Consistency Failure")), //
		STATE_23(Doc.of(Level.FAULT).text("Vac Consistency Failure")), //
		STATE_24(Doc.of(Level.WARNING).text("TBD")), //
		STATE_25(Doc.of(Level.WARNING).text("Relay Check Failure")), //
		STATE_26(Doc.of(Level.WARNING).text("TBD")), //
		STATE_27(Doc.of(Level.WARNING).text("TBD")), //
		STATE_28(Doc.of(Level.WARNING).text("TBD")), //
		STATE_29(Doc.of(Level.FAULT).text("Fac Failure")), //
		STATE_30(Doc.of(Level.FAULT).text("EEPROM R/W Failure")), //
		STATE_31(Doc.of(Level.FAULT).text("Internal Communication Failure")), //

		PV_E_TOTAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		PV_E_DAY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_TOTAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		H_TOTAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HOUR).accessMode(AccessMode.READ_ONLY)), //
		E_DAY_SELL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_TOTAL_BUY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_DAY_BUY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_TOTAL_LOAD(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_LOAD_DAY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_BATTERY_CHARGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_CHARGE_DAY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_BATTERY_DISCHARGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_DISCHARGE_DAY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		BATT_STRINGS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		CPLD_WARNING_CODE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		W_CHARGER_CTRL_FLAG(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		DERATE_FLAG(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		DERATE_FROZEN_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		DIAG_STATUS_H(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		DIAG_STATUS_L(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //

		// External Communication Data (ARM)
		COM_MODE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		RSSI(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		MANIFACTURE_CODE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		B_METER_COMMUNICATE_STATUS(Doc.of(MeterConnectStatus.values())), //
		METER_COMMUNICATE_STATUS(Doc.of(MeterCommunicateStatus.values())), //
		MT_ACTIVE_POWER_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		MT_ACTIVE_POWER_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		MT_ACTIVE_POWER_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		MT_TOTAL_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		MT_TOTAL_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		METER_PF_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		METER_PF_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		METER_PF_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		METER_POWER_FACTOR(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		METER_FREQUENCE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		E_TOTAL_SELL(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_TOTAL_BUY2(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //

		STATE_32(Doc.of(Level.WARNING).text("DRM0")), //
		STATE_33(Doc.of(Level.WARNING).text("DRM1")), //
		STATE_34(Doc.of(Level.WARNING).text("DRM2")), //
		STATE_35(Doc.of(Level.WARNING).text("DRM3")), //
		STATE_36(Doc.of(Level.WARNING).text("DRM4")), //
		STATE_37(Doc.of(Level.WARNING).text("DRM5")), //
		STATE_38(Doc.of(Level.WARNING).text("DRM6")), //
		STATE_39(Doc.of(Level.WARNING).text("DRM7")), //
		STATE_40(Doc.of(Level.WARNING).text("DRM8")), //
		STATE_41(Doc.of(Level.WARNING).text("DRED Connect Status")), //

		BATTERY_TYPE_INDEX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		BMS_STATUS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		BMS_PACK_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)), //
		BMS_CHARGE_IMAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		BMS_DISCHARGE_IMAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //

		STATE_42(Doc.of(Level.WARNING).text("Charging over-voltage2")), //
		STATE_43(Doc.of(Level.WARNING).text("Discharging under-voltage2")), //
		STATE_44(Doc.of(Level.WARNING).text("CellHigh temperature2")), //
		STATE_45(Doc.of(Level.WARNING).text("CellLow temperature2")), //
		STATE_46(Doc.of(Level.WARNING).text("Charging overcurrent2")), //
		STATE_47(Doc.of(Level.WARNING).text("Discharging overcurrent2")), //
		STATE_48(Doc.of(Level.WARNING).text("Precharge fault")), //
		STATE_49(Doc.of(Level.WARNING).text("DC bus fault")), //
		STATE_50(Doc.of(Level.WARNING).text("Battery break")), //
		STATE_51(Doc.of(Level.WARNING).text("Battery Lock")), //
		STATE_52(Doc.of(Level.WARNING).text("Discharge circuit Fault")), //
		STATE_53(Doc.of(Level.WARNING).text("Charging circuit Failure")), //
		STATE_54(Doc.of(Level.WARNING).text("Communication failure2")), //
		STATE_55(Doc.of(Level.WARNING).text("Cell High temperature3")), //
		STATE_56(Doc.of(Level.WARNING).text("Discharging under-voltage3")), //
		STATE_57(Doc.of(Level.WARNING).text("Charging over-voltage3")), //

		BMS_SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),
		BMS_BATTERY_STRINGS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //

		STATE_58(Doc.of(Level.WARNING).text("Charging over-voltage1")), //
		STATE_59(Doc.of(Level.WARNING).text("Discharging under-voltage1")), //
		STATE_60(Doc.of(Level.WARNING).text("Cell High temperature1")), //
		STATE_61(Doc.of(Level.WARNING).text("Cell Low temperature1")), //
		STATE_62(Doc.of(Level.WARNING).text("Charging over-current1")), //
		STATE_63(Doc.of(Level.WARNING).text("Discharging over-current1")), //
		STATE_64(Doc.of(Level.WARNING).text("communication failure1")), //
		STATE_65(Doc.of(Level.WARNING).text("System Reboot")), //
		STATE_66(Doc.of(Level.WARNING).text("Cell- imbalance")), //
		STATE_67(Doc.of(Level.WARNING).text("System Low temperature1")), //
		STATE_68(Doc.of(Level.WARNING).text("System Low temperature2")), //
		STATE_69(Doc.of(Level.WARNING).text("System High temperature")), //

		BATTERY_PROTOCOL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //

		// Setting Parameter
		USER_PASSWORD1(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		USER_PASSWORD2(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		USER_PASSWORD3(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		ROUTER_SSID(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		ROUTER_PASSWORD(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		ROUTER_ENCRYPTION_METHOD(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		DOMAIN1(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		PORT_NUMBER1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		DOMAIN2(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		PORT_NUMBER2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		MODBUS_ADDRESS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		MODBUS_MANUFACTURER(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		MODBUS_BADRATE_485(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		RTC_YEAR_MONTH_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		RTC_DAY_HOUR_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		RTC_MINUTE_SECOND_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		SERIAL_NUMBER_2(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		DEVICE_TYPE_2(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		RESUME_FACTORY_SETTING(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		CLEAR_DATA(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		START(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.WRITE_ONLY)), //
		STOP(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.WRITE_ONLY)), //
		RESET(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.WRITE_ONLY)), //
		RESET_SPS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.WRITE_ONLY)), //
		PV_E_TOTAL_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_WRITE)), //
		PV_E_DAY_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_WRITE)), //
		E_TOTAL_SELL_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_WRITE)), //
		H_TOTAL_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HOUR).accessMode(AccessMode.READ_WRITE)), //
		E_DAY_SELL_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_WRITE)), //
		E_TOTAL_BUY_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_WRITE)), //
		E_DAY_BUY_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_WRITE)), //
		E_TOTAL_LOAD_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_WRITE)), //
		E_LOAD_DAY_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_WRITE)), //
		E_BATTERY_CHARGE_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_WRITE)), //
		E_CHARGE_DAY_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_WRITE)), //
		E_BATTERY_DISCHARGE_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_WRITE)), //
		E_DISCHARGE_DAY_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_WRITE)), //
		LANGUAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		SAFETY_COUNTRY_CODE(Doc.of(SafetyCountry.values())), //
		ISO(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		LVRT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		ISLANDING(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BURN_IN_RESET_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MINUTE).accessMode(AccessMode.READ_WRITE)), //
		PV_START_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		ENABLE_MPPT_4SHADOW(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BACK_UP_ENABLE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		AUTO_START_BACKUP(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		GRID_WAVE_CHECK_LEVEL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		REPAID_CUT_OFF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BACKUP_START_DLY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		UPS_STD_VOLT_TYPE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		UNDER_ATS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BURN_IN_MODE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BACKUP_OVERLOAD_DELAY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		UPSPHASE_TYPE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		DERATE_RATE_VDE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		LEAD_BAT_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BATTERY_STRINGS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BATT_CHARGE_VOLT_MAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BATT_CHARGE_CURR_MAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BATT_VOLT_UNDER_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BATT_DISCHARGE_CURR_MAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BATT_SOC_UNDER_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BATT_OFF_LINE_VOLT_UNDER_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BATT_OFFLINE_SOC_UNDER_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		CLEAR_BATTERY_SETTING(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.WRITE_ONLY)), //

		// CosPhi curve
		ENABLE_CURVE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		POINT_A_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)), //
		POINT_A_PF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)), //
		POINT_B_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)), //
		POINT_B_PF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)), //
		POINT_C_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)), //
		POINT_C_PF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)), //
		LOCK_IN_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		LOCK_OUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		LOCK_OUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)), //

		// Power and frequency curve
		STATE_70(Doc.of(Level.INFO).text("ON/OFF")), //
		STATE_71(Doc.of(Level.INFO).text("response mode")), //

		FFROZEN_DCH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)), //
		FFROZEN_CH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)), //
		FSTOP_DCH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)), //
		FSTOP_CH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)), //
		RECOVERY_WAITING_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
		RECOVERY_FREQURNCY1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)), //
		RECOVERY_FREQUENCY2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)), //
		RECOVERY_SLOPE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		FFROZEN_DCH_SLOPE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)), //
		FFROZEN_CH_SLOPE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)), //
		DOWN_SLOPE_POWER_REFERENCE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		DOWN_SLOP(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //

		// QU curve
		ENABLE_CURVE_QU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		LOCK_IN_POWER_QU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)), //
		LOCK_OUT_POWER_QU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)), //
		V1_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		V1_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)), //
		V2_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		V2_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)), //
		V3_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		V3_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)), //
		V4_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		V4_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)), //
		K_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		TIME_CONSTANT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		MISCELLANEA(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		RATED_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		RESPONSE_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //

		// PU curve
		PU_CURVE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		POWER_CHANGE_RATE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		V1_VOLTAGE_PU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		V1_VALUE_PU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		V2_VOLTAGE_PU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		V2_VALUE_PU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		V3_VOLTAGE_PU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		V3_VALUE_PU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		V4_VOLTAGE_PU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		V4_VALUE_PU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		FIXED_POWER_FACTOR(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		FIXED_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		FIXED_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		GRID_LIMIT_BY_VOLT_START_VOL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		GRID_LIMIT_BY_VOLT_START_PER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)), //
		GRID_LIMIT_BY_VOLT_SLOPE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.WRITE_ONLY)), //
		AUTO_TEST_ENABLE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.WRITE_ONLY)), //
		AUTO_TEST_STEP(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.WRITE_ONLY)), //
		UW_ITALY_FREQ_MODE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //

		// Meter Control ARM
		APP_MODE_INDEX(Doc.of(AppModeIndex.values())), //
		METER_CHECK_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		WMETER_CONNECT_CHECK_FLAG(Doc.of(MeterConnectCheckFlag.values())), //
		SIMULATE_METER_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BREEZE_ON_OFF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		LOG_DATA_ENABLE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		DATA_SEND_INTERVAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //

		// Battery Control Data ARM
		STOP_SOC_PROTECT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BATTERY_FLOAT_VOLT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		BATTERY_FLOAT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)), //
		BATTERY_FLOAT_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MINUTE).accessMode(AccessMode.READ_WRITE)), //
		BATTERY_TYPE_INDEX_ARM(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		MANUFACTURE_CODE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		DC_VOLT_OUTPUT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BAT_AVG_CHG_VOLT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		BAT_AVG_CHG_HOURS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HOUR).accessMode(AccessMode.READ_WRITE)), //
		FEED_POWER_ENABLE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		FEED_POWER_PARA(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)), //
		EMS_POWER_MODE(Doc.of(PowerModeEms.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		EMS_POWER_SET(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BAT_BMS_CURR_LMT_COFF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BATTERY_PROTOCOL_ARM(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		START_TIME_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		END_TIME_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BAT_POWER_PERCENT_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)), //

		STATE_72(Doc.of(Level.INFO).text("SUNDAY")), //
		STATE_73(Doc.of(Level.WARNING).text("MONDAY")), //
		STATE_74(Doc.of(Level.WARNING).text("TUESDAY")), //
		STATE_75(Doc.of(Level.WARNING).text("Wednesday")), //
		STATE_76(Doc.of(Level.WARNING).text("Thursday")), //
		STATE_77(Doc.of(Level.WARNING).text("Friday")), //
		STATE_78(Doc.of(Level.WARNING).text("Saturday")), //

		START_TIME_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		END_TIME_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BAT_POWER_PERCENT_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)), //
		START_TIME_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		END_TIME_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BAT_POWER_PERCENT_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)), //
		START_TIME_4(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		END_TIME_4(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BAT_POWER_PERCENT_4(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)), //
		SOC_START_TO_FORCE_CHARGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		SOC_STOP_TO_FORCE_CHARGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		CLEAR_ALL_ECONOMIC_MODE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.WRITE_ONLY)), //

		// BMS for RS485
		BMS_VERSION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BATT_STRINGS_RS485(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		WBMS_BAT_CHARGE_VMAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		WBMS_BAT_CHARGE_IMAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)), //
		WBMS_BAT_DISCHARGE_VMIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		WBMS_BAT_DISCHARGE_IMAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)), //
		WBMS_BAT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		WBMS_BAT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)), //
		WBMS_BAT_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)), //
		WBMS_BAT_SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)), //
		WBMS_BAT_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)), //

		// BMS_STATUS(), //
		STATE_79(Doc.of(Level.INFO).text("force to charge")), //
		STATE_80(Doc.of(Level.INFO).text("Stop charging")), // TODO can be removed?
		STATE_81(Doc.of(Level.INFO).text("Stop discharging"));

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
	 * Gets the Channel for {@link ChannelId#GOODWE_TYPE}.
	 *
	 * @return the Channel
	 */
	public default Channel<GoodweType> getGoodweTypeChannel() {
		return this.channel(GoodWeEss.ChannelId.GOODWE_TYPE);
	}

	/**
	 * Gets the Device Type. See {@link ChannelId#GOODWE_TYPE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default GoodweType getGoodweType() {
		return this.getGoodweTypeChannel().value().asEnum();
	}

}
