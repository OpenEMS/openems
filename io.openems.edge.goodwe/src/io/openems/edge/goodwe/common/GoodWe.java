package io.openems.edge.goodwe.common;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.goodwe.charger.AbstractGoodWeEtCharger;
import io.openems.edge.goodwe.charger.GoodWeChargerPv1;
import io.openems.edge.goodwe.charger.GoodWeChargerPv2;
import io.openems.edge.goodwe.common.enums.AppModeIndex;
import io.openems.edge.goodwe.common.enums.BackupEnable;
import io.openems.edge.goodwe.common.enums.BatteryMode;
import io.openems.edge.goodwe.common.enums.DredCmd;
import io.openems.edge.goodwe.common.enums.DredOffgridCheck;
import io.openems.edge.goodwe.common.enums.EmsPowerMode;
import io.openems.edge.goodwe.common.enums.EnableCurve;
import io.openems.edge.goodwe.common.enums.ExternalEmsFlag;
import io.openems.edge.goodwe.common.enums.FeedPowerEnable;
import io.openems.edge.goodwe.common.enums.FixedPowerFactor;
import io.openems.edge.goodwe.common.enums.GoodweType;
import io.openems.edge.goodwe.common.enums.LedState;
import io.openems.edge.goodwe.common.enums.LoadMode;
import io.openems.edge.goodwe.common.enums.MeterCommunicateStatus;
import io.openems.edge.goodwe.common.enums.MeterConnectCheckFlag;
import io.openems.edge.goodwe.common.enums.MeterConnectStatus;
import io.openems.edge.goodwe.common.enums.OperationMode;
import io.openems.edge.goodwe.common.enums.OutputTypeAC;
import io.openems.edge.goodwe.common.enums.SafetyCountry;
import io.openems.edge.goodwe.common.enums.WifiOrLan;
import io.openems.edge.goodwe.common.enums.WorkMode;
import io.openems.edge.goodwe.common.enums.ZvrtConfig;

public interface GoodWe extends OpenemsComponent {

	/**
	 * Registers a GoodWe Charger.
	 * 
	 * @param charger either {@link GoodWeChargerPv1} or {@link GoodWeChargerPv2}
	 */
	public void addCharger(AbstractGoodWeEtCharger charger);

