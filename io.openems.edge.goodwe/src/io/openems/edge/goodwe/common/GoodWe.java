package io.openems.edge.goodwe.common;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.goodwe.charger.GoodWeCharger;
import io.openems.edge.goodwe.common.enums.AppModeIndex;
import io.openems.edge.goodwe.common.enums.ArcSelfCheckStatus;
import io.openems.edge.goodwe.common.enums.BatteryMode;
import io.openems.edge.goodwe.common.enums.BatteryProtocol;
import io.openems.edge.goodwe.common.enums.ComMode;
import io.openems.edge.goodwe.common.enums.CpldWarningCode;
import io.openems.edge.goodwe.common.enums.DredCmd;
import io.openems.edge.goodwe.common.enums.DredOffgridCheck;
import io.openems.edge.goodwe.common.enums.EhBatteryFunctionActive;
import io.openems.edge.goodwe.common.enums.EmsCheck;
import io.openems.edge.goodwe.common.enums.EmsPowerMode;
import io.openems.edge.goodwe.common.enums.EnableCurve;
import io.openems.edge.goodwe.common.enums.ExternalEmsFlag;
import io.openems.edge.goodwe.common.enums.EzloggerProCommStatus;
import io.openems.edge.goodwe.common.enums.FeedInPowerSettings.FixedPowerFactor;
import io.openems.edge.goodwe.common.enums.GoodWeGridMeterType;
import io.openems.edge.goodwe.common.enums.GoodWeType;
import io.openems.edge.goodwe.common.enums.GridProtect;
import io.openems.edge.goodwe.common.enums.GridWaveCheckLevel;
import io.openems.edge.goodwe.common.enums.LedState;
import io.openems.edge.goodwe.common.enums.LoadMode;
import io.openems.edge.goodwe.common.enums.LoadRegulationIndex;
import io.openems.edge.goodwe.common.enums.MeterCommunicateStatus;
import io.openems.edge.goodwe.common.enums.MeterConnectCheckFlag;
import io.openems.edge.goodwe.common.enums.OperationMode;
import io.openems.edge.goodwe.common.enums.OutputTypeAC;
import io.openems.edge.goodwe.common.enums.PvMode;
import io.openems.edge.goodwe.common.enums.SafetyCountry;
import io.openems.edge.goodwe.common.enums.UpsStandartVoltType;
import io.openems.edge.goodwe.common.enums.WifiOrLan;
import io.openems.edge.goodwe.common.enums.WorkMode;
import io.openems.edge.goodwe.common.enums.WorkWeek;
import io.openems.edge.goodwe.common.enums.ZvrtConfig;

public interface GoodWe extends OpenemsComponent {

	/**
	 * Registers a GoodWe Charger.
	 *
	 * @param charger {@link GoodWeCharger} charger
	 */
	public void addCharger(GoodWeCharger charger);

