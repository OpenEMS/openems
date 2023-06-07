package io.openems.backend.timedata.influx;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.InfluxException;

/**
 * Handles Influx FieldTypeConflictExceptions. This helper provides conversion
 * functions to provide the correct field types for InfluxDB.
 */
public class FieldTypeConflictHandler {

	private static final Pattern FIELD_TYPE_CONFLICT_EXCEPTION_PATTERN = Pattern.compile(
			"^.*partial write: field type conflict: input field \"(?<channel>.*)\" on measurement \"data\" is type (?<thisType>\\w+), already exists as type (?<requiredType>\\w+) dropped=\\d+$");

	private final Logger log = LoggerFactory.getLogger(FieldTypeConflictHandler.class);
	private final TimedataInfluxDb parent;
	private final ConcurrentHashMap<String, BiConsumer<Point, JsonElement>> specialCaseFieldHandlers = new ConcurrentHashMap<>();

	public FieldTypeConflictHandler(TimedataInfluxDb parent) {
		this.parent = parent;
		this.initializePredefinedHandlers();
	}

	/**
	 * Add some already known Handlers.
	 */
	private void initializePredefinedHandlers() {
		this.createAndAddHandler("01/_PropertyId", RequiredType.STRING);
		this.createAndAddHandler("01/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("01/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("01/_PropertyThreshold", RequiredType.STRING);
		this.createAndAddHandler("Bielmeier/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("Bielmeier/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("KACO Blueplanet 5.0/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("KACO Blueplanet 5.0/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("PV-Huawei/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("PV-Huawei/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("_component0/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("_component0/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("_component0/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("_component0/_PropertyThreshold", RequiredType.STRING);
		this.createAndAddHandler("_cycle/_PropertyCycleTime", RequiredType.INTEGER);
		this.createAndAddHandler("activepowerfrequencycharacteristic0/DebugSetTargetGridSetpoint", RequiredType.FLOAT);
		this.createAndAddHandler("airCon0/_PropertyModbusUnitId", RequiredType.INTEGER);
		this.createAndAddHandler("battery0/MasterMcuFirmwareVersion", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower0Module0SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower0Module1SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower0Module2SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower0Module3SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower0Module4SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower0Module5SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower0Module6SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower0Module7SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower0Module8SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower0Module9SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower0SubMasterFirmwareVersion", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower1BmsSerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower1Module0SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower1Module1SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower1Module2SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower1Module3SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower1Module4SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower1Module5SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower1Module6SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower1Module7SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower1Module8SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower1Module9SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("battery0/Tower1SubMasterFirmwareVersion", RequiredType.STRING);
		this.createAndAddHandler("battery1/BatterySoc", RequiredType.INTEGER);
		this.createAndAddHandler("battery1/MasterMcuFirmwareVersion", RequiredType.STRING);
		this.createAndAddHandler("battery1/Tower0SubMasterFirmwareVersion", RequiredType.STRING);
		this.createAndAddHandler("battery1/Tower1SubMasterFirmwareVersion", RequiredType.STRING);
		this.createAndAddHandler("battery11/MasterMcuFirmwareVersion", RequiredType.STRING);
		this.createAndAddHandler("battery12/MasterMcuFirmwareVersion", RequiredType.STRING);
		this.createAndAddHandler("battery2/BatterySoc", RequiredType.INTEGER);
		this.createAndAddHandler("battery2/MasterMcuFirmwareVersion", RequiredType.STRING);
		this.createAndAddHandler("battery2/Tower0SubMasterFirmwareVersion", RequiredType.STRING);
		this.createAndAddHandler("battery3/MasterMcuFirmwareVersion", RequiredType.STRING);
		this.createAndAddHandler("battery3/Tower0SubMasterFirmwareVersion", RequiredType.STRING);
		this.createAndAddHandler("battery4/MasterMcuFirmwareVersion", RequiredType.STRING);
		this.createAndAddHandler("battery5/MasterMcuFirmwareVersion", RequiredType.STRING);
		this.createAndAddHandler("battery6/MasterMcuFirmwareVersion", RequiredType.STRING);
		this.createAndAddHandler("battery7/MasterMcuFirmwareVersion", RequiredType.STRING);
		this.createAndAddHandler("battery8/MasterMcuFirmwareVersion", RequiredType.STRING);
		this.createAndAddHandler("battery9/MasterMcuFirmwareVersion", RequiredType.STRING);
		this.createAndAddHandler("batteryA11/AsamOdxFileVersion", RequiredType.STRING);
		this.createAndAddHandler("batteryA11/BatterymanagerSoftwareversion", RequiredType.STRING);
		this.createAndAddHandler("batteryA12/AsamOdxFileVersion", RequiredType.STRING);
		this.createAndAddHandler("batteryA12/BatterymanagerSoftwareversion", RequiredType.STRING);
		this.createAndAddHandler("batteryA12/VwEcuHardwareNumber", RequiredType.STRING);
		this.createAndAddHandler("batteryA12/VwEcuHardwareVersionNumber", RequiredType.STRING);
		this.createAndAddHandler("batteryA21/AsamOdxFileVersion", RequiredType.STRING);
		this.createAndAddHandler("batteryA21/BatterymanagerSoftwareversion", RequiredType.STRING);
		this.createAndAddHandler("batteryA21/VwEcuHardwareNumber", RequiredType.STRING);
		this.createAndAddHandler("batteryA21/VwEcuHardwareVersionNumber", RequiredType.STRING);
		this.createAndAddHandler("batteryA22/AsamOdxFileVersion", RequiredType.STRING);
		this.createAndAddHandler("batteryA22/BatterymanagerSoftwareversion", RequiredType.STRING);
		this.createAndAddHandler("batteryA22/VwEcuHardwareVersionNumber", RequiredType.STRING);
		this.createAndAddHandler("batteryA31/AsamOdxFileVersion", RequiredType.STRING);
		this.createAndAddHandler("batteryA31/BatterymanagerSoftwareversion", RequiredType.STRING);
		this.createAndAddHandler("batteryA31/VwEcuHardwareNumber", RequiredType.STRING);
		this.createAndAddHandler("batteryA31/VwEcuHardwareVersionNumber", RequiredType.STRING);
		this.createAndAddHandler("batteryA32/AsamOdxFileVersion", RequiredType.INTEGER);
		this.createAndAddHandler("batteryA32/BatterymanagerSoftwareversion", RequiredType.STRING);
		this.createAndAddHandler("batteryA32/VwEcuHardwareNumber", RequiredType.STRING);
		this.createAndAddHandler("batteryA32/VwEcuHardwareVersionNumber", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter0/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter0/ETotalBuyF", RequiredType.FLOAT);
		this.createAndAddHandler("batteryInverter0/ETotalSell", RequiredType.FLOAT);
		this.createAndAddHandler("batteryInverter0/ETotalSell2", RequiredType.FLOAT);
		this.createAndAddHandler("batteryInverter0/Mac", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter0/Opt", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter0/S103Wh", RequiredType.FLOAT);
		this.createAndAddHandler("batteryInverter0/Sn", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter0/Vr", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter1/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter1/Opt", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter1/S103Wh", RequiredType.FLOAT);
		this.createAndAddHandler("batteryInverter1/Sn", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter1/Vr", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter1/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter2/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter2/Opt", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter2/S103Wh", RequiredType.FLOAT);
		this.createAndAddHandler("batteryInverter2/Sn", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter2/Vr", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter2/_PropertyModbusUnitId", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter3/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter3/Opt", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter3/S103Wh", RequiredType.FLOAT);
		this.createAndAddHandler("batteryInverter3/Sn", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter3/Vr", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter3/_PropertyModbusUnitId", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter4/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter4/Opt", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter4/S103Wh", RequiredType.FLOAT);
		this.createAndAddHandler("batteryInverter4/Sn", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter4/Vr", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter5/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter5/Opt", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter5/S103Wh", RequiredType.FLOAT);
		this.createAndAddHandler("batteryInverter5/Sn", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter5/Vr", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter6/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter6/Opt", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter6/Sn", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter6/Vr", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter7/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter7/Opt", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter7/S103Wh", RequiredType.FLOAT);
		this.createAndAddHandler("batteryInverter7/Sn", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter7/Vr", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter8/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter8/Opt", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter8/S103Wh", RequiredType.FLOAT);
		this.createAndAddHandler("batteryInverter8/Sn", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter8/Vr", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter9/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter9/Opt", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter9/Sn", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter9/Vr", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterA1/Dca", RequiredType.FLOAT);
		this.createAndAddHandler("batteryInverterA1/Opt", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterA1/Sn", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterA1/Vr", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterA1/_PropertyActivateWatchdog", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterA1/_PropertyModbusUnitId", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterA1/_PropertyTimeLimitNoPower", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterA1/_PropertyWatchdoginterval", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterA2/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterA2/Opt", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterA2/Sn", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterA2/Vr", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterA2/_PropertyActivateWatchdog", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterA2/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterA2/_PropertyTimeLimitNoPower", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterA2/_PropertyWatchdoginterval", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterA3/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterA3/Opt", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterA3/Sn", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterA3/Vr", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterA3/_PropertyActivateWatchdog", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterA3/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterA4/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterA4/Opt", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterA4/Sn", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterA4/Vr", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterA4/_PropertyActivateWatchdog", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterA4/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterB1/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterB1/Opt", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterB1/Sn", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterB1/Vr", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterB1/_PropertyActivateWatchdog", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterB1/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterB1/_PropertyTimeLimitNoPower", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterB1/_PropertyWatchdoginterval", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterB2/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterB2/Opt", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterB2/Sn", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterB2/Vr", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterB2/_PropertyActivateWatchdog", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterB2/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterB2/_PropertyTimeLimitNoPower", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterB2/_PropertyWatchdoginterval", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterB3/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterB3/Opt", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterB3/Sn", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterB3/Vr", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterB3/_PropertyActivateWatchdog", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterB3/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterB4/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterB4/Opt", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterB4/Sn", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterB4/Vr", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterB4/_PropertyActivateWatchdog", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterB4/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("batteryInverterMasterA1/S103Wh", RequiredType.FLOAT);
		this.createAndAddHandler("batteryInverterMasterB1/S103Wh", RequiredType.FLOAT);
		this.createAndAddHandler("batteryInverterSlaveA1/S103Wh", RequiredType.FLOAT);
		this.createAndAddHandler("batteryInverterSlaveB1/S103Wh", RequiredType.FLOAT);
		this.createAndAddHandler("bms0/_PropertySource", RequiredType.STRING);
		this.createAndAddHandler("bms0/_PropertyWatchdog", RequiredType.STRING);
		this.createAndAddHandler("bms1/_PropertyMaxAllowedCurrentDifference", RequiredType.INTEGER);
		this.createAndAddHandler("bms1/_PropertyMaxAllowedVoltageDifference", RequiredType.INTEGER);
		this.createAndAddHandler("bms3/_PropertyMaxAllowedVoltageDifference", RequiredType.INTEGER);
		this.createAndAddHandler("bms4/_PropertyMaxAllowedVoltageDifference", RequiredType.INTEGER);
		this.createAndAddHandler("bms5/_PropertyMaxAllowedVoltageDifference", RequiredType.INTEGER);
		this.createAndAddHandler("bms6/_PropertyMaxAllowedVoltageDifference", RequiredType.INTEGER);
		this.createAndAddHandler("bms7/_PropertyMaxAllowedCurrentDifference", RequiredType.INTEGER);
		this.createAndAddHandler("bms7/_PropertyMaxAllowedVoltageDifference", RequiredType.INTEGER);
		this.createAndAddHandler("bms9/_PropertyMaxAllowedVoltageDifference", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA1/_PropertyErrorDelay", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA1/_PropertyMaxAllowedCellSocPct", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA1/_PropertyMaxStartAttempts", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA1/_PropertyMaxStartTime", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA1/_PropertyMinAllowedCellSocPct", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA1/_PropertyModbusUnitId", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA1/_PropertyPendingTolerance", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA1/_PropertyStartUnsuccessfulDelay", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA2/_PropertyErrorDelay", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA2/_PropertyMaxStartAttempts", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA2/_PropertyMaxStartTime", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA2/_PropertyModbusUnitId", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA2/_PropertyPendingTolerance", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA2/_PropertyStartUnsuccessfulDelay", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA3/_PropertyErrorDelay", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA3/_PropertyMaxStartAttempts", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA3/_PropertyMaxStartTime", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA3/_PropertyModbusUnitId", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA3/_PropertyPendingTolerance", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA3/_PropertyStartUnsuccessfulDelay", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA4/_PropertyErrorDelay", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA4/_PropertyMaxStartAttempts", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA4/_PropertyMaxStartTime", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA4/_PropertyModbusUnitId", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA4/_PropertyPendingTolerance", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA4/_PropertyStartUnsuccessfulDelay", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB1/_PropertyErrorDelay", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB1/_PropertyMaxAllowedCellSocPct", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB1/_PropertyMaxStartAttempts", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB1/_PropertyMaxStartTime", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB1/_PropertyMinAllowedCellSocPct", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB1/_PropertyModbusUnitId", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB1/_PropertyPendingTolerance", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB1/_PropertyStartUnsuccessfulDelay", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB2/_PropertyErrorDelay", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB2/_PropertyMaxStartAttempts", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB2/_PropertyMaxStartTime", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB2/_PropertyModbusUnitId", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB2/_PropertyPendingTolerance", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB2/_PropertyStartUnsuccessfulDelay", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB3/_PropertyErrorDelay", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB3/_PropertyMaxStartAttempts", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB3/_PropertyMaxStartTime", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB3/_PropertyModbusUnitId", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB3/_PropertyPendingTolerance", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB3/_PropertyStartUnsuccessfulDelay", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB4/_PropertyErrorDelay", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB4/_PropertyMaxStartAttempts", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB4/_PropertyMaxStartTime", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB4/_PropertyModbusUnitId", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB4/_PropertyPendingTolerance", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB4/_PropertyStartUnsuccessfulDelay", RequiredType.INTEGER);
		this.createAndAddHandler("charger0/_PropertyAlias", RequiredType.STRING);
		this.createAndAddHandler("charger1/_PropertyAlias", RequiredType.STRING);
		this.createAndAddHandler("charger1/_PropertyModbusUnitId", RequiredType.INTEGER);
		this.createAndAddHandler("chp0/PartsNumber", RequiredType.STRING);
		this.createAndAddHandler("chp0/SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("ctrlApiModbusTcp0/RunFailed", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlApiModbusTcp0/_PropertyApiTimeout", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlApiModbusTcp0/_PropertyMaxConcurrentConnections", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlApiModbusTcp0/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlApiModbusTcp1/_PropertyApiTimeout", RequiredType.STRING);
		this.createAndAddHandler("ctrlApiModbusTcp1/_PropertyMaxConcurrentConnections", RequiredType.STRING);
		this.createAndAddHandler("ctrlApiModbusTcp1/_PropertyPort", RequiredType.STRING);
		this.createAndAddHandler("ctrlApiRest0/RunFailed", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlApiRest0/_PropertyApiTimeout", RequiredType.STRING);
		this.createAndAddHandler("ctrlApiRest0/_PropertyPort", RequiredType.STRING);
		this.createAndAddHandler("ctrlApiRest1/_PropertyApiTimeout", RequiredType.STRING);
		this.createAndAddHandler("ctrlApiRest1/_PropertyPort", RequiredType.STRING);
		this.createAndAddHandler("ctrlApiWebsocket0/RunFailed", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlBackend0/RunFailed", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlBackend0/UnableToSend", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlBackend0/_PropertyApiTimeout", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlBackend0/_PropertyDebugMode", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlBackend0/_PropertyNoOfCycles", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlBackend0/_PropertyProxyPort", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlBalancing0/_PropertyEssId", RequiredType.STRING);
		this.createAndAddHandler("ctrlBalancing0/_PropertyMeterId", RequiredType.STRING);
		this.createAndAddHandler("ctrlBalancing0/_PropertyTargetGridSetpoint", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlBalancing1/_PropertyTargetGridSetpoint", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold0/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold0/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold0/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold0/_PropertyThreshold", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold1/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold1/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold1/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold1/_PropertyThreshold", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold10/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold10/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold10/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold10/_PropertyThreshold", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold11/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold11/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold11/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold11/_PropertyThreshold", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold12/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold12/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold12/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold12/_PropertyThreshold", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold13/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold13/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold14/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold14/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold14/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold14/_PropertyThreshold", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold15/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold15/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold15/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold17/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold17/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold17/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold17/_PropertyThreshold", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold18/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold18/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold18/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold2/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold2/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold2/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold2/_PropertyThreshold", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold3/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold3/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold3/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold3/_PropertyThreshold", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold4/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold4/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold4/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold5/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold5/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold5/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold6/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold6/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold6/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold7/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold7/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold7/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold8/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold8/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold8/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold9/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold9/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold9/_PropertySwitchedLoadPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelSingleThreshold9/_PropertyThreshold", RequiredType.STRING);
		this.createAndAddHandler("ctrlChannelThreshold0/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("ctrlDebugLog0/RunFailed", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlEmergencyCapacityReserve0/DebugRampPower", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlEmergencyCapacityReserve0/DebugTargetPower", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlEssSellToGridLimit0/_PropertyMaximumSellToGridPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlEvcs2/_PropertyDebugMode", RequiredType.STRING);
		this.createAndAddHandler("ctrlEvcs2/_PropertyDefaultChargeMinPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlEvcs2/_PropertyEnabledCharging", RequiredType.STRING);
		this.createAndAddHandler("ctrlEvcs2/_PropertyEnergySessionLimit", RequiredType.STRING);
		this.createAndAddHandler("ctrlEvcs2/_PropertyForceChargeMinPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlEvcs3/_PropertyDebugMode", RequiredType.STRING);
		this.createAndAddHandler("ctrlEvcs3/_PropertyDefaultChargeMinPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlEvcs3/_PropertyEnabledCharging", RequiredType.STRING);
		this.createAndAddHandler("ctrlEvcs3/_PropertyEnergySessionLimit", RequiredType.STRING);
		this.createAndAddHandler("ctrlEvcs3/_PropertyForceChargeMinPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlFixActivePower0/_PropertyPower", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlFixActivePower1/_PropertyPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlFixReactivePower0/_PropertyPower", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlGridOptimizedCharge0/StartEpochSeconds", RequiredType.STRING);
		this.createAndAddHandler("ctrlGridOptimizedCharge0/TargetEpochSeconds", RequiredType.STRING);
		this.createAndAddHandler("ctrlGridOptimizedCharge0/_PropertySellToGridLimitRampPercentage",
				RequiredType.STRING);
		this.createAndAddHandler("ctrlHighLoadTimeslot0/_PropertyChargePower", RequiredType.STRING);
		this.createAndAddHandler("ctrlHighLoadTimeslot0/_PropertyDischargePower", RequiredType.STRING);
		this.createAndAddHandler("ctrlHighLoadTimeslot0/_PropertyHysteresisSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlHighLoadTimeslot1/_PropertyChargePower", RequiredType.STRING);
		this.createAndAddHandler("ctrlHighLoadTimeslot1/_PropertyDischargePower", RequiredType.STRING);
		this.createAndAddHandler("ctrlHighLoadTimeslot1/_PropertyHysteresisSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlIoFixDigitalOutput0/_PropertyIsOn", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlIoFixDigitalOutput2/_PropertyIsOn", RequiredType.STRING);
		this.createAndAddHandler("ctrlIoFixDigitalOutput3/_PropertyIsOn", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlIoFixDigitalOutput4/_PropertyIsOn", RequiredType.STRING);
		this.createAndAddHandler("ctrlIoFixDigitalOutput5/_PropertyIsOn", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlIoFixDigitalOutput6/_PropertyIsOn", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlIoFixDigitalOutput7/_PropertyIsOn", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlIoFixDigitalOutput8/_PropertyIsOn", RequiredType.STRING);
		this.createAndAddHandler("ctrlIoHeatPump0/_PropertyAutomaticLockCtrlEnabled", RequiredType.STRING);
		this.createAndAddHandler("ctrlIoHeatPump0/_PropertyAutomaticLockGridBuyPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlIoHeatPump0/_PropertyDebugMode", RequiredType.STRING);
		this.createAndAddHandler("ctrlIoHeatPump0/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlIoHeatingElement0/_PropertyMinTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlIoHeatingElement0/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlIoHeatingElement0/_PropertyPowerPerPhase", RequiredType.STRING);
		this.createAndAddHandler("ctrlIoHeatingElement1/_PropertyMinimumSwitchingTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlIoHeatingElement1/_PropertyPowerPerPhase", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitActivePower0/_PropertyMaxChargePower", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitActivePower0/_PropertyMaxDischargePower", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitActivePower0/_PropertyValidatePowerConstraints", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitActivePower5/_PropertyMaxChargePower", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitActivePower5/_PropertyMaxDischargePower", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitActivePower5/_PropertyValidatePowerConstraints", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitTotalDischarge0/_PropertyEssId", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitTotalDischarge0/_PropertyForceChargePower", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitTotalDischarge0/_PropertyForceChargeSoc", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlLimitTotalDischarge0/_PropertyMinSoc", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlLimitTotalDischarge1/_PropertyForceChargePower", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitTotalDischarge1/_PropertyForceChargeSoc", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlLimitTotalDischarge1/_PropertyMinSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitTotalDischarge2/_PropertyForceChargePower", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitTotalDischarge2/_PropertyForceChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitTotalDischarge2/_PropertyMinSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitTotalDischarge7/_PropertyForceChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitTotalDischarge7/_PropertyMinSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitTotalDischarge8/_PropertyForceChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitTotalDischarge8/_PropertyMinSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity0/_PropertyAllowChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity0/_PropertyAllowDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity0/_PropertyForceChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity0/_PropertyStopChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity0/_PropertyStopDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity1/_PropertyAllowChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity1/_PropertyAllowDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity1/_PropertyForceChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity1/_PropertyStopChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity1/_PropertyStopDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity2/_PropertyAllowChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity2/_PropertyAllowDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity2/_PropertyForceChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity2/_PropertyStopChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity2/_PropertyStopDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity3/_PropertyAllowChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity3/_PropertyAllowDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity3/_PropertyForceChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity3/_PropertyStopChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity3/_PropertyStopDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity4/_PropertyAllowChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity4/_PropertyAllowDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity4/_PropertyForceChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity4/_PropertyStopChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity4/_PropertyStopDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity5/_PropertyAllowChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity5/_PropertyAllowDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity5/_PropertyForceChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity5/_PropertyStopChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity5/_PropertyStopDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity6/_PropertyAllowChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity6/_PropertyAllowDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity6/_PropertyForceChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity6/_PropertyStopChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity6/_PropertyStopDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity7/_PropertyAllowChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity7/_PropertyAllowDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity7/_PropertyForceChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity7/_PropertyStopChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacity7/_PropertyStopDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacityA1/_PropertyAllowChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacityA1/_PropertyStopChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacityA2/_PropertyAllowChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacityA2/_PropertyStopChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacityA3/_PropertyAllowChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacityA3/_PropertyStopChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacityB1/_PropertyAllowChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacityB1/_PropertyStopChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacityB3/_PropertyAllowChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacityB3/_PropertyStopChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacityEssMasterA1/_PropertyAllowDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacityEssMasterA1/_PropertyForceChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacityEssMasterA1/_PropertyStopDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacityEssSlaveB1/_PropertyAllowDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacityEssSlaveB1/_PropertyForceChargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlLimitUsableCapacityEssSlaveB1/_PropertyStopDischargeSoc", RequiredType.STRING);
		this.createAndAddHandler("ctrlMVS0/DebugTargetGridSetpoint", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlPvInverterSellToGridLimit0/_PropertyMaximumSellToGridPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlPvInverterSellToGridLimit1/_PropertyMaximumSellToGridPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlTimeslotPeakshaving0/_PropertyEndTime", RequiredType.STRING);
		this.createAndAddHandler("ctrlTimeslotPeakshaving0/_PropertyStartTime", RequiredType.STRING);
		this.createAndAddHandler("datasource0/_PropertySource", RequiredType.STRING);
		this.createAndAddHandler("ess0/ApplyPowerFailed", RequiredType.INTEGER);
		this.createAndAddHandler("ess0/Riso", RequiredType.FLOAT);
		this.createAndAddHandler("ess0/_PropertyInitialSoc", RequiredType.STRING);
		this.createAndAddHandler("ess0/_PropertyMaxApparentPower", RequiredType.STRING);
		this.createAndAddHandler("ess0/_PropertyMaxBatteryPower", RequiredType.STRING);
		this.createAndAddHandler("ess0/_PropertySource", RequiredType.STRING);
		this.createAndAddHandler("ess0/_PropertyTargetFrequencyOffGrid", RequiredType.STRING);
		this.createAndAddHandler("ess1/SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("ess1/_PropertyMaxBatteryPower", RequiredType.STRING);
		this.createAndAddHandler("ess1/_PropertySource", RequiredType.STRING);
		this.createAndAddHandler("ess2/Riso", RequiredType.FLOAT);
		this.createAndAddHandler("ess2/SerialNumber", RequiredType.STRING);
		this.createAndAddHandler("ess3/_PropertySource", RequiredType.STRING);
		this.createAndAddHandler("evcs0/ComModule", RequiredType.STRING);
		this.createAndAddHandler("evcs0/DipSwitch1", RequiredType.STRING);
		this.createAndAddHandler("evcs0/DipSwitch2", RequiredType.STRING);
		this.createAndAddHandler("evcs0/Firmware", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawActiveCurrentL1", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawActiveCurrentL2", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawActiveCurrentL3", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawCableCurrentLimit", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawChargeStatusPwm", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawCharging", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawContactorActual", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawContactorError", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawContactorHlcTarget", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawContactorTarget", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawDeviceHardwareVersion", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawDeviceProduct", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawDiodePresent", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawEmergencyShutdown", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawEvPresent", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawGridCurrentLimit", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawMeterSerialnumber", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawPlugLockError", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawPlugLockStateActual", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawPlugLockStateTarget", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawSaliaFirmwareprogress", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawSessionAuthorizationMethod", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawSessionStatusAuthorization", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawVentilationStateActual", RequiredType.STRING);
		this.createAndAddHandler("evcs0/Serial", RequiredType.STRING);
		this.createAndAddHandler("evcs0/_PropertyMaxHwCurrent", RequiredType.STRING);
		this.createAndAddHandler("evcs0/_PropertySource", RequiredType.STRING);
		this.createAndAddHandler("evcs1/ComModule", RequiredType.STRING);
		this.createAndAddHandler("evcs1/DipSwitch1", RequiredType.STRING);
		this.createAndAddHandler("evcs1/DipSwitch2", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawActiveCurrentL1", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawActiveCurrentL2", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawActiveCurrentL3", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawCableCurrentLimit", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawChargeStatusPwm", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawCharging", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawContactorActual", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawContactorError", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawContactorHlcTarget", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawContactorTarget", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawDeviceHardwareVersion", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawDeviceProduct", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawDiodePresent", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawEmergencyShutdown", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawEvPresent", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawGridCurrentLimit", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawMeterSerialnumber", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawPlugLockError", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawPlugLockStateActual", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawPlugLockStateTarget", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawSaliaFirmwareprogress", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawSessionAuthorizationMethod", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawSessionStatusAuthorization", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawVentilationStateActual", RequiredType.STRING);
		this.createAndAddHandler("evcs1/Serial", RequiredType.STRING);
		this.createAndAddHandler("evcs1/_PropertyMaxHwCurrent", RequiredType.STRING);
		this.createAndAddHandler("evcs1/_PropertyMinHwCurrent", RequiredType.STRING);
		this.createAndAddHandler("evcs1/_PropertySource", RequiredType.STRING);
		this.createAndAddHandler("evcs10/ComModule", RequiredType.STRING);
		this.createAndAddHandler("evcs10/DipSwitch1", RequiredType.STRING);
		this.createAndAddHandler("evcs10/DipSwitch2", RequiredType.STRING);
		this.createAndAddHandler("evcs10/Serial", RequiredType.STRING);
		this.createAndAddHandler("evcs2/ComModule", RequiredType.STRING);
		this.createAndAddHandler("evcs2/DipSwitch1", RequiredType.STRING);
		this.createAndAddHandler("evcs2/DipSwitch2", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawCableCurrentLimit", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawChargeStatusPwm", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawCharging", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawContactorActual", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawContactorError", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawContactorHlcTarget", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawContactorTarget", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawDeviceHardwareVersion", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawDeviceProduct", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawDiodePresent", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawEmergencyShutdown", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawEvPresent", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawGridCurrentLimit", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawMeterSerialnumber", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawPlugLockError", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawPlugLockStateActual", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawPlugLockStateTarget", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawSaliaFirmwareprogress", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawSessionStatusAuthorization", RequiredType.STRING);
		this.createAndAddHandler("evcs2/RawVentilationStateActual", RequiredType.STRING);
		this.createAndAddHandler("evcs2/Serial", RequiredType.STRING);
		this.createAndAddHandler("evcs2/_PropertyDebugMode", RequiredType.STRING);
		this.createAndAddHandler("evcs2/_PropertyMinHwCurrent", RequiredType.STRING);
		this.createAndAddHandler("evcs2/_PropertySource", RequiredType.STRING);
		this.createAndAddHandler("evcs20/ChargedEnergy", RequiredType.FLOAT);
		this.createAndAddHandler("evcs3/ComModule", RequiredType.STRING);
		this.createAndAddHandler("evcs3/DipSwitch1", RequiredType.STRING);
		this.createAndAddHandler("evcs3/DipSwitch2", RequiredType.STRING);
		this.createAndAddHandler("evcs3/RawSessionStatusAuthorization", RequiredType.INTEGER);
		this.createAndAddHandler("evcs3/Serial", RequiredType.STRING);
		this.createAndAddHandler("evcs3/_PropertyDebugMode", RequiredType.STRING);
		this.createAndAddHandler("evcs3/_PropertyMinHwCurrent", RequiredType.STRING);
		this.createAndAddHandler("evcs4/ComModule", RequiredType.STRING);
		this.createAndAddHandler("evcs4/DipSwitch1", RequiredType.STRING);
		this.createAndAddHandler("evcs4/DipSwitch2", RequiredType.STRING);
		this.createAndAddHandler("evcs4/Serial", RequiredType.STRING);
		this.createAndAddHandler("evcs40/RawActiveCurrentL1", RequiredType.STRING);
		this.createAndAddHandler("evcs40/RawActiveCurrentL2", RequiredType.STRING);
		this.createAndAddHandler("evcs40/RawActiveCurrentL3", RequiredType.STRING);
		this.createAndAddHandler("evcs5/ComModule", RequiredType.STRING);
		this.createAndAddHandler("evcs5/DipSwitch1", RequiredType.STRING);
		this.createAndAddHandler("evcs5/DipSwitch2", RequiredType.STRING);
		this.createAndAddHandler("evcs5/Serial", RequiredType.STRING);
		this.createAndAddHandler("evcs6/ComModule", RequiredType.STRING);
		this.createAndAddHandler("evcs6/DipSwitch1", RequiredType.STRING);
		this.createAndAddHandler("evcs6/DipSwitch2", RequiredType.STRING);
		this.createAndAddHandler("evcs6/Serial", RequiredType.STRING);
		this.createAndAddHandler("evcs7/ComModule", RequiredType.STRING);
		this.createAndAddHandler("evcs7/DipSwitch1", RequiredType.STRING);
		this.createAndAddHandler("evcs7/DipSwitch2", RequiredType.STRING);
		this.createAndAddHandler("evcs7/Serial", RequiredType.STRING);
		this.createAndAddHandler("evcs8/ComModule", RequiredType.STRING);
		this.createAndAddHandler("evcs8/DipSwitch1", RequiredType.STRING);
		this.createAndAddHandler("evcs8/DipSwitch2", RequiredType.STRING);
		this.createAndAddHandler("evcs8/Serial", RequiredType.STRING);
		this.createAndAddHandler("evcs9/ComModule", RequiredType.STRING);
		this.createAndAddHandler("evcs9/DipSwitch1", RequiredType.STRING);
		this.createAndAddHandler("evcs9/DipSwitch2", RequiredType.STRING);
		this.createAndAddHandler("evcs9/Serial", RequiredType.STRING);
		this.createAndAddHandler("evcsCluster0/_PropertyDebugMode", RequiredType.STRING);
		this.createAndAddHandler("gridcon0/CcuCurrentIl1", RequiredType.FLOAT);
		this.createAndAddHandler("gridcon0/CcuCurrentIl2", RequiredType.FLOAT);
		this.createAndAddHandler("gridcon0/CcuCurrentIl3", RequiredType.FLOAT);
		this.createAndAddHandler("gridcon0/CcuPowerP", RequiredType.FLOAT);
		this.createAndAddHandler("gridcon0/CcuPowerQ", RequiredType.FLOAT);
		this.createAndAddHandler("gridcon0/CommandControlParameterPRef", RequiredType.INTEGER);
		this.createAndAddHandler("gridcon0/CommandControlParameterPRefDebug", RequiredType.FLOAT);
		this.createAndAddHandler("gridcon0/DcdcMeasurementsCurrentStringA", RequiredType.FLOAT);
		this.createAndAddHandler("gridcon0/DcdcMeasurementsPowerStringA", RequiredType.FLOAT);
		this.createAndAddHandler("gridcon0/DcdcMeasurementsUtilizationStringA", RequiredType.FLOAT);
		this.createAndAddHandler("gridcon0/DcdcMeasurementsUtilizationStringC", RequiredType.FLOAT);
		this.createAndAddHandler("gridcon0/DcdcStatusTemperatureIgbtMax", RequiredType.FLOAT);
		this.createAndAddHandler("gridcon0/DcdcStatusTemperatureMcuBoard", RequiredType.FLOAT);
		this.createAndAddHandler("gridcon0/Inverter1StatusDcLinkActivePower", RequiredType.FLOAT);
		this.createAndAddHandler("gridcon0/Inverter1StatusDcLinkCurrent", RequiredType.FLOAT);
		this.createAndAddHandler("gridcon0/Inverter2StatusDcLinkActivePower", RequiredType.FLOAT);
		this.createAndAddHandler("gridcon0/Inverter2StatusDcLinkCurrent", RequiredType.FLOAT);
		this.createAndAddHandler("gridcon0/Inverter3StatusDcLinkActivePower", RequiredType.FLOAT);
		this.createAndAddHandler("gridcon0/Inverter3StatusDcLinkCurrent", RequiredType.FLOAT);
		this.createAndAddHandler("influx0/_PropertyIsReadOnly", RequiredType.STRING);
		this.createAndAddHandler("influx0/_PropertyNoOfCycles", RequiredType.STRING);
		this.createAndAddHandler("influx0/_PropertyPort", RequiredType.STRING);
		this.createAndAddHandler("io0/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("io0/_PropertySource", RequiredType.STRING);
		this.createAndAddHandler("io1/_PropertySource", RequiredType.STRING);
		this.createAndAddHandler("kacoCore0/Serialnumber", RequiredType.STRING);
		this.createAndAddHandler("kacoCore0/_PropertySerialnumber", RequiredType.STRING);
		this.createAndAddHandler("kacoCore0/_PropertyUserkey", RequiredType.STRING);
		this.createAndAddHandler("kacoCore1/Serialnumber", RequiredType.STRING);
		this.createAndAddHandler("kacoCore1/_PropertySerialnumber", RequiredType.STRING);
		this.createAndAddHandler("kacoCore2/Serialnumber", RequiredType.STRING);
		this.createAndAddHandler("kacoCore2/_PropertySerialnumber", RequiredType.STRING);
		this.createAndAddHandler("kostalPiko1/ArticleNumber", RequiredType.STRING);
		this.createAndAddHandler("kostalPiko1/FirmwareVersion", RequiredType.STRING);
		this.createAndAddHandler("kostalPiko1/HardwareVersion", RequiredType.STRING);
		this.createAndAddHandler("kostalPiko1/HomeConsumptionGrid", RequiredType.FLOAT);
		this.createAndAddHandler("kostalPiko1/HomeConsumptionPv", RequiredType.FLOAT);
		this.createAndAddHandler("kostalPiko1/KomboardVersion", RequiredType.STRING);
		this.createAndAddHandler("kostalPiko1/MaxResidualCurrent", RequiredType.FLOAT);
		this.createAndAddHandler("kostalPiko1/ParameterVersion", RequiredType.STRING);
		this.createAndAddHandler("kostalPiko1/_PropertyPort", RequiredType.STRING);
		this.createAndAddHandler("kostalPiko1/_PropertyUnitID", RequiredType.STRING);
		this.createAndAddHandler("meter0/_PropertyInvert", RequiredType.INTEGER);
		this.createAndAddHandler("meter0/_PropertyModbusUnitId", RequiredType.INTEGER);
		this.createAndAddHandler("meter0/_PropertySource", RequiredType.STRING);
		this.createAndAddHandler("meter1/_PropertyInvert", RequiredType.INTEGER);
		this.createAndAddHandler("meter1/_PropertyModbusUnitId", RequiredType.INTEGER);
		this.createAndAddHandler("meter1/_PropertySource", RequiredType.STRING);
		this.createAndAddHandler("meter10/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("meter10/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter11/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter13/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter14/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter15/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter16/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter17/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter18/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter30/_PropertySource", RequiredType.STRING);
		this.createAndAddHandler("meter5/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter6/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter7/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("meter7/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter8/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter9/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("modbus0/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbus0/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbus1/_PropertyBaudRate", RequiredType.INTEGER);
		this.createAndAddHandler("modbus1/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbus1/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbus10/_PropertyInvalidateElementsAfterReadErrors", RequiredType.STRING);
		this.createAndAddHandler("modbus10/_PropertyPort", RequiredType.STRING);
		this.createAndAddHandler("modbus19/_PropertyInvalidateElementsAfterReadErrors", RequiredType.STRING);
		this.createAndAddHandler("modbus19/_PropertyPort", RequiredType.STRING);
		this.createAndAddHandler("modbus2/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbus21/_PropertyInvalidateElementsAfterReadErrors", RequiredType.STRING);
		this.createAndAddHandler("modbus3/_PropertyBaudRate", RequiredType.STRING);
		this.createAndAddHandler("modbus3/_PropertyDatabits", RequiredType.STRING);
		this.createAndAddHandler("modbus3/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbus3/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbus30/_PropertyInvalidateElementsAfterReadErrors", RequiredType.STRING);
		this.createAndAddHandler("modbus30/_PropertyPort", RequiredType.STRING);
		this.createAndAddHandler("modbus4/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbus5/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbus6/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbus7/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbus8/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbus9/_PropertyInvalidateElementsAfterReadErrors", RequiredType.STRING);
		this.createAndAddHandler("modbusBmsA1/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbusBmsA1/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbusBmsA2/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbusBmsA2/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbusBmsA3/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbusBmsA3/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbusBmsA4/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbusBmsA4/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbusBmsB1/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbusBmsB1/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbusBmsB2/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbusBmsB2/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbusBmsB3/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbusBmsB3/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbusBmsB4/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbusBmsB4/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbusInverterA1/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbusInverterA1/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbusInverterA2/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbusInverterA2/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbusInverterA3/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbusInverterA3/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbusInverterA4/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbusInverterA4/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbusInverterB1/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbusInverterB1/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbusInverterB2/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbusInverterB2/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbusInverterB3/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbusInverterB3/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbusInverterB4/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbusInverterB4/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbusIoWago/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbusIoWago/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("modbusIoWago1/_PropertyInvalidateElementsAfterReadErrors", RequiredType.STRING);
		this.createAndAddHandler("modbusIoWago1/_PropertyPort", RequiredType.STRING);
		this.createAndAddHandler("modbusMeter0/_PropertyInvalidateElementsAfterReadErrors", RequiredType.STRING);
		this.createAndAddHandler("modbusMeter0/_PropertyPort", RequiredType.STRING);
		this.createAndAddHandler("modbusMeter1/_PropertyInvalidateElementsAfterReadErrors", RequiredType.STRING);
		this.createAndAddHandler("modbusMeter1/_PropertyPort", RequiredType.STRING);
		this.createAndAddHandler("modbusSlaveWago/_PropertyInvalidateElementsAfterReadErrors", RequiredType.STRING);
		this.createAndAddHandler("modbusSlaveWago/_PropertyPort", RequiredType.STRING);
		this.createAndAddHandler("modbusWago/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbusWago/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter0/S101Wh", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S103A", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S103AphA", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S103AphB", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S103AphC", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S103Hz", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter0/S103PPVphAB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter0/S103PPVphBC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter0/S103PPVphCA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter0/S103Pf", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S103PhVphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter0/S103PhVphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter0/S103PhVphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter0/S103VAr", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S103Va", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S103Wh", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S120AhrRtg", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S120MaxChaRte", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S120PFRtgQ1", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S120PFRtgQ4", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S120VArRtgQ1", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S120VArRtgQ2", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S120VArRtgQ3", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S120VArRtgQ4", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S120WRtg", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S121ECPNomHz", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S121PFMinQ1", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S121PFMinQ2", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S121PFMinQ3", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S121VMax", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S121VMin", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S121WMax", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S123OutPFSet", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S123VArAvalPct", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S123VArMaxPct", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S123VArWMaxPct", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter0/S1Opt", RequiredType.STRING);
		this.createAndAddHandler("pvInverter0/S1Sn", RequiredType.STRING);
		this.createAndAddHandler("pvInverter0/S1Vr", RequiredType.STRING);
		this.createAndAddHandler("pvInverter01/_PropertyMaxActivePower", RequiredType.STRING);
		this.createAndAddHandler("pvInverter01/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("pvInverter1/S103Pf", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S103Wh", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S120AhrRtg", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter1/S120MaxChaRte", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S120PFRtgQ1", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S120PFRtgQ4", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S120VARtg", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S120VArRtgQ1", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S120VArRtgQ4", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S121ECPNomHz", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S121PFMinQ1", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S121PFMinQ2", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S121PFMinQ3", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S121PFMinQ4", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S121VArMaxQ1", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S121VArMaxQ2", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S121WMax", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S123OutPFSet", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S123VArAvalPct", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S123VArMaxPct", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S123VArWMaxPct", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter1/S1Opt", RequiredType.STRING);
		this.createAndAddHandler("pvInverter1/S1Sn", RequiredType.STRING);
		this.createAndAddHandler("pvInverter1/S1Vr", RequiredType.STRING);
		this.createAndAddHandler("pvInverter1/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("pvInverter10/S103A", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S103AphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S103AphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S103AphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S103Dca", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S103Dcv", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S103Dcw", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S103Hz", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S103Pf", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S103PhVphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S103PhVphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S103PhVphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S103TmpCab", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S103VAr", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S103Va", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S103W", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S103Wh", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S120PFRtgQ1", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S120PFRtgQ4", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S120VARtg", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S120VArRtgQ1", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S120VArRtgQ4", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S120WRtg", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S121VAMax", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S121VRef", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S121WMax", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S123OutPFSet", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S123VArMaxPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S123VArWMaxPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter10/S123WMaxLimPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter19/S103Wh", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter19/S1Opt", RequiredType.STRING);
		this.createAndAddHandler("pvInverter19/S1Sn", RequiredType.STRING);
		this.createAndAddHandler("pvInverter19/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("pvInverter2/S103Wh", RequiredType.FLOAT);
		this.createAndAddHandler("pvInverter2/S1Opt", RequiredType.STRING);
		this.createAndAddHandler("pvInverter2/S1Sn", RequiredType.STRING);
		this.createAndAddHandler("pvInverter2/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("pvInverter21/S103A", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S103AphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S103AphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S103AphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S103Dca", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S103Dcv", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S103Dcw", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S103Hz", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S103Pf", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S103PhVphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S103PhVphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S103PhVphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S103TmpCab", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S103VAr", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S103Va", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S103W", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S103Wh", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S120PFRtgQ1", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S120PFRtgQ4", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S120VARtg", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S120VArRtgQ1", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S120VArRtgQ4", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S120WRtg", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S121VAMax", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S121VRef", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S121WMax", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S123OutPFSet", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S123VArMaxPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S123VArWMaxPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter21/S123WMaxLimPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S103A", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S103AphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S103AphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S103AphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S103Dca", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S103Dcv", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S103Dcw", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S103Hz", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S103Pf", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S103PhVphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S103PhVphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S103PhVphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S103TmpCab", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S103VAr", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S103Va", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S103W", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S103Wh", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S120PFRtgQ1", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S120PFRtgQ4", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S120VARtg", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S120VArRtgQ1", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S120VArRtgQ4", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S120WRtg", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S121VAMax", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S121VRef", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S121WMax", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S123OutPFSet", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S123VArMaxPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S123VArWMaxPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S123WMaxLimPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter3/S1Opt", RequiredType.STRING);
		this.createAndAddHandler("pvInverter3/S1Sn", RequiredType.STRING);
		this.createAndAddHandler("pvInverter4/S103A", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S103AphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S103AphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S103AphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S103Dca", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S103Dcv", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S103Dcw", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S103Hz", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S103Pf", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S103PhVphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S103PhVphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S103PhVphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S103TmpCab", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S103VAr", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S103Va", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S103W", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S103Wh", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S120PFRtgQ1", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S120PFRtgQ4", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S120VARtg", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S120VArRtgQ1", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S120VArRtgQ4", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S120WRtg", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S121VAMax", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S121VRef", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S121WMax", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S123OutPFSet", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S123VArMaxPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S123VArWMaxPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S123WMaxLimPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter4/S1Opt", RequiredType.STRING);
		this.createAndAddHandler("pvInverter4/S1Sn", RequiredType.STRING);
		this.createAndAddHandler("pvInverter5/S103A", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S103AphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S103AphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S103AphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S103Dca", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S103Dcv", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S103Dcw", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S103Hz", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S103Pf", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S103PhVphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S103PhVphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S103PhVphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S103TmpCab", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S103VAr", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S103Va", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S103W", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S103Wh", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S120PFRtgQ1", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S120PFRtgQ4", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S120VARtg", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S120VArRtgQ1", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S120VArRtgQ4", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S120WRtg", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S121VAMax", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S121VRef", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S121WMax", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S123OutPFSet", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S123VArMaxPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S123VArWMaxPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter5/S123WMaxLimPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S103A", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S103AphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S103AphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S103AphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S103Dca", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S103Dcv", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S103Dcw", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S103Hz", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S103Pf", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S103PhVphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S103PhVphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S103PhVphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S103TmpCab", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S103VAr", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S103Va", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S103W", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S103Wh", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S120PFRtgQ1", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S120PFRtgQ4", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S120VARtg", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S120VArRtgQ1", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S120VArRtgQ4", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S120WRtg", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S121VAMax", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S121VRef", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S121WMax", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S123OutPFSet", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S123VArMaxPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S123VArWMaxPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter6/S123WMaxLimPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S103A", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S103AphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S103AphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S103AphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S103Dca", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S103Dcv", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S103Dcw", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S103Hz", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S103Pf", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S103PhVphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S103PhVphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S103PhVphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S103TmpCab", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S103VAr", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S103Va", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S103W", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S103Wh", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S120PFRtgQ1", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S120PFRtgQ4", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S120VARtg", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S120VArRtgQ1", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S120VArRtgQ4", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S120WRtg", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S121VAMax", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S121VRef", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S121WMax", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S123OutPFSet", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S123VArMaxPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S123VArWMaxPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter7/S123WMaxLimPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S103A", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S103AphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S103AphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S103AphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S103Dca", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S103Dcv", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S103Dcw", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S103Hz", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S103Pf", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S103PhVphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S103PhVphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S103PhVphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S103TmpCab", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S103VAr", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S103Va", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S103W", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S103Wh", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S120PFRtgQ1", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S120PFRtgQ4", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S120VARtg", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S120VArRtgQ1", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S120VArRtgQ4", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S120WRtg", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S121VAMax", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S121VRef", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S121WMax", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S123OutPFSet", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S123VArMaxPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S123VArWMaxPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter8/S123WMaxLimPct", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter9/S103A", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter9/S103AphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter9/S103AphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter9/S103AphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter9/S103Dca", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter9/S103Dcv", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter9/S103Dcw", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter9/S103Hz", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter9/S103Pf", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter9/S103PhVphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter9/S103PhVphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter9/S103PhVphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter9/S103TmpCab", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter9/S103VAr", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter9/S103Wh", RequiredType.FLOAT);
		this.createAndAddHandler("rrd4j0/_PropertyNoOfCycles", RequiredType.STRING);
		this.createAndAddHandler("scheduler0/ControllerIsMissing", RequiredType.INTEGER);
		this.createAndAddHandler("timeOfUseTariff0/_PropertyZipcode", RequiredType.INTEGER);
	}

	/**
	 * Handles a {@link FieldTypeConflictException}; adds special handling for
	 * fields that already exist in the database.
	 *
	 * @param e the {@link FieldTypeConflictException}
	 */
	public synchronized void handleException(InfluxException e) throws IllegalStateException, IllegalArgumentException {
		this.handleExceptionMessage(e.getMessage());
	}

	protected synchronized boolean handleExceptionMessage(String message)
			throws IllegalStateException, IllegalArgumentException {
		var matcher = FieldTypeConflictHandler.FIELD_TYPE_CONFLICT_EXCEPTION_PATTERN.matcher(message);
		if (!matcher.find()) {
			return false;
		}
		var field = matcher.group("channel");
		var thisType = matcher.group("thisType");
		var requiredType = RequiredType.valueOf(matcher.group("requiredType").toUpperCase());

		if (this.specialCaseFieldHandlers.containsKey(field)) {
			// Special handling had already been added.
			this.parent.logWarn(this.log, "Special field handler for message [" + message + "] is already existing");
			return false;
		}

		var handler = this.createAndAddHandler(field, requiredType);

		if (handler == null) {
			this.parent.logWarn(this.log, "Unable to add special field handler for [" + field + "] from [" + thisType
					+ "] to [" + requiredType.name().toLowerCase() + "]");
		}
		this.parent.logInfo(this.log,
				"Add handler for [" + field + "] from [" + thisType + "] to [" + requiredType.name().toLowerCase()
						+ "]\n" //
						+ "Add predefined FieldTypeConflictHandler: this.createAndAddHandler(\"" + field
						+ "\", RequiredType." + requiredType.name() + ");");
		;

		return true;
	}

	private static enum RequiredType {
		STRING, INTEGER, FLOAT;
	}

	private BiConsumer<Point, JsonElement> createAndAddHandler(String field, RequiredType requiredType)
			throws IllegalStateException {
		var handler = this.createHandler(field, requiredType);
		if (this.specialCaseFieldHandlers.put(field, handler) != null) {
			throw new IllegalStateException("Handler for field [" + field + "] was already existing");
		}
		return handler;
	}

	/**
	 * Creates a Handler for the given field, to convert a Point to a
	 * 'requiredType'.
	 * 
	 * @param field        the field name, i.e. the Channel-Address
	 * @param requiredType the {@link RequiredType}
	 * @return the Handler
	 */
	private BiConsumer<Point, JsonElement> createHandler(String field, RequiredType requiredType) {
		switch (requiredType) {
		case STRING:
			return (builder, jValue) -> {
				var value = getAsFieldTypeString(jValue);
				if (value != null) {
					builder.addField(field, value);
				}
			};

		case INTEGER:
			return (builder, jValue) -> {
				try {
					var value = getAsFieldTypeNumber(jValue);
					if (value != null) {
						builder.addField(field, value);
					}
				} catch (NumberFormatException e1) {
					try {
						// Failed -> try conversion to float and then to int
						var value = getAsFieldTypeFloat(jValue);
						if (value != null) {
							builder.addField(field, Math.round(value));
						}
					} catch (NumberFormatException e2) {
						this.parent.logWarn(this.log, "Unable to convert field [" + field + "] value [" + jValue
								+ "] to integer: " + e2.getMessage());
					}
				}
			};

		case FLOAT:
			return (builder, jValue) -> {
				try {
					var value = getAsFieldTypeFloat(jValue);
					if (value != null) {
						builder.addField(field, value);
					}
				} catch (NumberFormatException e1) {
					this.parent.logInfo(this.log, "Unable to convert field [" + field + "] value [" + jValue
							+ "] to float: " + e1.getMessage());
				}
			};
		}
		return null; // can never happen
	}

	/**
	 * Convert JsonElement to String.
	 *
	 * @param jValue the value
	 * @return the value as String; null if value represents null
	 */
	private static String getAsFieldTypeString(JsonElement jValue) {
		if (jValue.isJsonNull()) {
			return null;
		}
		return jValue.toString().replace("\"", "");
	}

	/**
	 * Convert JsonElement to Number.
	 *
	 * @param jValue the value
	 * @return the value as Number; null if value represents null
	 * @throws NumberFormatException on error
	 */
	private static Number getAsFieldTypeNumber(JsonElement jValue) throws NumberFormatException {
		if (jValue.isJsonNull()) {
			return null;
		}
		var value = jValue.toString().replace("\"", "");
		if (value.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e1) {
			if (value.equalsIgnoreCase("false")) {
				return 0L;
			} else if (value.equalsIgnoreCase("true")) {
				return 1L;
			} else {
				throw e1;
			}
		}
	}

	/**
	 * Convert JsonElement to Float.
	 *
	 * @param jValue the value
	 * @return the value as Float; null if value represents null
	 * @throws NumberFormatException on error
	 */
	private static Float getAsFieldTypeFloat(JsonElement jValue) throws NumberFormatException {
		if (jValue.isJsonNull()) {
			return null;
		}
		var value = jValue.toString().replace("\"", "");
		if (value.isEmpty()) {
			return null;
		}
		return Float.parseFloat(value);
	}

	/**
	 * Gets the handler for the given Field.
	 *
	 * @param field the Field
	 * @return the handler or null
	 */
	public BiConsumer<Point, JsonElement> getHandler(String field) {
		return this.specialCaseFieldHandlers.get(field);
	}
}