	/**
	 * Unregisters a GoodWe Charger.
	 * 
	 * @param charger either {@link GoodWeChargerPv1} or {@link GoodWeChargerPv2}
	 */
	public void removeCharger(AbstractGoodWeEtCharger charger);

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		MODBUS_PROTOCOL_VERSION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)),
		RATED_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)),
		AC_OUTPUT_TYPE(Doc.of(OutputTypeAC.values())), //
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_ONLY)),
		DEVICE_TYPE(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_WRITE)), //
		DSP1_SOFTWARE_VERSION(Doc.of(OpenemsType.INTEGER)), //
		DSP2_SOFTWARE_VERSION(Doc.of(OpenemsType.INTEGER)), //
		DSP_SPN_VERSION(Doc.of(OpenemsType.INTEGER)), //
		ARM_SOFTWARE_VERSION(Doc.of(OpenemsType.INTEGER)), //
		ARM_SVN_VERSION(Doc.of(OpenemsType.INTEGER)), //
		DSP_INTERNAL_FIRMWARE_VERSION(Doc.of(OpenemsType.STRING)), //
		ARM_INTERNAL_FIRMWARE_VERSION(Doc.of(OpenemsType.STRING)), //
		SIMCCID(Doc.of(OpenemsType.STRING)), //

		// Running Data
		RTC_YEAR_MONTH(Doc.of(OpenemsType.INTEGER)), //
		RTC_DATE_HOUR(Doc.of(OpenemsType.INTEGER)), //
		RTC_MINUTE_SECOND(Doc.of(OpenemsType.INTEGER)), //
		V_PV1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		I_PV1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		P_PV1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		V_PV2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		I_PV2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		P_PV2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		V_PV3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		I_PV3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		P_PV3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		V_PV4(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		I_PV4(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		P_PV4(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		PV_MODE(Doc.of(WorkMode.values())), //
		VGRID_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		IGRID_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		FGRID_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ)), //
		PGRID_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		VGRID_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		IGRID_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		FGRID_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ)), //
		PGRID_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		VGRID_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		IGRID_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		FGRID_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ)), //
		PGRID_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		TOTAL_INV_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		AC_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		AC_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		AC_APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)), //
		BACK_UP_V_LOAD_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		BACK_UP_I_LOAD_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		BACK_UP_F_LOAD_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ)), //
		LOAD_MODE_R(Doc.of(LoadMode.values())), //
		BACK_UP_P_LOAD_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		BACK_UP_V_LOAD_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		BACK_UP_I_LOAD_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		BACK_UP_F_LOAD_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ)), //
		LOAD_MODE_S(Doc.of(LoadMode.values())), //
		BACK_UP_P_LOAD_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		BACK_UP_V_LOAD_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		BACK_UP_I_LOAD_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		BACK_UP_F_LOAD_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ)), //
		LOAD_MODE_T(Doc.of(LoadMode.values())), //
		BACK_UP_P_LOAD_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
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
				.accessMode(AccessMode.READ_ONLY)), //
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
				.accessMode(AccessMode.READ_ONLY)), //
		SAFETY_COUNTRY(Doc.of(SafetyCountry.values())), // .
		WORK_MODE(Doc.of(WorkMode.values())), //
		OPERATION_MODE(Doc.of(OperationMode.values())), //

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

		// Setting/Controlling Data Registers 45222-45242
		PV_E_TOTAL_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		PV_E_DAY_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_TOTAL_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		H_TOTAL_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HOUR).accessMode(AccessMode.READ_ONLY)), //
		E_DAY_SELL_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_TOTAL_BUY_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_DAY_BUY_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_TOTAL_LOAD_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_LOAD_DAY_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_BATTERY_CHARGE_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_CHARGE_DAY_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_BATTERY_DISCHARGE_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_DISCHARGE_DAY_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		CPLD_WARNING_CODE_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		W_CHARGER_CTRL_FLAG_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		DERATE_FLAG_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		DERATE_FROZEN_POWER_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		DIAG_STATUS_H_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		DIAG_STATUS_L_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		// Error Message 35189
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

		// External Communication Data (ARM)
		// External Communication Data (ARM)
		COM_MODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		RSSI(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		MANIFACTURE_CODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
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
		MT_ACTIVE_POWER_R_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		MT_ACTIVE_POWER_S_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		MT_ACTIVE_POWER_T_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		MT_TOTAL_ACTIVE_POWER_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //

		MT_TOTAL_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.READ_ONLY)), //
		MT_REACTIVE_POWER_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.READ_ONLY)), //
		MT_REACTIVE_POWER_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.READ_ONLY)), //
		MT_REACTIVE_POWER_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.READ_ONLY)), //
		MT_TOTAL_REACTIVE_POWER_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.READ_ONLY)), //

		MT_APPARENT_POWER_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE).accessMode(AccessMode.READ_ONLY)), //
		MT_APPARENT_POWER_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE).accessMode(AccessMode.READ_ONLY)), //
		MT_APPARENT_POWER_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE).accessMode(AccessMode.READ_ONLY)), //
		MT_TOTAL_APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE).accessMode(AccessMode.READ_ONLY)), //

		METER_TYPE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		METER_SOFTWARE_VERSION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		FLASH_PGM_PARA_VER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		FLASH_PGM_WRITE_COUNT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		FLASH_SYS_PARA_VER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		FLASH_SYS_WRITE_COUNT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		FLASH_BAT_PARA_VER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		FLASH_BAT_WRITE_COUNT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		FLASH_EEPROM_PARA_VER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		FLASH_EEPROM_WRITE_COUNT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		WIFI_DATA_SEND_COUNT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		WIFI_UP_DATA_DEBUG(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		DRM_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		METER_PF_R(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		METER_PF_S(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		METER_PF_T(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		METER_POWER_FACTOR(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		METER_FREQUENCE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		E_TOTAL_SELL(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_TOTAL_SELL_2(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		E_TOTAL_BUY_F(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //

		DRM0(Doc.of(OpenemsType.BOOLEAN) //
				.text("DRM Switch 1 On")), //
		DRM1(Doc.of(OpenemsType.BOOLEAN) //
				.text("DRM Switch 2 On")), //
		DRM2(Doc.of(OpenemsType.BOOLEAN) //
				.text("DRM Switch 3 On")), //
		DRM3(Doc.of(OpenemsType.BOOLEAN) //
				.text("DRM Switch 4 On")), //
		DRM4(Doc.of(OpenemsType.BOOLEAN) //
				.text("DRM Switch 5 On")), //
		DRM5(Doc.of(OpenemsType.BOOLEAN) //
				.text("DRM Switch 6 On")), //
		DRM6(Doc.of(OpenemsType.BOOLEAN) //
				.text("DRM Switch 7 On")), //
		DRM7(Doc.of(OpenemsType.BOOLEAN) //
				.text("DRM Switch 8 On")), //
		DRM8(Doc.of(OpenemsType.BOOLEAN) //
				.text("DRM Switch 9 On")), //
		DRED_CONNECT(Doc.of(OpenemsType.BOOLEAN) //
				.text("DRED Connected")), //

		BATTERY_TYPE_INDEX(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		BMS_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		BMS_PACK_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)), //
		BMS_CHARGE_IMAX(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		BMS_DISCHARGE_IMAX(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		BMS_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),
		BMS_SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),
		BMS_BATTERY_STRINGS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		// Table 8-7 BMS Alarm Code
		STATE_42(Doc.of(Level.INFO).text("Charging over voltage 2")), //
		STATE_43(Doc.of(Level.INFO).text("Discharge under voltage 1 ")), //
		STATE_44(Doc.of(Level.INFO).text("Cell high temperature 2")), //
		STATE_45(Doc.of(Level.INFO).text("Charging over current")), //
		STATE_46(Doc.of(Level.INFO).text("Charging over current 2")), //
		STATE_47(Doc.of(Level.INFO).text("Discharging over current 2")), //
		STATE_48(Doc.of(Level.INFO).text("Pre-charge fault")), //
		STATE_49(Doc.of(Level.INFO).text("DC bus fault")), //
		STATE_50(Doc.of(Level.INFO).text("Battery break")), //
		STATE_51(Doc.of(Level.INFO).text("Battery lock")), //
		STATE_52(Doc.of(Level.INFO).text("Discharge circuit fault")), //
		STATE_53(Doc.of(Level.INFO).text("Charging circuit failure")), //
		STATE_54(Doc.of(Level.INFO).text("Communication failure 2")), //
		STATE_55(Doc.of(Level.INFO).text("Cell high temperature 3")), //
		STATE_56(Doc.of(Level.INFO).text("Discharge under voltage 3")), //
		STATE_57(Doc.of(Level.INFO).text("Charging under voltage 3")), //

		// Table 8-8 BMS Warning Code
		STATE_58(Doc.of(Level.WARNING).text("Charging over voltage 1 ")), //
		STATE_59(Doc.of(Level.WARNING).text("Discharging under voltage 1 ")), //
		STATE_60(Doc.of(Level.WARNING).text("Cell high temperature 1 ")), //
		STATE_61(Doc.of(Level.WARNING).text("Cell low temperature 1 ")), //
		STATE_62(Doc.of(Level.WARNING).text("Charging over current 1 ")), //
		STATE_63(Doc.of(Level.WARNING).text("Discharging over current 1 ")), //
		STATE_64(Doc.of(Level.WARNING).text("Communication failure 1 ")), //
		STATE_65(Doc.of(Level.WARNING).text("System reboot ")), //
		STATE_66(Doc.of(Level.WARNING).text("Cell imbalance")), //
		STATE_67(Doc.of(Level.WARNING).text("System low temperature 1 ")), //
		STATE_68(Doc.of(Level.WARNING).text("System low temperature 1 ")), //
		STATE_69(Doc.of(Level.WARNING).text("System high temperature")), //

		// BMS Information
		BATTERY_PROTOCOL(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		BMS_SOFTWARE_VERSION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		BATTERY_HARDWARE_VERSION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		MAXIMUM_CELL_TEMPERATURE_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		MINIMUM_CELL_TEMPERATURE_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		MAXIMUM_CELL_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		MINIMUM_CELL_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		MAXIMUM_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)), //
		MINIMUM_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)), //
		MAXIMUM_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT).accessMode(AccessMode.READ_ONLY)), //
		MINIMUM_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT).accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_4(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_5(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_6(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_7(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_8(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_9(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_10(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_11(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_12(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_13(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_14(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_15(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_16(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_17(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_18(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_19(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_20(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_21(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_22(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_23(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_24(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_25(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_26(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_27(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_28(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_29(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_30(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_31(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PASS_INFORMATION_32(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		// BMS Detailed Information
		BMS_FLAG(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		BMS_WORK_MODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		BMS_ALLOW_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		BMS_ALLOW_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		BMS_RELAY_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		BATTERY_MODULE_NUMBER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		BMS_SHUTDOWN_FAULT_CODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		BATTERY_READY_ENABLE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		ALARM_UNDER_TEMPERATURE_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		ALARM_OVER_TEMPERATURE_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		ALARM_DIFFER_TEMPERATURE_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		ALARM_CHARGE_CURRENT_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		ALARM_DISCHARGE_CURRENT_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		ALARM_CELL_OVER_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		ALARM_CELL_UNDER_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		ALARM_SOC_LOWER_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		ALARM_CELL_VOLTAGE_DIFFER_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		BATTERY_CURRENT_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)), //
		BATTERY_CURRENT_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)), //
		BATTERY_CURRENT_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)), //
		BATTERY_CURRENT_4(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)), //
		BATTERY_CURRENT_5(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)), //
		BATTERY_CURRENT_6(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)), //
		BATTERY_CURRENT_7(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)), //
		BATTERY_CURRENT_8(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)), //
		BATTERY_1_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)), //
		BATTERY_2_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)), //
		BATTERY_3_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)), //
		BATTERY_4_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)), //
		BATTERY_5_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)), //
		BATTERY_6_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)), //
		BATTERY_7_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)), //
		BATTERY_8_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)), //
		BATTERY_1_SN(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		BATTERY_2_SN(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		BATTERY_3_SN(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		BATTERY_4_SN(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		BATTERY_5_SN(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		BATTERY_6_SN(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		BATTERY_7_SN(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		BATTERY_8_SN(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		// For CEI Auto Test
		WORK_MODE_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		ERROR_MESSAGE_H(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		ERROR_MESSAGE_L(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		SIM_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		SIM_FREQUENCY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)), //
		TEST_RESULT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		VAC_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		FAC_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)), //
		PAC_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		LINE_1_AVG_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		LINE_1_AVG_FAULT_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_1_V_HIGH_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		LINE_1_V_HIGH_FAULT_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_1_V_LOW_FAULT_VALUE_S1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		LINE_1_V_LOW_FAULT_TIME_S1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_1_V_LOW_FAULT_VALUE_S2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		LINE_1_V_LOW_FAULT_TIME_S2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_1_F_HIGH_FAULT_VALUE_COM(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)), //
		LINE_1_F_HIGH_FAULT_TIME_COM(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_1_FLOW_FAULT_VALUE_COM(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)), //
		LINE_1_FLOW_FAULT_TIME_COM(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_1_F_HIGH_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)), //
		LINE_1_F_HIGH_FAULT_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_1_F_LOW_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)), //
		LINE_1_F_LOW_FAULT_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		VAC_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		FAC_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)), //
		PAC_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		LINE_2_AVG_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		LINE_2_AVG_FAULT_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_2_V_HIGH_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		LINE_2_V_HIGH_FAULT_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_2_V_LOW_FAULT_VALUE_S1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		LINE_2_V_LOW_FAULT_TIME_S1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_2_V_LOW_FAULT_VALUE_S2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		LINE_2_V_LOW_FAULT_TIME_S2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_2_F_HIGH_FAULT_VALUE_COM(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)), //
		LINE_2_F_HIGH_FAULT_TIME_COM(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_2_FLOW_FAULT_VALUE_COM(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)), //
		LINE_2_FLOW_FAULT_TIME_COM(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_2_F_HIGH_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)), //
		LINE_2_F_HIGH_FAULT_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_2_F_LOW_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)), //
		LINE_2_F_LOW_FAULT_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		VAC_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		FAC_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)), //
		PAC_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		LINE_3_AVG_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		LINE_3_AVG_FAULT_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_3_V_HIGH_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		LINE_3_V_HIGH_FAULT_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_3_V_LOW_FAULT_VALUE_S1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		LINE_3_V_LOW_FAULT_TIME_S1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_3_V_LOW_FAULT_VALUE_S2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		LINE_3_V_LOW_FAULT_TIME_S2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_3_F_HIGH_FAULT_VALUE_COM(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)), //
		LINE_3_F_HIGH_FAULT_TIME_COM(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_3_FLOW_FAULT_VALUE_COM(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)), //
		LINE_3_FLOW_FAULT_TIME_COM(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_3_F_HIGH_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)), //
		LINE_3_F_HIGH_FAULT_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //
		LINE_3_F_LOW_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)), //
		LINE_3_F_LOW_FAULT_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)), //

		// Power Limit
		FEED_POWER_LIMIT_COEFFICIENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.THOUSANDTH).accessMode(AccessMode.READ_ONLY)), //
		L1_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		L2_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		L3_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		INVERTER_POWER_FACTOR(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		PV_METER_DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		E_TOTAL_GRID_CHARGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)), //
		DISPATCH_SWITCH(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		DISPATCH_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		DISPATCH_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)), //
		DISPATCH_MODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		// Setting Parameter
		USER_PASSWORD1(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_WRITE)), //
		USER_PASSWORD2(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_WRITE)), //
		USER_PASSWORD3(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_WRITE)), //
		ROUTER_SSID(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_WRITE)), //
		ROUTER_PASSWORD(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_WRITE)), //
		ROUTER_ENCRYPTION_METHOD(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_WRITE)), //
		DOMAIN1(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_WRITE)), //
		PORT_NUMBER1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		DOMAIN2(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_WRITE)), //
		PORT_NUMBER2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		MODBUS_ADDRESS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		MODBUS_MANUFACTURER(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_WRITE)), //
		MODBUS_BAUDRATE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		RTC_YEAR_MONTH_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		RTC_DAY_HOUR_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		RTC_MINUTE_SECOND_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		SERIAL_NUMBER_2(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_WRITE)), //
		GOODWE_TYPE(Doc.of(GoodweType.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		RESUME_FACTORY_SETTING(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		CLEAR_DATA(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		ALLOW_CONNECT_TO_GRID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		FORBID_CONNECT_TO_GRID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		RESET(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		RESET_SPS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
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
				.accessMode(AccessMode.READ_WRITE)), //
		SAFETY_COUNTRY_CODE(Doc.of(SafetyCountry.values())//
				.accessMode(AccessMode.READ_WRITE)), //
		ISO(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		LVRT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		ISLANDING(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		BURN_IN_RESET_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MINUTE).accessMode(AccessMode.READ_WRITE)), //
		PV_START_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		ENABLE_MPPT4_SHADOW(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		BACK_UP_ENABLE(Doc.of(BackupEnable.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		AUTO_START_BACKUP(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_WAVE_CHECK_LEVEL(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		REPAID_CUT_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		BACKUP_START_DLY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UPS_STD_VOLT_TYPE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UNDER_ATS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		BURN_IN_MODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		BACKUP_OVERLOAD_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UPSPHASE_TYPE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		DERATE_RATE_VDE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		THREE_PHASE_UNBALANCED_OUTPUT(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE).text("All power needs to be turned off and restarted")), //
		PRE_RELAY_CHECK_ENABLE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		HIGH_IMP_MODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		BAT_SP_FUNC(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		AFCI_SHUT_OFF_PWM(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		DEVICE_LICENCE(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		USER_LICENCE(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_WRITE)), //
		REMOTE_USER_LICENCE(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_WRITE)), //
		REMOTE_LOCK_CODE(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_WRITE)), //
		BATTERY_STRINGS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		CPLD_WARNING_CODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		W_CHARGER_CTRL_FLAG(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		DERATE_FLAG(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		DERATE_FROZEN_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		DIAG_STATUS_H(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //
		DIAG_STATUS_L(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		// BMS
		BMS_LEAD_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BMS_STRINGS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		BMS_CHARGE_MAX_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		BMS_CHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE)), //
		BMS_DISCHARGE_MIN_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		BMS_DISCHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE)), //
		BMS_SOC_UNDER_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)), //
		BMS_OFFLINE_DISCHARGE_MIN_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
		BMS_OFFLINE_SOC_UNDER_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
		CLEAR_BATTERY_SETTING(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.WRITE_ONLY)),

		// Safety
		GRID_VOLT_HIGH_S1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_HIGH_S1_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_LOW_S1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_LOW_S1_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_HIGH_S2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_HIGH_S2_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_LOW_S2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_LOW_S2_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_QUALITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_HIGH_S1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_HIGH_S1_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_LOW_S1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_LOW_S1_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_HIGH_S2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_HIGH_S2_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_LOW_S2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_LOW_S2_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_HIGH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_LOW(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_HIGH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_LOW(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)), //
		GRID_START_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //

		// Decide by specific safety regulations
		GRID_VOLT_RECOVER_HIGH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_RECOVER_LOW(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_RECOVER_HIGH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_RECOVER_LOW(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)), //
		GRID_RECOVER_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
		POWER_RATE_LIMIT_GENERATE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		POWER_RATE_LIMIT_RECONNECT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		POWER_RATE_LIMIT_REDUCTION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_PROTECT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		POWER_SLOPE_ENABLE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		// CosPhi curve
		ENABLE_CURVE_PU(Doc.of(EnableCurve.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
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
		POWER_FREQUENCY_ENABLED(Doc.of(OpenemsType.BOOLEAN) //
				.text("Power and Frequency Curve Enabled")), //
		POWER_FREQUENCY_RESPONSE_MODE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Power and Frequency Curve: 0=Slope, 1=Fstop")), //

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
		ENABLE_CURVE_QU(Doc.of(EnableCurve.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
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
				.accessMode(AccessMode.READ_WRITE)), //
		TIME_CONSTANT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		MISCELLANEA(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		RATED_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		RESPONSE_TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		// PU curve
		PU_CURVE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		POWER_CHANGE_RATE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		V1_VOLTAGE_PU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		V1_VALUE_PU(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		V2_VOLTAGE_PU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		V2_VALUE_PU(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		V3_VOLTAGE_PU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		V3_VALUE_PU(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		V4_VOLTAGE_PU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		V4_VALUE_PU(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		FIXED_POWER_FACTOR(Doc.of(FixedPowerFactor.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		FIXED_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		FIXED_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_LIMIT_BY_VOLT_START_VOL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		GRID_LIMIT_BY_VOLT_START_PER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)), //
		GRID_LIMIT_BY_VOLT_SLOPE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT).accessMode(AccessMode.WRITE_ONLY)), //
		AUTO_TEST_ENABLE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		AUTO_TEST_STEP(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		UW_ITALY_FREQ_MODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		ALL_POWER_CURVE_DISABLE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		R_PHASE_FIXED_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.THOUSANDTH).accessMode(AccessMode.READ_WRITE)), //
		S_PHASE_FIXED_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.THOUSANDTH).accessMode(AccessMode.READ_WRITE)), //
		T_PHASE_FIXED_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.THOUSANDTH).accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_HIGH_S3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_HIGH_S3_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_LOW_S3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_LOW_S3_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
		ZVRT_CONFIG(Doc.of(ZvrtConfig.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		LVRT_START_VOLT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		LVRT_END_VOLT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		LVRT_START_TRIP_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
		LVRT_END_TRIP_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
		LVRT_TRIP_LIMIT_VOLT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		HVRT_START_VOLT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		HVRT_END_VOLT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		HVRT_START_TRIP_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
		HVRT_END_TRIP_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
		HVRT_TRIP_LIMIT_VOLT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //

		// Meter Control ARM
		SELECT_WORK_MODE(Doc.of(AppModeIndex.values())), //
		METER_CHECK_VALUE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WMETER_CONNECT_CHECK_FLAG(Doc.of(MeterConnectCheckFlag.values())), //
		SIMULATE_METER_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		BREEZE_ON_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		LOG_DATA_ENABLE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		DATA_SEND_INTERVAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
		DRED_CMD(Doc.of(DredCmd.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		LED_TEST_FLAG(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		WIFI_OR_LAN_SWITCH(Doc.of(WifiOrLan.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		DRED_OFFGRID_CHECK(Doc.of(DredOffgridCheck.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		EXTERNAL_EMS_FLAG(Doc.of(ExternalEmsFlag.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		LED_BLINK_TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WIFI_LED_STATE(Doc.of(LedState.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		COM_LED_STATE(Doc.of(LedState.values()) //
				.accessMode(AccessMode.READ_WRITE)), //

		// Battery Control Data ARM
		STOP_SOC_PROTECT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		BMS_FLOAT_VOLT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		BMS_FLOAT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)), //
		BMS_FLOAT_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MINUTE).accessMode(AccessMode.READ_WRITE)), //
		BMS_TYPE_INDEX_ARM(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		MANUFACTURE_CODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		DC_VOLT_OUTPUT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		BMS_AVG_CHG_VOLT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		BMS_AVG_CHG_HOURS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HOUR).accessMode(AccessMode.READ_WRITE)), //
		FEED_POWER_ENABLE(Doc.of(FeedPowerEnable.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		FEED_POWER_PARA(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)), //
		EMS_POWER_MODE(Doc.of(EmsPowerMode.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		EMS_POWER_SET(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		BMS_CURR_LMT_COFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		BATTERY_PROTOCOL_ARM(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_1_ENABLED(Doc.of(OpenemsType.BOOLEAN) //
				.text("Work Week 1 Enabled")), //
		WORK_WEEK_1_START_TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_1_END_TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_1_BAT_POWER_PERCENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_1_SUNDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_1_MONDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_1_TUESDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_1_WEDNESDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_1_THURSDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_1_FRIDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_1_SATURDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_1_NA(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_2_ENABLED(Doc.of(OpenemsType.BOOLEAN) //
				.text("Work Week 2 Enabled")), //
		WORK_WEEK_2_START_TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_2_END_TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_2_BAT_POWER_PERCENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_2_SUNDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_2_MONDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_2_TUESDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_2_WEDNESDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_2_THURSDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_2_FRIDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_2_SATURDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_2_NA(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_3_ENABLED(Doc.of(OpenemsType.BOOLEAN) //
				.text("Work Week 3 Enabled")), //
		WORK_WEEK_3_START_TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_3_END_TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_3_BAT_POWER_PERCENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_3_SUNDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_3_MONDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_3_TUESDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_3_WEDNESDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_3_THURSDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_3_FRIDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_3_SATURDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_3_NA(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_4_ENABLED(Doc.of(OpenemsType.BOOLEAN) //
				.text("Work Week 4 Enabled")), //
		WORK_WEEK_4_START_TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_4_END_TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_4_BMS_POWER_PERCENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_4_SUNDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_4_MONDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_4_TUESDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_4_WEDNESDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_4_THURSDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_4_FRIDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_4_SATURDAY(Doc.of(OpenemsType.BOOLEAN)), //
		WORK_WEEK_4_NA(Doc.of(OpenemsType.BOOLEAN)), //
		SOC_START_TO_FORCE_CHARGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		SOC_STOP_TO_FORCE_CHARGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		CLEAR_ALL_ECONOMIC_MODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		THREE_PHASE_FEED_POWER_ENABLE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		R_PHASE_FEED_POWER_PARA(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)), //
		S_PHASE_FEED_POWER_PARA(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)), //
		T_PHASE_FEED_POWER_PARA(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)), //
		STOP_SOC_ADJUST(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WIFI_RESET(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		ARM_SOFT_RESET(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //

		// BMS for RS485
		WBMS_VERSION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WBMS_STRINGS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WBMS_CHARGE_MAX_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		WBMS_CHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE)), //
		WBMS_DISCHARGE_MIN_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		WBMS_DISCHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE)), //
		WBMS_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		WBMS_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE)), //
		WBMS_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)), //
		WBMS_SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)), //
		WBMS_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_WRITE)), //
		WBMS_WARNING_CODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WBMS_ALARM_CODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WBMS_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WBMS_DISABLE_TIMEOUT_DETECTION(Doc.of(OpenemsType.INTEGER) //
				.text("Cancel EMS mode BMS communication timeout detection") //
				.accessMode(AccessMode.READ_WRITE)), //
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
	 * Gets the Channel for {@link ChannelId#GOODWE_TYPE}.
	 *
	 * @return the Channel
	 */
	public default Channel<GoodweType> getGoodweTypeChannel() {
		return this.channel(GoodWe.ChannelId.GOODWE_TYPE);
	}

	/**
	 * Gets the Device Type. See {@link ChannelId#GOODWE_TYPE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default GoodweType getGoodweType() {
		return this.getGoodweTypeChannel().value().asEnum();
	}

	// TODO drop these methods
	/**
	 * Gets the Channel for {@link ChannelId#BMS_CHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getBmsChargeMaxCurrentChannel() {
		return this.channel(ChannelId.BMS_CHARGE_MAX_CURRENT);
	}

	/**
	 * Gets the BMS Charge-Max-Current in [A]. See
	 * {@link ChannelId#BMS_CHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsChargeMaxCurrent() {
		return this.getBmsChargeMaxCurrentChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#BMS_CHARGE_MAX_CURRENT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBmsChargeMaxCurrent(Integer value) {
		this.getBmsChargeMaxCurrentChannel().setNextValue(value);
	}

	/**
	 * Gets the BMS Charge-Max-Current in [A]. See
	 * {@link ChannelId#BMS_CHARGE_MAX_CURRENT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setBmsChargeMaxCurrent(Integer value) throws OpenemsNamedException {
		this.getBmsChargeMaxCurrentChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BMS_DISCHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getBmsDischargeMaxCurrentChannel() {
		return this.channel(ChannelId.BMS_DISCHARGE_MAX_CURRENT);
	}

	/**
	 * Gets the BMS Discharge-Max-Current in [A]. See
	 * {@link ChannelId#BMS_DISCHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsDischargeMaxCurrent() {
		return this.getBmsDischargeMaxCurrentChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#BMS_DISCHARGE_MAX_CURRENT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBmsDischargeMaxCurrent(Integer value) {
		this.getBmsDischargeMaxCurrentChannel().setNextValue(value);
	}

	/**
	 * Gets the BMS Discharge-Max-Current in [A]. See
	 * {@link ChannelId#BMS_DISCHARGE_MAX_CURRENT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setBmsDischargeMaxCurrent(Integer value) throws OpenemsNamedException {
		this.getBmsDischargeMaxCurrentChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BMS_CHARGE_MAX_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getBmsChargeMaxVoltageChannel() {
		return this.channel(ChannelId.BMS_CHARGE_MAX_VOLTAGE);
	}

	/**
	 * Gets the BMS Charge-Max-Voltage in [V]. See
	 * {@link ChannelId#BMS_CHARGE_MAX_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsChargeMaxVoltage() {
		return this.getBmsChargeMaxVoltageChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#BMS_CHARGE_MAX_VOLTAGE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBmsChargeMaxVoltage(Integer value) {
		this.getBmsChargeMaxVoltageChannel().setNextValue(value);
	}

	/**
	 * Gets the BMS Charge-Max-Voltage in [V]. See
	 * {@link ChannelId#BMS_CHARGE_MAX_VOLTAGE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setBmsChargeMaxVoltage(Integer value) throws OpenemsNamedException {
		this.getBmsChargeMaxVoltageChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BMS_DISCHARGE_MIN_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getBmsDischargeMinVoltageChannel() {
		return this.channel(ChannelId.BMS_DISCHARGE_MIN_VOLTAGE);
	}

	/**
	 * Gets the BMS Discharge-Min-Voltage in [V]. See
	 * {@link ChannelId#BMS_DISCHARGE_MIN_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsDischargeMinVoltage() {
		return this.getBmsDischargeMinVoltageChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#BMS_DISCHARGE_MIN_VOLTAGE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBmsDischargeMinVoltage(Integer value) {
		this.getBmsDischargeMinVoltageChannel().setNextValue(value);
	}

	/**
	 * Gets the BMS Discharge-Min-Voltage in [V]. See
	 * {@link ChannelId#BMS_DISCHARGE_MIN_VOLTAGE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setBmsDischargeMinVoltage(Integer value) throws OpenemsNamedException {
		this.getBmsDischargeMinVoltageChannel().setNextWriteValue(value);
	}
}