	/**
	 * Unregisters a GoodWe Charger.
	 *
	 * @param charger {@link GoodWeCharger} charger
	 */
	public void removeCharger(GoodWeCharger charger);

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		AC_OUTPUT_TYPE(Doc.of(OutputTypeAC.values())), //
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.HIGH) //
				.accessMode(AccessMode.READ_WRITE)),
		EMS_CHECK_INVERTER_OPERATION_STATUS(Doc.of(EmsCheck.values())), //
		DSP_FM_VERSION_MASTER(Doc.of(OpenemsType.INTEGER)), //
		DSP_FM_VERSION_SLAVE(Doc.of(OpenemsType.INTEGER)), //
		DSP_BETA_VERSION(Doc.of(OpenemsType.INTEGER)), //
		ARM_FM_VERSION(Doc.of(OpenemsType.INTEGER)), //
		ARM_BETA_VERSION(Doc.of(OpenemsType.INTEGER)), //
		DSP_INTERNAL_FIRMWARE_VERSION(Doc.of(OpenemsType.STRING)), //
		DSP_DCDC_FM_VERSION(Doc.of(OpenemsType.INTEGER)), //
		DSP_MPPT_BETA_VERSION(Doc.of(OpenemsType.INTEGER)), //
		DSP_STS_FM_VERSION(Doc.of(OpenemsType.INTEGER)), //

		// Running Data
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
		PV_MODE(Doc.of(PvMode.values())), //
		TOTAL_INV_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //

		/*
		 * Channels for multiple String charger in one MPPT. Channels only set and used
		 * for GoodWe 20/30
		 *
		 * MPPT1
		 */
		TWO_S_MPPT1_P(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		TWO_S_MPPT1_I(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		TWO_S_PV1_V(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		TWO_S_PV1_I(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		TWO_S_PV2_V(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		TWO_S_PV2_I(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		/*
		 * MPPT2
		 */
		TWO_S_MPPT2_P(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		TWO_S_MPPT2_I(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		TWO_S_PV3_V(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		TWO_S_PV3_I(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		TWO_S_PV4_V(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		TWO_S_PV4_I(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		/*
		 * MPPT3
		 */
		TWO_S_MPPT3_P(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		TWO_S_MPPT3_I(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		TWO_S_PV5_V(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		TWO_S_PV5_I(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		TWO_S_PV6_V(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		TWO_S_PV6_I(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //

		/**
		 * Total Active Power Of Inverter.
		 *
		 * <p>
		 * (If meter connection ok, it is meter power.If meter connection fail, it is
		 * inverter on-grid port power)
		 */
		AC_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		AC_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		AC_APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)), //

		/**
		 * Off means there is No voltage of Backup port. Also used for 1-p inverter
		 */
		LOAD_MODE_R(Doc.of(LoadMode.values())), //
		LOAD_MODE_S(Doc.of(LoadMode.values())), //
		LOAD_MODE_T(Doc.of(LoadMode.values())), //
		P_LOAD_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		P_LOAD_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		P_LOAD_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		TOTAL_BACK_UP_LOAD_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		TOTAL_LOAD_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		UPS_LOAD_PERCENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)), //
		AIR_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		MODULE_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		RADIATOR_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		FUNCTION_BIT_VALUE(Doc.of(OpenemsType.INTEGER)), //
		BUS_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		NBUS_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		V_BATTERY1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		I_BATTERY1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		P_BATTERY1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		BATTERY_MODE(Doc.of(BatteryMode.values())), //
		SAFETY_COUNTRY(Doc.of(SafetyCountry.values())), // .
		WORK_MODE(Doc.of(WorkMode.values())), //
		OPERATION_MODE(Doc.of(OperationMode.values())), //

		PV_E_TOTAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		PV_E_DAY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		H_TOTAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HOUR)), //
		E_DAY_SELL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		E_TOTAL_BUY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		E_DAY_BUY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		E_TOTAL_LOAD(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		E_LOAD_DAY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		E_BATTERY_CHARGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		E_CHARGE_DAY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		E_BATTERY_DISCHARGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		E_DISCHARGE_DAY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //

		// Setting/Controlling Data Registers 45222-45242
		PV_E_TOTAL_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		PV_E_DAY_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		H_TOTAL_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HOUR)), //
		E_DAY_SELL_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		E_TOTAL_BUY_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		E_DAY_BUY_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		E_TOTAL_LOAD_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		E_LOAD_DAY_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		E_BATTERY_CHARGE_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		E_CHARGE_DAY_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		E_BATTERY_DISCHARGE_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		E_DISCHARGE_DAY_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //

		// Error Message 35189
		STATE_0(Doc.of(Level.FAULT) //
				.text("The Ground Fault Circuit Interrupter (GFCI) detecting circuit is abnormal " //
						+ "| Interne Fehlerstrom-Schutzeinrichtung (RCD Einheit) wurde ausgelöst " //
						+ "| Bitte überprüfen Sie den Netzanschluss sowie ggf. Backup-Lasten")), //

		STATE_1(Doc.of(Level.FAULT) //
				.text("The output current sensor is abnormal " //
						+ "| Der Ausgangs-Stromsensor liefert unplausible Werte " //
						+ "| Bitte überprüfen Sie die Installation")), //

		// Bit 2 - TBD (Currently reserved) - warning should not appear unless there is
		// a new description of GoodWe
		STATE_2(Doc.of(Level.WARNING) //
				.text("Warning Code 1")), //

		STATE_3(Doc.of(Level.FAULT) //
				.text("DCI Consistency Failure " //
						+ "| Werte der Impedanzmessung (DCI Einheit) sind widersprüchlich/unplausibel " //
						+ "| Bitte überprüfen Sie den Netzanschluss")), //

		STATE_4(Doc.of(Level.FAULT) //
				.text("Ground Fault Circuit Interrupter (GFCI) Consistency Failure " //
						+ "| Werte der internen Fehlerstrom-Schutzeinrichtung (RCD) sind widersprüchlich/unplausibel " //
						+ "| Bitte überprüfen Sie den Netzanschluss")), //

		// Bit 5 - TBD (Currently reserved) - warning should not appear unless there is
		// a new description of GoodWe
		STATE_5(Doc.of(Level.WARNING) //
				.text("Warning Code 2")), //

		STATE_6(Doc.of(Level.FAULT) //
				.text("Ground Fault Circuit Interrupter (GFCI) Device Failure " //
						+ "| Interne Fehlerstrom-Schutzeinrichtung (RCD Einheit) befindet sich im Fehlerzustand " //
						+ "| Bitte führen Sie einen Geräteneustart aus")), //

		STATE_7(Doc.of(Level.FAULT) //
				.text("Relay Device Failure " //
						+ "| Interne Relais befinden sich im Fehlerzustand " //
						+ "| Bitte führen Sie einen Geräteneustart aus")), //

		STATE_8(Doc.of(Level.FAULT) //
				.text("AC HCT Failure " //
						+ "| Die HCT Einheit befindet sich im Fehlerzustand " //
						+ "| Bitte führen Sie einen Geräteneustart aus")), //

		STATE_9(Doc.of(Level.FAULT) //
				.text("Utility Loss " //
						+ "| Netzausfall wurde erkannt " //
						+ "| Bitte überprüfen Sie ob das Kommunikationsmodul richtig gesteckt ist")), //

		// TODO: Use new-lines or html-lists when the UI and edge log are able to handle
		// them
		STATE_10(Doc.of(Level.FAULT) //
				.text("Ground I Failure " //
						+ "| Erdungsfehler " //
						+ "| Ggf. N und PE Leiter sind nicht richtig mit dem Netzanschluss des Wechselrichters verbunden. " //
						+ "Ggf. zu hoher Ableitstrom der PV-Module zur Erde (z.B. bei hoher Luftfeuchtigkeit). " //
						+ "Ggf. Netzerdungsverlust bzw. Wechselrichter kann keine Verbindung zu einem geerdeten Potential feststellen. " //
						+ "Installation (Netz und PV) überprüfen")), //

		STATE_11(Doc.of(Level.WARNING) //
				.text("DC Bus High " //
						+ "| Interne Betriebsspannung hoch " //
						+ "| Ggf. übersteigt die Leerlauf- oder Betriebsspannung der PV-Module den für diesen Wechselrichter zulässigen Bereich. " //
						+ "Ggf. liegt ein PV-Kriechstrom zur Erde an")), //

		STATE_12(Doc.of(Level.FAULT) //
				.text("Internal Fan Failure " //
						+ "| Der interne Lüfter meldet einen Defekt")), //

		STATE_13(Doc.of(Level.WARNING) //
				.text("Over Temperature " //
						+ "| Übertemperatur " //
						+ "| Ggf. Luft-Umgebungstemperatur ist über einen längeren Zeitraum zu hoch. "
						+ "Ggf. Luftstrom durch den Kühlkörper für Normalbetrieb unzureichend (Aufstellbedingungen beachten!). "
						+ "Ggf. Behinderung des Luftstroms, z.B. Kühlkörper wurde abgedeckt")), //

		STATE_14(Doc.of(Level.FAULT) //
				.text("Utility Phase Failure " //
						+ "| Phasenfehler " //
						+ "| Überprüfen Sie das Drehfeld am Wechselrichter. " //
						+ "Ggf. Kommunikationsadapter (ET+) nicht (richtig) gesteckt")), //

		STATE_15(Doc.of(Level.FAULT) //
				.text("PV Over Voltage " //
						+ "| Überspannung PV " //
						+ "| Bitte überprüfen Sie die Installation")), //

		STATE_16(Doc.of(Level.WARNING) //
				.text("External Fan Failure " //
						+ "| Externer Lüfter befindet sich im Fehlerzustand")), //

		STATE_17(Doc.of(Level.FAULT) //
				.text("Vac Failure " //
						+ "| Spannungsfehler " //
						+ "| Die anliegende Spannung am \"On-Grid\" Anschluss befindet sich außerhalb der gültigen Parameter (für DE siehe VDE AR N 4105). " //
						+ "Ggf. Kommunikationsmodul nicht (richtig) gesteckt")), //

		STATE_18(Doc.of(Level.FAULT) //
				.text("Isolation resistance of PV-plant too low " //
						+ "| Isolationsfehler auf PV-Strings " //
						+ "| Bitte überprüfen Sie die Installation")), //

		STATE_19(Doc.of(Level.WARNING) //
				.text("The DC injection to grid is too high " //
						+ "| DC-Strom Einspeisung auf \"On-Grid\" Seite ist zu hoch " //
						+ "| Bitte überprüfen Sie die Installation und angeschlossene Verbraucher bzw. Erzeuger")), //

		STATE_20(Doc.of(Level.FAULT) //
				.text("Back-Up Over Load " //
						+ "| Überlastung Backup-Anschluss " //
						+ "| Bitte beachten Sie die im Datenblatt angegebenen Maximal-Lasten")), //

		// Bit 21 - TBD (Currently reserved) - warning should not appear unless there is
		// a new description of GoodWe
		STATE_21(Doc.of(Level.WARNING) //
				.text("Warning Code 3")), //

		STATE_22(Doc.of(Level.FAULT) //
				.text("Difference between Master and Slave frequency too high " //
						+ "| Frequenz zwischen Master und Slave weicht zu stark ab " //
						+ "| Bitte führen Sie einen Geräteneustart aus")), //

		STATE_23(Doc.of(Level.FAULT) //
				.text("Difference between Master and Slave voltage too high " //
						+ "| Spannung zwischen Master und Slave weicht zu stark ab " //
						+ "| Bitte führen Sie einen Geräteneustart aus")), //

		// Bit 24 - TBD (Currently reserved) - warning should not appear unless there is
		// a new description of GoodWe
		STATE_24(Doc.of(Level.WARNING) //
				.text("Warning Code 4")), //

		STATE_25(Doc.of(Level.FAULT) //
				.text("Relay Check Failure " //
						+ "| Selbsttest der Relais ist Fehlgeschlagen " //
						+ "| Ggf. sind N und PE-Leiter nicht richtig mit den Anschlussklemmen des Wechselrichters verbunden. " //
						+ "Ggf. Netzerdungsverlust. " //
						+ "Bitte überprüfen Sie die Installation")), //

		// Bit 26 - TBD (Currently reserved) - warning should not appear unless there is
		// a new description of GoodWe
		STATE_26(Doc.of(Level.WARNING) //
				.text("Warning Code 5")), //

		STATE_27(Doc.of(Level.WARNING) //
				.text("Phase angle out of range (110~140°) " //
						+ "| Die Phasenverschiebung zwischen den Phasen ist außerhalb der zulässigen Parameter (110~140°) " //
						+ "| Bitte überprüfen Sie die Installation")), //

		STATE_28(Doc.of(Level.WARNING) //
				.text("Communication failure between ARM and DSP " //
						+ "| Kommunikation zwischen der ARM und DSP Einheit ist fehlgeschlagen " //
						+ "| Bitte führen Sie einen Geräteneustart aus")), //

		STATE_29(Doc.of(Level.FAULT) //
				.text("The grid frequency is out of tolerable range " //
						+ "| Die Netz-Frequenz befindet sich außerhalb der zulässigen Parameter " //
						+ "| Bitte überprüfen Sie die Installation und führen anschließend einen Geräteneustart aus")), //

		STATE_30(Doc.of(Level.FAULT) //
				.text("EEPROM cannot be read or written " //
						+ "| EEPROM kann nicht gelesen oder geschrieben werden " //
						+ "| Bitte führen Sie einen Geräteneustart aus")), //

		STATE_31(Doc.of(Level.FAULT) //
				.text("Communication failure between microcontrollers " //
						+ "| Die Kommunikation zwischen den einzelnen Microkontrollern ist fehlerhaft " //
						+ "| Bitte führen Sie einen Geräteneustart aus")), //

		// External Communication Data (ARM)
		COM_MODE(Doc.of(ComMode.values())), //
		RSSI(Doc.of(OpenemsType.INTEGER)), //
		METER_COMMUNICATE_STATUS(Doc.of(MeterCommunicateStatus.values())), //
		METER_ACTIVE_POWER_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		METER_ACTIVE_POWER_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		METER_ACTIVE_POWER_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		METER_TOTAL_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //

		METER_TOTAL_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		METER_REACTIVE_POWER_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		METER_REACTIVE_POWER_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		METER_REACTIVE_POWER_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //

		METER_APPARENT_POWER_R(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)), //
		METER_APPARENT_POWER_S(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)), //
		METER_APPARENT_POWER_T(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)), //
		METER_TOTAL_APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)), //

		METER_TYPE(Doc.of(GoodWeGridMeterType.values())), //
		METER_SOFTWARE_VERSION(Doc.of(OpenemsType.INTEGER)), //

		METER_CT2_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		CT2_E_TOTAL_SELL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		CT2_E_TOTAL_BUY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		METER_CT2_STATUS(Doc.of(OpenemsType.INTEGER)), //
		EZLOGGER_PRO_COMM_STATUS(Doc.of(EzloggerProCommStatus.values())), //

		DRM_STATUS(Doc.of(OpenemsType.INTEGER)), //

		E_TOTAL_SELL(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT_HOURS)), //
		E_TOTAL_SELL_2(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT_HOURS)), //
		E_TOTAL_BUY_F(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT_HOURS)), //

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

		BATTERY_TYPE_INDEX(Doc.of(OpenemsType.INTEGER)), //
		BMS_STATUS(Doc.of(OpenemsType.INTEGER)), //
		BMS_PACK_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		BMS_CHARGE_IMAX(Doc.of(OpenemsType.INTEGER)), //
		BMS_DISCHARGE_IMAX(Doc.of(OpenemsType.INTEGER)), //
		BMS_SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)),
		BMS_BATTERY_STRINGS(Doc.of(OpenemsType.INTEGER)), //

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
		STATE_54(Doc.of(Level.INFO).text("Communication failure")), //
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

		// Table 8-30 Grid Detailed Fault
		STATE_70(Doc.of(Level.FAULT).text("Power outage")), //
		STATE_71(Doc.of(Level.FAULT).text("Grid undervoltage first level fault")), //
		STATE_72(Doc.of(Level.FAULT).text("Grid undervoltage second level fault")), //
		STATE_73(Doc.of(Level.FAULT).text("Grid undervoltage third level fault")), //
		STATE_74(Doc.of(Level.FAULT).text("Grid overvoltage first level fault")), //
		STATE_75(Doc.of(Level.FAULT).text("Grid overvoltage second level fault")), //
		STATE_76(Doc.of(Level.FAULT).text("Grid overvoltage third level fault")), //
		STATE_77(Doc.of(Level.FAULT).text("Grid average voltage high fault")), //
		STATE_78(Doc.of(Level.FAULT).text("Grid underfrequency first level fault")), //
		STATE_79(Doc.of(Level.FAULT).text("Grid underfrequency second level fault")), //
		STATE_80(Doc.of(Level.FAULT).text("Islanding protection underfrequency fault")), //
		STATE_81(Doc.of(Level.FAULT).text("Grid overfrequency first level fault")), //
		STATE_82(Doc.of(Level.FAULT).text("Grid overfrequency second level fault")), //
		STATE_83(Doc.of(Level.FAULT).text("Islanding protection overfrequency fault")), //
		STATE_84(Doc.of(Level.FAULT).text("Grid frequency shift fault")), //
		STATE_85(Doc.of(Level.FAULT).text("Grid waveform check fault")), //
		STATE_86(Doc.of(Level.FAULT).text("Grid line voltage fault flag")), //
		STATE_87(Doc.of(Level.FAULT).text("Grid low voltage ride-through flag")), //
		STATE_88(Doc.of(Level.FAULT).text("Grid high voltage ride-through flag")), //
		STATE_89(Doc.of(Level.FAULT).text("Grid voltage exceeds the upper sampling limit")), //
		STATE_90(Doc.of(Level.FAULT).text("Grid connection voltage high")), //
		STATE_91(Doc.of(Level.FAULT).text("Grid connection voltage low")), //
		STATE_92(Doc.of(Level.FAULT).text("Grid connection frequency high")), //
		STATE_93(Doc.of(Level.FAULT).text("Grid connection frequency low")), //

		// Table 8-31 Inverter detailed error
		STATE_94(Doc.of(Level.FAULT).text("LLC hardware over current")), //
		STATE_95(Doc.of(Level.FAULT).text("Battery boost hardware over current")), //
		STATE_96(Doc.of(Level.FAULT).text("Battery boost software over current")), //
		STATE_97(Doc.of(Level.FAULT).text("Battery bms fault")), //
		STATE_98(Doc.of(Level.FAULT).text("Battery bms discharge disable")), //
		STATE_99(Doc.of(Level.FAULT).text("Battery current rms over current")), //
		STATE_100(Doc.of(Level.FAULT).text("Off-grid mode exceeds bms current limit")), //
		STATE_101(Doc.of(Level.FAULT).text("Bus voltage soft start failed")), //
		STATE_102(Doc.of(Level.FAULT).text("Bus voltage is too low")), //
		STATE_103(Doc.of(Level.FAULT).text("Bus voltage is too high")), //
		STATE_104(Doc.of(Level.FAULT).text("Inverter hardware over current")), //
		STATE_105(Doc.of(Level.FAULT).text("Inverter software over current")), //
		STATE_106(Doc.of(Level.FAULT).text("Pv boost hardware over current")), //
		STATE_107(Doc.of(Level.FAULT).text("Pv boost software over current")), //
		STATE_108(Doc.of(Level.FAULT).text("Grid back flow")), //
		STATE_109(Doc.of(Level.FAULT).text("Off-grid mode battery voltage is too low")), //
		STATE_110(Doc.of(Level.FAULT).text("Off-grid mode AC voltage is too low")), //
		STATE_111(Doc.of(Level.FAULT).text("Off-grid mode AC voltage is too high")), //
		STATE_112(Doc.of(Level.FAULT).text("Backup over load")), //
		STATE_113(Doc.of(Level.FAULT).text("Off-grid zero error")), //
		STATE_114(Doc.of(Level.FAULT).text("Power fast retrack error")), //
		STATE_115(Doc.of(Level.FAULT).text("Bypass relay switch error")), //
		STATE_116(Doc.of(Level.FAULT).text("Backup load relay switch error")), //

		// Table 8-32 Inverter detailed status
		STATE_117(Doc.of(Level.INFO).text("Over frequency curve running")), //
		STATE_118(Doc.of(Level.INFO).text("Under frequency curve running")), //
		STATE_119(Doc.of(Level.INFO).text("Frequency curve exiting recovery")), //
		STATE_120(Doc.of(Level.INFO).text("PU over voltage curve running")), //
		STATE_121(Doc.of(Level.INFO).text("PU under voltage curve running")), //
		STATE_122(Doc.of(Level.INFO).text("QU curve running")), //
		STATE_123(Doc.of(Level.INFO).text("PF curve running")), //
		STATE_124(Doc.of(Level.INFO).text("Fixed PF is set")), //
		STATE_125(Doc.of(Level.INFO).text("Fixed reactive power is set")), //
		STATE_126(Doc.of(Level.INFO).text("Inverter over temp,derating curve operation")), //
		STATE_127(Doc.of(Level.INFO).text("Australian DRED electricity sale status")), //
		STATE_128(Doc.of(Level.INFO).text("Australian DRED purchase status")), //
		STATE_129(Doc.of(Level.INFO).text("Active power limit set")), //
		STATE_130(Doc.of(Level.INFO).text("70 percent derating (Germany) has been opened")), //
		STATE_131(Doc.of(Level.INFO).text("CEI021 selftest running")), //
		STATE_132(Doc.of(Level.INFO).text("Inverter first level over voltage derate")), //
		STATE_133(Doc.of(Level.INFO).text("Force off grid flag")), //
		STATE_134(Doc.of(Level.INFO).text("Force stop mode flag")), //
		STATE_135(Doc.of(Level.INFO).text("Pv charge, off backup output flag")), //
		STATE_136(Doc.of(Level.INFO).text("QU curve over voltage flag")), //
		STATE_137(Doc.of(Level.INFO).text("QU curve under voltage flag")), //

		// BMS Information
		BATTERY_PROTOCOL(Doc.of(BatteryProtocol.values())), //
		BMS_SOFTWARE_VERSION(Doc.of(OpenemsType.INTEGER)), //
		BATTERY_HARDWARE_VERSION(Doc.of(OpenemsType.INTEGER)), //
		MAXIMUM_CELL_TEMPERATURE_ID(Doc.of(OpenemsType.INTEGER)), //
		MINIMUM_CELL_TEMPERATURE_ID(Doc.of(OpenemsType.INTEGER)), //
		MAXIMUM_CELL_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER)), //
		MINIMUM_CELL_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER)), //
		MAXIMUM_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		MINIMUM_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		MAXIMUM_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		MINIMUM_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		PASS_INFORMATION_1(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_2(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_3(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_4(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_5(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_6(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_7(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_8(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_9(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_10(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_11(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_12(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_13(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_14(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_15(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_16(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_17(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_18(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_19(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_20(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_21(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_22(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_23(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_24(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_25(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_26(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_27(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_28(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_29(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_30(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_31(Doc.of(OpenemsType.INTEGER)), //
		PASS_INFORMATION_32(Doc.of(OpenemsType.INTEGER)), //

		// For CEI Auto Test
		ERROR_MESSAGE_H(Doc.of(OpenemsType.INTEGER)), //
		ERROR_MESSAGE_L(Doc.of(OpenemsType.INTEGER)), //

		// Setting Parameter
		INVERTER_UNIT_ID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		MODBUS_BAUDRATE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		GOODWE_TYPE(Doc.of(GoodWeType.values())), //
		FACTORY_SETTING(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		CLEAR_DATA(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		ALLOW_CONNECT_TO_GRID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		FORBID_CONNECT_TO_GRID(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		RESTART(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		RESET_SPS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		E_DAY_SELL_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS) //
				.accessMode(AccessMode.READ_WRITE)), //
		E_DAY_BUY_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS) //
				.accessMode(AccessMode.READ_WRITE)), //
		E_DISCHARGE_DAY_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS) //
				.accessMode(AccessMode.READ_WRITE)), //
		SAFETY_COUNTRY_CODE(Doc.of(SafetyCountry.values())//
				.accessMode(AccessMode.READ_WRITE)), //
		ISO_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		LVRT_HVRT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		PV_START_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		MPPT_FOR_SHADOW_ENABLE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		BACK_UP_ENABLE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		AUTO_START_BACKUP(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_WAVE_CHECK_LEVEL(Doc.of(GridWaveCheckLevel.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		BACKUP_START_DLY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		UPS_STD_VOLT_TYPE(Doc.of(UpsStandartVoltType.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		DERATE_RATE_VDE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		THREE_PHASE_UNBALANCED_OUTPUT(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		HIGH_IMP_MODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		ARC_SELF_CHECK(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		ARC_FAULT_REMOVE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		ISO_CHECK_MODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OFF_GRID_TO_ON_GRID_DELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		OFF_GRID_UNDER_VOLTAGE_PROTECT_COEFFICIENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		BATTERY_MODE_PV_CHARGE_ENABLE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		DCV_CHECK_OFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		DEVICE_LICENCE(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		REMOTE_USER_LICENCE(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_WRITE)), //
		BATTERY_STRINGS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		CPLD_WARNING_CODE(Doc.of(CpldWarningCode.values())), //

		// DIAGNOSIS STATUS HIGH
		DIAG_STATUS_BATTERY_PRECHARGE_RELAY_OFF(Doc.of(OpenemsType.BOOLEAN) //
				.text("Battery Precharge Relay Off")), //

		DIAG_STATUS_BYPASS_RELAY_STICK(Doc.of(OpenemsType.BOOLEAN) //
				.text("Bypass relay is sticking")), //

		DIAG_STATUS_METER_VOLTAGE_SAMPLE_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.text("Meter voltage sample fault")), //

		DIAG_STATUS_EXTERNAL_STOP_MODE_ENABLE(Doc.of(OpenemsType.BOOLEAN) //
				.text("DRED or ESD stop the inverter")), //

		DIAG_STATUS_BATTERY_OFFGRID_DOD(Doc.of(OpenemsType.BOOLEAN) //
				.text("Battery SOC less than Offgrid DOD")), //

		DIAG_STATUS_BATTERY_SOC_ADJUST_ENABLE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Only for BYD, adjust the SOC")), //
		/*
		 * DIAGNOSIS STATUS LOW
		 */
		DIAG_STATUS_BATTERY_VOLT_LOW(Doc.of(OpenemsType.BOOLEAN) //
				.text("Battery not discharge caused by low battery voltage")), //

		DIAG_STATUS_BATTERY_SOC_LOW(Doc.of(OpenemsType.BOOLEAN) //
				.text("Battery not discharge caused by low SOC")), //

		DIAG_STATUS_BATTERY_SOC_IN_BACK(Doc.of(OpenemsType.BOOLEAN) //
				.text("Battery SOC not recover to allow-discharge level")), //

		DIAG_STATUS_BMS_DISCHARGE_DISABLE(Doc.of(OpenemsType.BOOLEAN) //
				.text("BMS not allow discharge")), //

		DIAG_STATUS_DISCHARGE_TIME_ON(Doc.of(OpenemsType.BOOLEAN) //
				.text("Discharge time is set, 1: On, 0: OFF")), //

		DIAG_STATUS_CHARGE_TIME_ON(Doc.of(OpenemsType.BOOLEAN) //
				.text("Charge time is set, 1: On, 0: OFF")), //

		DIAG_STATUS_DISCHARGE_DRIVE_ON(Doc.of(OpenemsType.BOOLEAN) //
				.text("Discharge driver is turned on")), //

		DIAG_STATUS_BMS_DISCHG_CURRENT_LOW(Doc.of(OpenemsType.BOOLEAN) //
				.text("BMS discharge current limit is too low")), //

		DIAG_STATUS_DISCHARGE_CURRENT_LOW(Doc.of(OpenemsType.BOOLEAN) //
				.text("Discharge current limit is too low (from App)")), //

		DIAG_STATUS_METER_COMM_LOSS(Doc.of(OpenemsType.BOOLEAN) //
				.text("Smart Meter communication failure")), //

		DIAG_STATUS_METER_CONNECT_REVERSE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Smart Meter connection reversed")), //

		DIAG_STATUS_SELF_USE_LOAD_LIGHT(Doc.of(OpenemsType.BOOLEAN) //
				.text("Low load power, cannot activate battery discharge")), //

		DIAG_STATUS_EMS_DISCHARGE_IZERO(Doc.of(OpenemsType.BOOLEAN) //
				.text("Discharge current limit 0A from EMS")), //

		DIAG_STATUS_DISCHARGE_BUS_HIGH(Doc.of(OpenemsType.BOOLEAN) //
				.text("Battery not discharge caused by over high PV voltage")), //

		DIAG_STATUS_BATTERY_DISCONNECT(Doc.of(OpenemsType.BOOLEAN) //
				.text("Battery disconnected")), //

		DIAG_STATUS_BATTERY_OVERCHARGE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Battery overcharged")), //

		DIAG_STATUS_BMS_OVER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Lithium battery over temperature")), //

		DIAG_STATUS_BMS_OVERCHARGE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Lithium battery overcharged or an individual cell voltage is higher")), //

		DIAG_STATUS_BMS_CHARGE_DISABLE(Doc.of(OpenemsType.BOOLEAN) //
				.text("BMS does not allow charge")), //

		DIAG_STATUS_SELF_USE_OFF(Doc.of(OpenemsType.BOOLEAN) //
				.text("Self-use mode turned off")), //

		DIAG_STATUS_SOC_DELTA_OVER_RANGE(Doc.of(OpenemsType.BOOLEAN) //
				.text("SOC Jumps abnormally")), //

		DIAG_STATUS_BATTERY_SELF_DISCHARGE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Battery discharge at low current for long time, continuously over 30% of battery SOC")), //

		DIAG_STATUS_OFFGRID_SOC_LOW(Doc.of(OpenemsType.BOOLEAN) //
				.text("SOC is low under off-grid statues")), //

		DIAG_STATUS_GRID_WAVE_UNSTABLE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Grid wave is bad, switch to back-up mode frequently")), //

		DIAG_STATUS_FEED_POWER_LIMIT(Doc.of(OpenemsType.BOOLEAN) //
				.text("Export power limit is set")), //

		DIAG_STATUS_PF_VALUE_SET(Doc.of(OpenemsType.BOOLEAN) //
				.text("PF value is set")), //

		DIAG_STATUS_REAL_POWER_LIMIT(Doc.of(OpenemsType.BOOLEAN) //
				.text("Active power value is set")), //

		DIAG_STATUS_SOC_PROTECT_OFF(Doc.of(OpenemsType.BOOLEAN) //
				.text("SOC protect Off")), //

		EH_BATTERY_FUNCTION_ACTIVE(Doc.of(EhBatteryFunctionActive.values())), //
		ARC_SELF_CHECK_STATUS(Doc.of(ArcSelfCheckStatus.values())), //

		MAX_GRID_FREQ_WITHIN_1_MINUTE(Doc.of(OpenemsType.INTEGER)), //
		MIN_GRID_FREQ_WITHIN_1_MINUTE(Doc.of(OpenemsType.INTEGER)), //
		MAX_GRID_VOLTAGE_WITHIN_1_MINUTE_R(Doc.of(OpenemsType.INTEGER)), //
		MIN_GRID_VOLTAGE_WITHIN_1_MINUTE_R(Doc.of(OpenemsType.INTEGER)), //
		MAX_GRID_VOLTAGE_WITHIN_1_MINUTE_S(Doc.of(OpenemsType.INTEGER)), //
		MIN_GRID_VOLTAGE_WITHIN_1_MINUTE_S(Doc.of(OpenemsType.INTEGER)), //
		MAX_GRID_VOLTAGE_WITHIN_1_MINUTE_T(Doc.of(OpenemsType.INTEGER)), //
		MIN_GRID_VOLTAGE_WITHIN_1_MINUTE_T(Doc.of(OpenemsType.INTEGER)), //
		MAX_BACKUP_POWER_WITHIN_1_MINUTE_R(Doc.of(OpenemsType.LONG)), //
		MAX_BACKUP_POWER_WITHIN_1_MINUTE_S(Doc.of(OpenemsType.LONG)), //
		MAX_BACKUP_POWER_WITHIN_1_MINUTE_T(Doc.of(OpenemsType.LONG)), //
		MAX_BACKUP_POWER_WITHIN_1_MINUTE_TOTAL(Doc.of(OpenemsType.LONG)), //
		GRID_HVRT_EVENT_TIMES(Doc.of(OpenemsType.INTEGER)), //
		GRID_LVRT_EVENT_TIMES(Doc.of(OpenemsType.INTEGER)), //
		INV_ERROR_MSG_RECORD_FOR_EMS(Doc.of(OpenemsType.LONG)), //
		INV_WARNING_CODE_RECORD_FOR_EMS(Doc.of(OpenemsType.LONG)), //
		INV_CPLD_WARNING_RECORD_FOR_EMS(Doc.of(OpenemsType.LONG)), //

		// BMS
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
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)),
		BMS_OFFLINE_SOC_UNDER_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)),
		CLEAR_BATTERY_SETTING(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)),

		// Safety
		GRID_VOLT_HIGH_S1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_HIGH_S1_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_LOW_S1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_LOW_S1_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_HIGH_S2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_HIGH_S2_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_LOW_S2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_LOW_S2_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_QUALITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_HIGH_S1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_HIGH_S1_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_LOW_S1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_LOW_S1_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_HIGH_S2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_HIGH_S2_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_LOW_S2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_LOW_S2_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_HIGH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_LOW(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_HIGH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_LOW(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_RECOVER_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //

		// Decide by specific safety regulations
		GRID_VOLT_RECOVER_HIGH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_RECOVER_LOW(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_RECOVER_HIGH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_RECOVER_LOW(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_RECOVER_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_FREQ_RECOVER_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //
		POWER_RATE_LIMIT_GENERATE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		POWER_RATE_LIMIT_RECONNECT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		POWER_RATE_LIMIT_REDUCTION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_PROTECT(Doc.of(GridProtect.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		POWER_SLOPE_ENABLE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		// CosPhi curve
		ENABLE_CURVE_PU(Doc.of(EnableCurve.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		A_POINT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		A_POINT_COS_PHI(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)), //
		B_POINT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		B_POINT_COS_PHI(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)), //
		C_POINT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		C_POINT_COS_PHI(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)), //
		LOCK_IN_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		LOCK_OUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		LOCK_OUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //

		// Power and frequency curve
		POWER_FREQUENCY_ENABLED(Doc.of(OpenemsType.BOOLEAN) //
				.text("Power and Frequency Curve Enabled")), //
		POWER_FREQUENCY_RESPONSE_MODE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Power and Frequency Curve: 0=Slope, 1=Fstop")), //

		FFROZEN_DCH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ) //
				.accessMode(AccessMode.READ_WRITE)), //
		FFROZEN_CH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ) //
				.accessMode(AccessMode.READ_WRITE)), //
		FSTOP_DCH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ) //
				.accessMode(AccessMode.READ_WRITE)), //
		FSTOP_CH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ) //
				.accessMode(AccessMode.READ_WRITE)), //
		OF_RECOVERY_WAITING_TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		RECOVERY_WAITING_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		RECOVERY_FREQURNCY1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ) //
				.accessMode(AccessMode.READ_WRITE)), //
		CFP_SETTINGS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		RECOVERY_FREQUENCY2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ) //
				.accessMode(AccessMode.READ_WRITE)), //
		OF_RECOVERY_SLOPE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		CFP_OF_SLOPE_PERCENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ) //
				.accessMode(AccessMode.READ_WRITE)), //
		CFP_UF_SLOPE_PERCENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ) //
				.accessMode(AccessMode.READ_WRITE)), //
		CFP_OF_RECOVER_POWER_PERCENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		// QU curve
		QU_CURVE(Doc.of(EnableCurve.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		LOCK_IN_POWER_QU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		LOCK_OUT_POWER_QU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		V1_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		V1_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		V2_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		V2_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		V3_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		V3_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		V4_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		V4_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		K_VALUE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		TIME_CONSTANT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		MISCELLANEA(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		// PU curve
		PU_CURVE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		POWER_CHANGE_RATE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		V1_VOLTAGE_PU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		V1_VALUE_PU(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		V2_VOLTAGE_PU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		V2_VALUE_PU(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		V3_VOLTAGE_PU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		V3_VALUE_PU(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		V4_VOLTAGE_PU(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		V4_VALUE_PU(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		FIXED_POWER_FACTOR(Doc.of(FixedPowerFactor.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		FIXED_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		FIXED_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_LIMIT_BY_VOLT_SLOPE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		AUTO_TEST_ENABLE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		AUTO_TEST_STEP(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		UW_ITALY_FREQ_MODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		ALL_POWER_CURVE_DISABLE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		R_PHASE_FIXED_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.THOUSANDTH) //
				.accessMode(AccessMode.READ_WRITE)), //
		S_PHASE_FIXED_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.THOUSANDTH) //
				.accessMode(AccessMode.READ_WRITE)), //
		T_PHASE_FIXED_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.THOUSANDTH) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_HIGH_S3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_HIGH_S3_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_LOW_S3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_VOLT_LOW_S3_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //
		ZVRT_CONFIG(Doc.of(ZvrtConfig.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		LVRT_START_VOLT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		LVRT_END_VOLT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		LVRT_START_TRIP_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //
		LVRT_END_TRIP_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //
		LVRT_TRIP_LIMIT_VOLT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		HVRT_START_VOLT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		HVRT_END_VOLT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		HVRT_START_TRIP_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //
		HVRT_END_TRIP_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //
		HVRT_TRIP_LIMIT_VOLT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //

		PF_TIME_CONSTANT(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		POWER_FREQ_TIME_CONSTANT(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		PU_TIME_CONSTANT(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		D_POINT_POWER(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		D_POINT_COS_PHI(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		UF_RECOVERY_WAITING_TIME(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		UF_RECOVER_SLOPE(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		CFP_UF_RECOVER_POWER_PERCENT(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		POWER_CHARGE_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		POWER_CHARGE_LIMIT_RECONNECT(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		C_EXT_UF_CHARGE_STOP(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		C_EXT_OF_DISCHARGE_STOP(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		C_EXT_TWOSSTEPF_FLG(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //

		// Meter Control ARM
		SELECT_WORK_MODE(Doc.of(AppModeIndex.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		WMETER_CONNECT_CHECK_FLAG(Doc.of(MeterConnectCheckFlag.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		LOG_DATA_ENABLE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		DATA_SEND_INTERVAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //
		DRED_CMD(Doc.of(DredCmd.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
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
		METER_CT1_REVERSE_ENABLE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		ERROR_LOG_READ_PAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		MODBUS_TCP_WITHOUT_INTERNET(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		BACKUP_LED(Doc.of(LedState.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_LED(Doc.of(LedState.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		SOC_LED_1(Doc.of(LedState.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		SOC_LED_2(Doc.of(LedState.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		SOC_LED_3(Doc.of(LedState.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		SOC_LED_4(Doc.of(LedState.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		BATTERY_LED(Doc.of(LedState.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		SYSTEM_LED(Doc.of(LedState.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		FAULT_LED(Doc.of(LedState.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		ENERGY_LED(Doc.of(LedState.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		LED_EXTERNAL_CONTROL(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		STOP_MODE_SAVE_ENABLE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		// Battery Control Data ARM
		STOP_SOC_PROTECT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		BMS_FLOAT_VOLT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		BMS_FLOAT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE)), //
		BMS_FLOAT_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MINUTE) //
				.accessMode(AccessMode.READ_WRITE)), //
		BMS_TYPE_INDEX_ARM(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		MANUFACTURE_CODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		DC_VOLT_OUTPUT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		BMS_AVG_CHG_VOLT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		BMS_AVG_CHG_HOURS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HOUR) //
				.accessMode(AccessMode.READ_WRITE)), //
		FEED_POWER_ENABLE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		FEED_POWER_PARA_SET(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)), //
		RIPPLE_CONTROL_RECEIVER_ENABLE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //

		DEBUG_EMS_POWER_MODE(Doc.of(EmsPowerMode.values())), //
		DEBUG_EMS_POWER_SET(Doc.of(OpenemsType.INTEGER)), //
		EMS_POWER_MODE(Doc.of(EmsPowerMode.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_EMS_POWER_MODE)), //
		EMS_POWER_SET(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_EMS_POWER_SET)), //

		BMS_CURR_LMT_COFF(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		BATTERY_PROTOCOL_ARM(Doc.of(BatteryProtocol.values()) //
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
		WORK_WEEK_1(Doc.of(WorkWeek.values()) //
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
		WORK_WEEK_2(Doc.of(WorkWeek.values()) //
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
		WORK_WEEK_3(Doc.of(WorkWeek.values()) //
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
		WORK_WEEK_4(Doc.of(WorkWeek.values()) //
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
		WIFI_RESET(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		ARM_SOFT_RESET(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		WIFI_RELOAD(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		PEAK_SHAVING_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		PEAK_SHAVING_SOC(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		FAST_CHARGE_ENABLE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		FAST_CHARGE_STOP_SOC(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		WORK_WEEK_1_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_1_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_1_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_1_PARAMETER1_1_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_1_PARAMETER1_2_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_1_PARAMETER1_3_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		WORK_WEEK_2_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_2_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_2_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_2_PARAMETER2_1_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_2_PARAMETER2_2_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_2_PARAMETER2_3_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		WORK_WEEK_3_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_3_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_3_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_3_PARAMETER3_1_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_3_PARAMETER3_2_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_3_PARAMETER3_3_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		WORK_WEEK_4_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_4_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_4_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_4_PARAMETER4_1_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_4_PARAMETER4_2_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_4_PARAMETER4_3_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		WORK_WEEK_5_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_5_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_5_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_5_PARAMETER5_1_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_5_PARAMETER5_2_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_5_PARAMETER5_3_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		WORK_WEEK_6_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_6_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_6_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_6_PARAMETER6_1_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_6_PARAMETER6_2_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_6_PARAMETER6_3_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		WORK_WEEK_7_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_7_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_7_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_7_PARAMETER7_1_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_7_PARAMETER7_2_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_7_PARAMETER7_3_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		WORK_WEEK_8_START_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_8_END_TIME_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_8_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_8_PARAMETER8_1_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_8_PARAMETER8_2_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WORK_WEEK_8_PARAMETER8_3_ECO_MODE_FOR_ARM_18_AND_GREATER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		LOAD_REGULATION_INDEX(Doc.of(LoadRegulationIndex.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		LOAD_SWITCH_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		BACKUP_SWITCH_SOC_MIN(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		HARDWARE_FEED_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

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

		/**
		 * Warning Codes (table 8-8).
		 *
		 * <ul>
		 * <li>Bit 12-31 Reserved
		 * <li>Bit 11: System High Temperature
		 * <li>Bit 10: System Low Temperature 2
		 * <li>Bit 09: System Low Temperature 1
		 * <li>Bit 08: Cell Imbalance
		 * <li>Bit 07: System Reboot
		 * <li>Bit 06: Communication Failure
		 * <li>Bit 05: Discharge Over-Current
		 * <li>Bit 04: Charge Over-Current
		 * <li>Bit 03: Cell Low Temperature
		 * <li>Bit 02: Cell High Temperature
		 * <li>Bit 01: Discharge Under-Voltage
		 * <li>Bit 00: Charge Over-Voltage
		 * </ul>
		 */
		// TODO: Into enum
		WBMS_WARNING_CODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		/**
		 * Alarm Codes (table 8-7).
		 *
		 * <ul>
		 * <li>Bit 16-31 Reserved
		 * <li>Bit 15: Charge Over-Voltage Fault
		 * <li>Bit 14: Discharge Under-Voltage Fault
		 * <li>Bit 13: Cell High Temperature
		 * <li>Bit 12: Communication Fault
		 * <li>Bit 11: Charge Circuit Fault
		 * <li>Bit 10: Discharge Circuit Fault
		 * <li>Bit 09: Battery Lock
		 * <li>Bit 08: Battery Break
		 * <li>Bit 07: DC Bus Fault
		 * <li>Bit 06: Precharge Fault
		 * <li>Bit 05: Discharge Over-Current
		 * <li>Bit 04: Charge Over-Current
		 * <li>Bit 03: Cell Low Temperature
		 * <li>Bit 02: Cell High Temperature
		 * <li>Bit 01: Discharge Under-Voltage
		 * <li>Bit 00: Charge Over-Voltage
		 * </ul>
		 */
		// TODO: Into enum
		WBMS_ALARM_CODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		/**
		 * BMS Status.
		 *
		 * <ul>
		 * <li>Bit 2: Stop Discharge
		 * <li>Bit 1: Stop Charge
		 * <li>Bit 0: Force Charge
		 * </ul>
		 */
		// TODO: Into enum
		WBMS_STATUS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		WBMS_DISABLE_TIMEOUT_DETECTION(Doc.of(OpenemsType.INTEGER) //
				.text("Cancel EMS mode BMS communication timeout detection") //
				.accessMode(AccessMode.READ_WRITE)), //
		MAX_AC_EXPORT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		MAX_AC_IMPORT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		SMART_MODE_NOT_WORKING_WITH_PID_FILTER(Doc.of(Level.WARNING) //
				.text("SMART mode does not work correctly with active PID filter")),
		NO_SMART_METER_DETECTED(Doc.of(Level.WARNING) //
				.text("No GoodWe Smart Meter detected. Only REMOTE mode can work correctly")),
		IMPOSSIBLE_FENECON_HOME_COMBINATION(Doc.of(Level.FAULT) //
				.text("The installed inverter and battery combination is not authorised. Operation could cause hardware damages, so charging and discharging is blocked. Please install a complete Home 10, Home 20 or Home 30 system.")) //
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
	public default Channel<GoodWeType> getGoodweTypeChannel() {
		return this.channel(GoodWe.ChannelId.GOODWE_TYPE);
	}

	/**
	 * Gets the Device Type. See {@link ChannelId#GOODWE_TYPE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default GoodWeType getGoodweType() {
		return this.getGoodweTypeChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#GOODWE_TYPE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setGoodweType(GoodWeType value) {
		this.getGoodweTypeChannel().setNextValue(value);
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

	/**
	 * Gets the Channel for {@link ChannelId#WBMS_CHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getWbmsChargeMaxCurrentChannel() {
		return this.channel(ChannelId.WBMS_CHARGE_MAX_CURRENT);
	}

	/**
	 * Gets the Wbms Charge Max Current in [A]. See
	 * {@link ChannelId#WBMS_CHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getWbmsChargeMaxCurrent() {
		return this.getWbmsChargeMaxCurrentChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#WBMS_DISCHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getWbmsDischargeMaxCurrentChannel() {
		return this.channel(ChannelId.WBMS_DISCHARGE_MAX_CURRENT);
	}

	/**
	 * Gets the Wbms Discharge Max Current in [A]. See
	 * {@link ChannelId#WBMS_DISCHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getWbmsDischargeMaxCurrent() {
		return this.getWbmsDischargeMaxCurrentChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#WBMS_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getWbmsVoltageChannel() {
		return this.channel(ChannelId.WBMS_VOLTAGE);
	}

	/**
	 * Gets the Wbms voltage in [V]. See {@link ChannelId#WBMS_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getWbmsVoltage() {
		return this.getWbmsVoltageChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_AC_EXPORT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxAcExportChannel() {
		return this.channel(ChannelId.MAX_AC_EXPORT);
	}

	/**
	 * Gets the Max AC-Export Power in [W]. Positive Values. See
	 * {@link ChannelId#MAX_AC_EXPORT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxAcExport() {
		return this.getMaxAcExportChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_AC_EXPORT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxAcExport(Integer value) {
		this.getMaxAcExportChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_AC_IMPORT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxAcImportChannel() {
		return this.channel(ChannelId.MAX_AC_IMPORT);
	}

	/**
	 * Gets the Max AC-Import Power in [W]. Negative Values. See
	 * {@link ChannelId#MAX_AC_IMPORT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxAcImport() {
		return this.getMaxAcImportChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_AC_IMPORT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxAcImport(Integer value) {
		this.getMaxAcImportChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_PROTOCOL_ARM}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getBatteryProtocolArmChannel() {
		return this.channel(ChannelId.BATTERY_PROTOCOL_ARM);
	}

	/**
	 * Gets the battery protocol arm as enum. See
	 * {@link ChannelId#BATTERY_PROTOCOL_ARM}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default BatteryProtocol getBatteryProtocolArm() {
		return this.getBatteryProtocolArmChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#SOC_START_TO_FORCE_CHARGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getSocStartToForceChargeChannel() {
		return this.channel(ChannelId.SOC_START_TO_FORCE_CHARGE);
	}

	/**
	 * Gets the SoC to start the force charge [%]. See
	 * {@link ChannelId#SOC_START_TO_FORCE_CHARGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSocStartToForceCharge() {
		return this.getSocStartToForceChargeChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#SOC_STOP_TO_FORCE_CHARGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getSocStopToForceChargeChannel() {
		return this.channel(ChannelId.SOC_STOP_TO_FORCE_CHARGE);
	}

	/**
	 * Gets the SoC to stop the force charge [%]. See
	 * {@link ChannelId#SOC_STOP_TO_FORCE_CHARGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSocStopToForceCharge() {
		return this.getSocStopToForceChargeChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#IMPOSSIBLE_FENECON_HOME_COMBINATION}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getImpossibleFeneconHomeCombinationChannel() {
		return this.channel(ChannelId.IMPOSSIBLE_FENECON_HOME_COMBINATION);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#IMPOSSIBLE_FENECON_HOME_COMBINATION} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setImpossibleFeneconHomeCombination(boolean value) {
		this.getImpossibleFeneconHomeCombinationChannel().setNextValue(value);
	}
}
