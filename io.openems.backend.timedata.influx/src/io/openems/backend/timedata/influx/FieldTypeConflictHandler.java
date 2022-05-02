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
			"^partial write: field type conflict: input field \"(?<channel>.*)\" on measurement \"data\" is type (?<thisType>\\w+), already exists as type (?<requiredType>\\w+) dropped=\\d+$");

	private final Logger log = LoggerFactory.getLogger(FieldTypeConflictHandler.class);
	private final Influx parent;
	private final ConcurrentHashMap<String, BiConsumer<Point, JsonElement>> specialCaseFieldHandlers = new ConcurrentHashMap<>();

	public FieldTypeConflictHandler(Influx parent) {
		this.parent = parent;
		this.initializePredefinedHandlers();
	}

	/**
	 * Add some already known Handlers.
	 */
	private void initializePredefinedHandlers() {
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
		this.createAndAddHandler("battery0/Tower1BmsSerialNumber", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter0/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter1/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter1/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("batteryInverter2/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter3/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter4/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter5/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter6/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter7/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter8/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverter9/Dca", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterA1/_PropertyActivateWatchdog", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterA1/_PropertyTimeLimitNoPower", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterA1/_PropertyWatchdoginterval", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterA2/_PropertyActivateWatchdog", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterA2/_PropertyTimeLimitNoPower", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterA3/_PropertyActivateWatchdog", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterB1/_PropertyActivateWatchdog", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterB1/_PropertyTimeLimitNoPower", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterB1/_PropertyWatchdoginterval", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterB2/_PropertyActivateWatchdog", RequiredType.INTEGER);
		this.createAndAddHandler("batteryInverterB3/_PropertyActivateWatchdog", RequiredType.INTEGER);
		this.createAndAddHandler("bms0/_PropertyWatchdog", RequiredType.STRING);
		this.createAndAddHandler("bms1/_PropertyMaxAllowedCurrentDifference", RequiredType.INTEGER);
		this.createAndAddHandler("bms1/_PropertyMaxAllowedVoltageDifference", RequiredType.INTEGER);
		this.createAndAddHandler("bms3/_PropertyMaxAllowedVoltageDifference", RequiredType.INTEGER);
		this.createAndAddHandler("bms4/_PropertyMaxAllowedVoltageDifference", RequiredType.INTEGER);
		this.createAndAddHandler("bms5/_PropertyMaxAllowedVoltageDifference", RequiredType.INTEGER);
		this.createAndAddHandler("bms6/_PropertyMaxAllowedVoltageDifference", RequiredType.INTEGER);
		this.createAndAddHandler("bms7/_PropertyMaxAllowedCurrentDifference", RequiredType.INTEGER);
		this.createAndAddHandler("bms7/_PropertyMaxAllowedVoltageDifference", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA1/_PropertyErrorDelay", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA1/_PropertyMaxStartAttempts", RequiredType.INTEGER);
		this.createAndAddHandler("bmsA1/_PropertyMaxStartTime", RequiredType.INTEGER);
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
		this.createAndAddHandler("bmsB1/_PropertyMaxStartAttempts", RequiredType.INTEGER);
		this.createAndAddHandler("bmsB1/_PropertyMaxStartTime", RequiredType.INTEGER);
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
		this.createAndAddHandler("ctrlApiModbusTcp1/_PropertyApiTimeout", RequiredType.STRING);
		this.createAndAddHandler("ctrlApiModbusTcp1/_PropertyMaxConcurrentConnections", RequiredType.STRING);
		this.createAndAddHandler("ctrlApiModbusTcp1/_PropertyPort", RequiredType.STRING);
		this.createAndAddHandler("ctrlApiRest0/_PropertyApiTimeout", RequiredType.STRING);
		this.createAndAddHandler("ctrlApiRest0/_PropertyPort", RequiredType.STRING);
		this.createAndAddHandler("ctrlApiRest1/_PropertyApiTimeout", RequiredType.STRING);
		this.createAndAddHandler("ctrlApiRest1/_PropertyPort", RequiredType.STRING);
		this.createAndAddHandler("ctrlBackend0/_PropertyNoOfCycles", RequiredType.INTEGER);
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
		this.createAndAddHandler("ctrlMVS0/DebugTargetGridSetpoint", RequiredType.INTEGER);
		this.createAndAddHandler("ctrlPvInverterSellToGridLimit0/_PropertyMaximumSellToGridPower", RequiredType.STRING);
		this.createAndAddHandler("ctrlPvInverterSellToGridLimit1/_PropertyMaximumSellToGridPower", RequiredType.STRING);
		this.createAndAddHandler("ess0/_PropertyInitialSoc", RequiredType.STRING);
		this.createAndAddHandler("ess0/_PropertyMaxApparentPower", RequiredType.STRING);
		this.createAndAddHandler("ess0/_PropertyMaxBatteryPower", RequiredType.STRING);
		this.createAndAddHandler("ess0/_PropertyTargetFrequencyOffGrid", RequiredType.STRING);
		this.createAndAddHandler("ess1/_PropertyMaxBatteryPower", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawActiveCurrentL1", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawActiveCurrentL2", RequiredType.STRING);
		this.createAndAddHandler("evcs0/RawActiveCurrentL3", RequiredType.STRING);
		this.createAndAddHandler("evcs0/_PropertyMaxHwCurrent", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawActiveCurrentL1", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawActiveCurrentL2", RequiredType.STRING);
		this.createAndAddHandler("evcs1/RawActiveCurrentL3", RequiredType.STRING);
		this.createAndAddHandler("evcs1/_PropertyMaxHwCurrent", RequiredType.STRING);
		this.createAndAddHandler("evcs1/_PropertyMinHwCurrent", RequiredType.STRING);
		this.createAndAddHandler("evcs2/_PropertyDebugMode", RequiredType.STRING);
		this.createAndAddHandler("evcs2/_PropertyMinHwCurrent", RequiredType.STRING);
		this.createAndAddHandler("evcs3/_PropertyDebugMode", RequiredType.STRING);
		this.createAndAddHandler("evcs3/_PropertyMinHwCurrent", RequiredType.STRING);
		this.createAndAddHandler("evcs40/RawActiveCurrentL1", RequiredType.STRING);
		this.createAndAddHandler("evcs40/RawActiveCurrentL2", RequiredType.STRING);
		this.createAndAddHandler("evcs40/RawActiveCurrentL3", RequiredType.STRING);
		this.createAndAddHandler("evcsCluster0/_PropertyDebugMode", RequiredType.STRING);
		this.createAndAddHandler("influx0/_PropertyIsReadOnly", RequiredType.STRING);
		this.createAndAddHandler("influx0/_PropertyNoOfCycles", RequiredType.STRING);
		this.createAndAddHandler("influx0/_PropertyPort", RequiredType.STRING);
		this.createAndAddHandler("io0/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("kostalPiko1/_PropertyPort", RequiredType.STRING);
		this.createAndAddHandler("kostalPiko1/_PropertyUnitID", RequiredType.STRING);
		this.createAndAddHandler("meter0/_PropertyModbusUnitId", RequiredType.INTEGER);
		this.createAndAddHandler("meter1/_PropertyInvert", RequiredType.INTEGER);
		this.createAndAddHandler("meter1/_PropertyModbusUnitId", RequiredType.INTEGER);
		this.createAndAddHandler("meter10/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("meter10/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter11/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter13/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter14/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter15/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter16/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter17/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter18/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter5/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter6/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter7/_PropertyInvert", RequiredType.STRING);
		this.createAndAddHandler("meter7/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter8/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("meter9/_PropertyModbusUnitId", RequiredType.STRING);
		this.createAndAddHandler("modbus0/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbus0/_PropertyPort", RequiredType.INTEGER);
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
		this.createAndAddHandler("modbusWago/_PropertyInvalidateElementsAfterReadErrors", RequiredType.INTEGER);
		this.createAndAddHandler("modbusWago/_PropertyPort", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter0/S103Hz", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter0/S103PPVphAB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter0/S103PPVphBC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter0/S103PPVphCA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter0/S103PhVphA", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter0/S103PhVphB", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter0/S103PhVphC", RequiredType.INTEGER);
		this.createAndAddHandler("pvInverter01/_PropertyMaxActivePower", RequiredType.STRING);
		this.createAndAddHandler("pvInverter01/_PropertyModbusUnitId", RequiredType.STRING);
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
		this.createAndAddHandler("pvInverter19/_PropertyModbusUnitId", RequiredType.STRING);
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
		this.createAndAddHandler("rrd4j0/_PropertyNoOfCycles", RequiredType.STRING);
		this.createAndAddHandler("timeOfUseTariff0/_PropertyZipcode", RequiredType.INTEGER);
	}

	/**
	 * Handles a {@link FieldTypeConflictException}; adds special handling for
	 * fields that already exist in the database.
	 *
	 * @param e the {@link FieldTypeConflictException}
	 */
	public synchronized void handleException(InfluxException e) {
		var matcher = FieldTypeConflictHandler.FIELD_TYPE_CONFLICT_EXCEPTION_PATTERN.matcher(e.getMessage());
		if (!matcher.find()) {
			this.parent.logWarn(this.log, "Unable to add special field handler for message [" + e.getMessage() + "]");
			return;
		}
		var field = matcher.group("channel");
		var thisType = matcher.group("thisType");
		var requiredType = RequiredType.valueOf(matcher.group("requiredType").toUpperCase());

		if (this.specialCaseFieldHandlers.containsKey(field)) {
			// Special handling had already been added.
			return;
		}

		var handler = this.createAndAddHandler(field, requiredType);

		if (handler == null) {
			this.parent.logWarn(this.log, "Unable to add special field handler for [" + field + "] from [" + thisType
					+ "] to [" + requiredType + "]");
		}
		this.parent.logInfo(this.log,
				"Add special field handler for [" + field + "] from [" + thisType + "] to [" + requiredType + "]");
	}

	private static enum RequiredType {
		STRING, INTEGER, FLOAT;
	}

	private BiConsumer<Point, JsonElement> createAndAddHandler(String field, RequiredType requiredType) {
		var handler = this.createHandler(field, requiredType);
		this.specialCaseFieldHandlers.put(field, handler);
		return handler;
	}

	/**
	 * Creates a Handler for the given field, to convert a Point to a
	 * 'requiredType'.
	 * 
	 * @param field        the field name, i.e. the Channel-Address
	 * @param requiredType the {@link RequiredType
	 * @return
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
