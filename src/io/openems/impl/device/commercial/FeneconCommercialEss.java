/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.impl.device.commercial;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ConfigChannelBuilder;
import io.openems.api.channel.IsChannel;
import io.openems.api.channel.WriteableChannel;
import io.openems.api.device.nature.EssNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.WriteableModbusChannel;
import io.openems.impl.protocol.modbus.internal.ElementBuilder;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.ModbusRange;
import io.openems.impl.protocol.modbus.internal.WritableModbusRange;
import io.openems.impl.protocol.modbus.internal.channel.ModbusChannelBuilder;
import io.openems.impl.protocol.modbus.internal.channel.WriteableModbusChannelBuilder;

public class FeneconCommercialEss extends ModbusDeviceNature implements EssNature {

	@IsChannel(id = "Abnormity1")
	public final ModbusChannel _abnormity1 = new ModbusChannelBuilder().nature(this) //
			.label(1, "DC precharge contactor close unsuccessfully") //
			.label(2, "AC precharge contactor close unsuccessfully") //
			.label(4, "AC main contactor close unsuccessfully") //
			.label(8, "DC electrical breaker 1 close unsuccessfully") //
			.label(16, "DC main contactor close unsuccessfully") //
			.label(32, "AC breaker trip") //
			.label(64, "AC main contactor open when running") //
			.label(128, "DC main contactor open when running") //
			.label(256, "AC main contactor open unsuccessfully") //
			.label(512, "DC electrical breaker 1 open unsuccessfully") //
			.label(1024, "DC main contactor open unsuccessfully") //
			.label(2048, "Hardware PDP fault") //
			.label(4096, "Master stop suddenly").build();
	@IsChannel(id = "Abnormity2")
	public final ModbusChannel _abnormity2 = new ModbusChannelBuilder().nature(this) //
			.label(1, "DC short circuit protection") //
			.label(2, "DC overvoltage protection") //
			.label(4, "DC undervoltage protection") //
			.label(8, "DC inverse/no connection protection") //
			.label(16, "DC disconnection protection") //
			.label(32, "Commuting voltage abnormity protection") //
			.label(64, "DC overcurrent protection") //
			.label(128, "Phase A peak current over limit protection") //
			.label(256, "Phase B peak current over limit protection") //
			.label(512, "Phase C peak current over limit protection") //
			.label(1024, "Phase A grid voltage sampling invalidation") //
			.label(2048, "Phase B virtual current over limit protection") //
			.label(4096, "Phase C virtual current over limit protection") //
			.label(8192, "Phase A grid voltage sampling invalidation") //
			.label(16384, "Phase B grid voltage sampling invalidation") //
			.label(32768, "Phase C grid voltage sampling invalidation").build();
	@IsChannel(id = "Abnormity3")
	public final ModbusChannel _abnormity3 = new ModbusChannelBuilder().nature(this) //
			.label(1, "Phase A invert voltage sampling invalidation") //
			.label(2, "Phase B invert voltage sampling invalidation") //
			.label(4, "Phase C invert voltage sampling invalidation") //
			.label(8, "AC current sampling invalidation") //
			.label(16, "DC current sampling invalidation") //
			.label(32, "Phase A overtemperature protection") //
			.label(64, "Phase B overtemperature protection") //
			.label(128, "Phase C overtemperature protection") //
			.label(256, "Phase A temperature sampling invalidation") //
			.label(512, "Phase B temperature sampling invalidation") //
			.label(1024, "Phase C temperature sampling invalidation") //
			.label(2048, "Phase A precharge unmet protection") //
			.label(4096, "Phase B precharge unmet protection") //
			.label(8192, "Phase C precharge unmet protection") //
			.label(16384, "Unadaptable phase sequence error protection")//
			.label(132768, "DSP protection").build();
	@IsChannel(id = "Abnormity4")
	public final ModbusChannel _abnormity4 = new ModbusChannelBuilder().nature(this) //
			.label(1, "Phase A grid voltage severe overvoltage protection") //
			.label(2, "Phase A grid voltage general overvoltage protection") //
			.label(4, "Phase B grid voltage severe overvoltage protection") //
			.label(8, "Phase B grid voltage general overvoltage protection") //
			.label(16, "Phase C grid voltage severe overvoltage protection") //
			.label(32, "Phase C grid voltage general overvoltage protection") //
			.label(64, "Phase A grid voltage severe undervoltage protection") //
			.label(128, "Phase A grid voltage general undervoltage protection") //
			.label(256, "Phase B grid voltage severe undervoltage protection") //
			.label(512, "Phase B grid voltage general undervoltage protection") //
			.label(1024, "Phase B Inverter voltage general overvoltage protection") //
			.label(2048, "Phase C Inverter voltage severe overvoltage protection") //
			.label(4096, "Phase C Inverter voltage general overvoltage protection") //
			.label(8192, "Inverter peak voltage high protection cause by AC disconnect").build();
	@IsChannel(id = "Abnormity5")
	public final ModbusChannel _abnormity5 = new ModbusChannelBuilder().nature(this) //
			.label(1, "Phase A gird loss") //
			.label(2, "Phase B gird loss") //
			.label(4, "Phase C gird loss") //
			.label(8, "Islanding protection") //
			.label(16, "Phase A under voltage ride through") //
			.label(32, "Phase B under voltage ride through") //
			.label(64, "Phase C under voltage ride through ") //
			.label(128, "Phase A Inverter voltage severe overvoltage protection") //
			.label(256, "Phase A Inverter voltage general overvoltage protection") //
			.label(512, "Phase B Inverter voltage severe overvoltage protection") //
			.label(1024, "Phase B Inverter voltage general overvoltage protection") //
			.label(2048, "Phase C Inverter voltage severe overvoltage protection") //
			.label(4096, "Phase C Inverter voltage general overvoltage protection") //
			.label(8192, "Inverter peak voltage high protection cause by AC disconnect").build();
	@IsChannel(id = "AcChargeEnergy")
	public final ModbusChannel _acChargeEnergy = new ModbusChannelBuilder().nature(this).unit("Wh").multiplier(100)
			.build();

	@IsChannel(id = "AcDischargeEnergy")
	public final ModbusChannel _acDischargeEnergy = new ModbusChannelBuilder().nature(this).unit("Wh").multiplier(100)
			.build();
	public final ModbusChannel _activePower = new ModbusChannelBuilder().nature(this).unit("W").multiplier(100).build();
	@IsChannel(id = "AllowedApparent")
	public final ModbusChannel _allowedApparent = new ModbusChannelBuilder().nature(this).unit("VA").multiplier(100)
			.build();
	private final ModbusChannel _allowedCharge = new ModbusChannelBuilder().nature(this).unit("W").multiplier(100)
			.build();
	private final ModbusChannel _allowedDischarge = new ModbusChannelBuilder().nature(this).unit("W").multiplier(100)
			.build();
	private final ModbusChannel _apparentPower = new ModbusChannelBuilder().nature(this).unit("VA").multiplier(100)
			.build();
	@IsChannel(id = "BatteryAccumulatedCharge")
	public final ModbusChannel _batteryAccumulatedCharge = new ModbusChannelBuilder().nature(this).unit("Wh").build();
	@IsChannel(id = "BatteryAccumulatedDischarge")
	public final ModbusChannel _batteryAccumulatedDischarge = new ModbusChannelBuilder().nature(this).unit("Wh")
			.build();
	@IsChannel(id = "BatteryChargeCycles")
	public final ModbusChannel _batteryChargeCycles = new ModbusChannelBuilder().nature(this).build();
	@IsChannel(id = "BatteryMaintenanceState")
	public final ModbusChannel _batteryMaintenanceState = new ModbusChannelBuilder().nature(this) //
			.label(0, "Off") //
			.label(1, "On").build();
	@IsChannel(id = "BatteryPower")
	public final ModbusChannel _batteryPower = new ModbusChannelBuilder().nature(this).unit("W").multiplier(100)
			.build();
	@IsChannel(id = "BatteryStringTotalCurrent")
	public final ModbusChannel _batteryStringTotalCurrent = new ModbusChannelBuilder().nature(this).unit("mA")
			.multiplier(100).build();
	@IsChannel(id = "BatteryStringAbnormity1")
	public final ModbusChannel _batteryStringAbnormity1 = new ModbusChannelBuilder().nature(this) //
			.label(4, "Battery string voltage sampling route invalidation") //
			.label(16, "Battery string voltage sampling route disconnected") //
			.label(32, "Battery string temperature sampling route disconnected") //
			.label(64, "Battery string inside CAN disconnected") //
			.label(512, "Battery string current sampling circuit abnormity") //
			.label(1024, "Battery string battery cell invalidation") //
			.label(2048, "Battery string main contactor inspection abnormity") //
			.label(4096, "Battery string precharge contactor inspection abnormity") //
			.label(8192, "Battery string negative contactor inspection abnormity") //
			.label(16384, "Battery string power supply relay inspection abnormity")//
			.label(132768, "Battery string middle relay abnormity").build();
	@IsChannel(id = "BatteryStringAbnormity2")
	public final ModbusChannel _batteryStringAbnormity2 = new ModbusChannelBuilder().nature(this) //
			.label(4, "Battery string severe overtemperature") //
			.label(128, "Battery string smog fault") //
			.label(256, "Battery string blown fuse indicator fault") //
			.label(1024, "Battery string general leakage") //
			.label(2048, "Battery string severe leakage") //
			.label(4096, "Communication between BECU and periphery CAN disconnected") //
			.label(16384, "Battery string power supply relay contactor disconnected").build();
	@IsChannel(id = "BatteryStringCellAverageTemperature")
	public final ModbusChannel _batteryStringCellAverageTemperature = new ModbusChannelBuilder().nature(this).unit("°C")
			.multiplier(100).build();
	@IsChannel(id = "BatteryStringChargeCurrentLimit")
	public final ModbusChannel _batteryStringChargeCurrentLimit = new ModbusChannelBuilder().nature(this).unit("mA")
			.multiplier(100).build();
	@IsChannel(id = "BatteryStringDischargeCurrentLimit")
	public final ModbusChannel _batteryStringDischargeCurrentLimit = new ModbusChannelBuilder().nature(this).unit("mA")
			.multiplier(100).build();
	@IsChannel(id = "BatteryStringPeripheralIoState")
	public final ModbusChannel _batteryStringPeripheralIoState = new ModbusChannelBuilder().nature(this)
			.label(1, "Fuse state") //
			.label(2, "Isolated switch state").build();
	@IsChannel(id = "BatteryStringSOH")
	public final ModbusChannel _batteryStringSOH = new ModbusChannelBuilder().nature(this).unit("%").multiplier(100)
			.build();
	@IsChannel(id = "BatteryStringSuggestiveInformation")
	public final ModbusChannel _batteryStringSuggestiveInformation = new ModbusChannelBuilder().nature(this)
			.label(1, "Battery string charge general overcurrent") //
			.label(2, "Battery string discharge general overcurrent") //
			.label(4, "Battery string charge current over limit") //
			.label(8, "Battery string discharge current over limit") //
			.label(16, "Battery string general overvoltage") //
			.label(32, "Battery string general undervoltage") //
			.label(128, "Battery string general over temperature") //
			.label(256, "Battery string general under temperature") //
			.label(1024, "Battery string severe overvoltage") //
			.label(2048, "Battery string severe under voltage") //
			.label(4096, "Battery string severe under temperature") //
			.label(8192, "Battery string charge severe overcurrent") //
			.label(16384, "Battery string discharge severe overcurrent")//
			.label(132768, "Battery string capacity abnormity").build();
	@IsChannel(id = "BatteryStringSwitchState")
	public final ModbusChannel _batteryStringSwitchState = new ModbusChannelBuilder().nature(this)
			.label(1, "Main contactor") //
			.label(2, "Precharge contactor") //
			.label(4, "FAN contactor") //
			.label(8, "BMU power supply relay") //
			.label(16, "Middle relay").build();
	@IsChannel(id = "BatteryStringTotalVoltage")
	public final ModbusChannel _batteryStringTotalVoltage = new ModbusChannelBuilder().nature(this).unit("mV")
			.multiplier(100).build();
	@IsChannel(id = "BatteryStringWorkState")
	public final ModbusChannel _batteryStringWorkState = new ModbusChannelBuilder().nature(this) //
			.label(1, "Initial") //
			.label(2, "Stop") //
			.label(4, "Starting up") //
			.label(8, "Running") //
			.label(16, "Fault").build();
	@IsChannel(id = "ControlMode")
	public final ModbusChannel _controlMode = new ModbusChannelBuilder().nature(this) //
			.label(1, "Remote") //
			.label(2, "Local").build();
	@IsChannel(id = "CurrentPhase1")
	public final ModbusChannel _currentPhase1 = new ModbusChannelBuilder().nature(this).unit("mA").multiplier(100)
			.build();
	@IsChannel(id = "CurrentPhase2")
	public final ModbusChannel _currentPhase2 = new ModbusChannelBuilder().nature(this).unit("mA").multiplier(100)
			.build();
	@IsChannel(id = "CurrentPhase3")
	public final ModbusChannel _currentPhase3 = new ModbusChannelBuilder().nature(this).unit("mA").multiplier(100)
			.build();
	@IsChannel(id = "DCCurrent")
	public final ModbusChannel _dcCurrent = new ModbusChannelBuilder().nature(this).unit("mA").multiplier(100).build();
	@IsChannel(id = "DCPower")
	public final ModbusChannel _dcPower = new ModbusChannelBuilder().nature(this).unit("W").multiplier(100).build();
	@IsChannel(id = "DCVoltage")
	public final ModbusChannel _dcVoltage = new ModbusChannelBuilder().nature(this).unit("mV").multiplier(100).build();
	@IsChannel(id = "Frequency")
	public final ModbusChannel _frequency = new ModbusChannelBuilder().nature(this).unit("mHZ").multiplier(10).build();
	private final ModbusChannel _gridMode = new ModbusChannelBuilder().nature(this) //
			.label(1, OFF_GRID) //
			.label(2, ON_GRID).build();
	@IsChannel(id = "InverterActivePower")
	public final ModbusChannel _inverterActivePower = new ModbusChannelBuilder().nature(this).unit("W").multiplier(100)
			.build();
	@IsChannel(id = "InverterCurrentPhase1")
	public final ModbusChannel _inverterCurrentPhase1 = new ModbusChannelBuilder().nature(this).unit("mA")
			.multiplier(100).build();
	@IsChannel(id = "InverterCurrentPhase2")
	public final ModbusChannel _inverterCurrentPhase2 = new ModbusChannelBuilder().nature(this).unit("mA")
			.multiplier(100).build();
	@IsChannel(id = "InverterCurrentPhase3")
	public final ModbusChannel _inverterCurrentPhase3 = new ModbusChannelBuilder().nature(this).unit("mA")
			.multiplier(100).build();
	@IsChannel(id = "InverterState")
	public final ModbusChannel _inverterState = new ModbusChannelBuilder().nature(this) //
			.label(0, "Init") //
			.label(2, "Fault") //
			.label(4, STOP) //
			.label(8, STANDBY) //
			.label(16, "Grid-Monitor") // ,
			.label(32, "Ready") //
			.label(64, START) //
			.label(128, "Debug").build();
	@IsChannel(id = "InverterVoltagePhase1")
	public final ModbusChannel _inverterVoltagePhase1 = new ModbusChannelBuilder().nature(this).unit("mV")
			.multiplier(100).build();
	@IsChannel(id = "InverterVoltagePhase2")
	public final ModbusChannel _inverterVoltagePhase2 = new ModbusChannelBuilder().nature(this).unit("mV")
			.multiplier(100).build();
	@IsChannel(id = "InverterVoltagePhase3")
	public final ModbusChannel _inverterVoltagePhase3 = new ModbusChannelBuilder().nature(this).unit("mV")
			.multiplier(100).build();
	@IsChannel(id = "IPMPhaseATemperature")
	public final ModbusChannel _ipmPhaseATemperature = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "IPMPhaseBTemperature")
	public final ModbusChannel _ipmPhaseBTemperature = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "IPMPhaseCTemperature")
	public final ModbusChannel _ipmPhaseCTemperature = new ModbusChannelBuilder().nature(this).unit("°C").build();
	private final ConfigChannel _minSoc = new ConfigChannelBuilder().nature(this).defaultValue(DEFAULT_MINSOC)
			.percentType().build();
	@IsChannel(id = "ProtocolVersion")
	public final ModbusChannel _protocolVersion = new ModbusChannelBuilder().nature(this).build();
	private final ModbusChannel _reactivePower = new ModbusChannelBuilder().nature(this).unit("var").multiplier(100)
			.build();
	private final WriteableModbusChannel _setActivePower = new WriteableModbusChannelBuilder().nature(this).unit("W")
			.multiplier(100).minWriteValue(_allowedCharge).maxWriteValue(_allowedDischarge).build();

	@IsChannel(id = "Abnormity1")
	public final WriteableModbusChannel _setReactivePower = new WriteableModbusChannelBuilder().nature(this).unit("var")
			.multiplier(100).minWriteValue(_allowedCharge).maxWriteValue(_allowedDischarge).build();
	private final WriteableModbusChannel _setWorkState = new WriteableModbusChannelBuilder().nature(this) //
			.label(4, STOP) //
			.label(32, STANDBY) //
			.label(64, START).build();
	private final ModbusChannel _soc = new ModbusChannelBuilder().nature(this).percentType().build();
	@IsChannel(id = "SuggestiveInformation1")
	public final ModbusChannel _suggestiveInformation1 = new ModbusChannelBuilder().nature(this) //
			.label(4, "EmergencyStop") //
			.label(64, "KeyManualStop").build();
	@IsChannel(id = "SuggestiveInformation2")
	public final ModbusChannel _suggestiveInformation2 = new ModbusChannelBuilder().nature(this) //
			.label(4, "EmergencyStop") //
			.label(64, "KeyManualStop").build();
	@IsChannel(id = "SuggestiveInformation3")
	public final ModbusChannel _suggestiveInformation3 = new ModbusChannelBuilder().nature(this) //
			.label(1, "Inverter communication abnormity") //
			.label(2, "Battery stack communication abnormity") //
			.label(4, "Multifunctional ammeter communication abnormity") //
			.label(16, "Remote communication abnormity").build();
	@IsChannel(id = "SuggestiveInformation4")
	public final ModbusChannel _suggestiveInformation4 = new ModbusChannelBuilder().nature(this) //
			.label(8, "Transformer severe overtemperature").build();
	@IsChannel(id = "SuggestiveInformation5")
	public final ModbusChannel _suggestiveInformation5 = new ModbusChannelBuilder().nature(this) //
			.label(1, "DC precharge contactor inspection abnormity") //
			.label(2, "DC breaker 1 inspection abnormity ") //
			.label(4, "DC breaker 2 inspection abnormity ") //
			.label(8, "AC precharge contactor inspection abnormity ") //
			.label(16, "AC main contactor inspection abnormity ") //
			.label(32, "AC breaker inspection abnormity ") //
			.label(64, "DC breaker 1 close unsuccessfully") //
			.label(128, "DC breaker 2 close unsuccessfully") //
			.label(256, "Control signal close abnormally inspected by system") //
			.label(512, "Control signal open abnormally inspected by system") //
			.label(1024, "Neutral wire contactor close unsuccessfully") //
			.label(2048, "Neutral wire contactor open unsuccessfully") //
			.label(4096, "Work door open") //
			.label(8192, "Emergency stop") //
			.label(16384, "AC breaker close unsuccessfully")//
			.label(132768, "Control switch stop").build();
	@IsChannel(id = "SuggestiveInformation6")
	public final ModbusChannel _suggestiveInformation6 = new ModbusChannelBuilder().nature(this) //
			.label(1, "General overload") //
			.label(2, "Severe overload") //
			.label(4, "Battery current over limit") //
			.label(8, "Power decrease caused by overtemperature") //
			.label(16, "Inverter general overtemperature") //
			.label(32, "AC three-phase current unbalance") //
			.label(64, "Rstore factory setting unsuccessfully") //
			.label(128, "Pole-board invalidation") //
			.label(256, "Self-inspection failed") //
			.label(512, "Receive BMS fault and stop") //
			.label(1024, "Refrigeration equipment invalidation") //
			.label(2048, "Large temperature difference among IGBT three phases") //
			.label(4096, "EEPROM parameters over range") //
			.label(8192, "EEPROM parameters backup failed") //
			.label(16384, "DC breaker close unsuccessfully").build();
	@IsChannel(id = "SuggestiveInformation7")
	public final ModbusChannel _suggestiveInformation7 = new ModbusChannelBuilder().nature(this) //
			.label(1, "Communication between inverter and BSMU disconnected") //
			.label(2, "Communication between inverter and Master disconnected") //
			.label(4, "Communication between inverter and UC disconnected") //
			.label(8, "BMS start overtime controlled by PCS") //
			.label(16, "BMS stop overtime controlled by PCS") //
			.label(32, "Sync signal invalidation") //
			.label(64, "Sync signal continuous caputure fault") //
			.label(128, "Sync signal several times caputure fault").build();
	@IsChannel(id = "SwitchState")
	public final ModbusChannel _switchState = new ModbusChannelBuilder().nature(this) //
			.label(2, "DC main contactor state") //
			.label(4, "DC precharge contactor state") //
			.label(8, "AC breaker state") //
			.label(16, "AC main contactor state") //
			.label(32, "AC precharge contactor state").build();
	@IsChannel(id = "SystemManufacturer")
	public final ModbusChannel _systemManufacturer = new ModbusChannelBuilder().nature(this) //
			.label(1, "BYD").build();
	private final ModbusChannel _systemState = new ModbusChannelBuilder().nature(this) //
			.label(2, STOP) //
			.label(4, "PV-Charge") //
			.label(8, "Standby") //
			.label(16, START) //
			.label(32, "Fault") //
			.label(64, "Debug").build();
	@IsChannel(id = "SystemType")
	public final ModbusChannel _systemType = new ModbusChannelBuilder().nature(this) //
			.label(1, "CESS").build();
	@IsChannel(id = "TotalDateEnergy")
	public final ModbusChannel _totalDateEnergy = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalEnergy")
	public final ModbusChannel _totalEnergy = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy0")
	public final ModbusChannel _totalHourEnergy0 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy1")
	public final ModbusChannel _totalHourEnergy1 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy10")
	public final ModbusChannel _totalHourEnergy10 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy11")
	public final ModbusChannel _totalHourEnergy11 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy12")
	public final ModbusChannel _totalHourEnergy12 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy13")
	public final ModbusChannel _totalHourEnergy13 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy14")
	public final ModbusChannel _totalHourEnergy14 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy15")
	public final ModbusChannel _totalHourEnergy15 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy16")
	public final ModbusChannel _totalHourEnergy16 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy17")
	public final ModbusChannel _totalHourEnergy17 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy18")
	public final ModbusChannel _totalHourEnergy18 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy19")
	public final ModbusChannel _totalHourEnergy19 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy2")
	public final ModbusChannel _totalHourEnergy2 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy20")
	public final ModbusChannel _totalHourEnergy20 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy21")
	public final ModbusChannel _totalHourEnergy21 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy22")
	public final ModbusChannel _totalHourEnergy22 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy23")
	public final ModbusChannel _totalHourEnergy23 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy3")
	public final ModbusChannel _totalHourEnergy3 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy4")
	public final ModbusChannel _totalHourEnergy4 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy5")
	public final ModbusChannel _totalHourEnergy5 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy6")
	public final ModbusChannel _totalHourEnergy6 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy7")
	public final ModbusChannel _totalHourEnergy7 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy8")
	public final ModbusChannel _totalHourEnergy8 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalHourEnergy9")
	public final ModbusChannel _totalHourEnergy9 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalMonthEnergy")
	public final ModbusChannel _totalMonthEnergy = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TotalYearEnergy")
	public final ModbusChannel _totalYearEnergy = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	@IsChannel(id = "TransformerPhaseBTemperature")
	public final ModbusChannel _transformerPhaseBTemperature = new ModbusChannelBuilder().nature(this).unit("°C")
			.build();
	@IsChannel(id = "VoltagePhase1")
	public final ModbusChannel _voltagePhase1 = new ModbusChannelBuilder().nature(this).unit("mV").multiplier(100)
			.build();
	@IsChannel(id = "VoltagePhase2")
	public final ModbusChannel _voltagePhase2 = new ModbusChannelBuilder().nature(this).unit("mV").multiplier(100)
			.build();
	@IsChannel(id = "VoltagePhase3")
	public final ModbusChannel _voltagePhase3 = new ModbusChannelBuilder().nature(this).unit("mV").multiplier(100)
			.build();
	@IsChannel(id = "MaxVoltageCellNo")
	public final ModbusChannel _maxVoltageCellNo = new ModbusChannelBuilder().nature(this).build();
	@IsChannel(id = "MaxVoltageCellVoltage")
	public final ModbusChannel _maxVoltageCellVoltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "MaxVoltageCellTemp")
	public final ModbusChannel _maxVoltageCellTemp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "MinVoltageCellNo")
	public final ModbusChannel _minVoltageCellNo = new ModbusChannelBuilder().nature(this).build();
	@IsChannel(id = "MinVoltageCellVoltage")
	public final ModbusChannel _minVoltageCellVoltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "MinVoltageCellTemp")
	public final ModbusChannel _minVoltageCellTemp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "MaxTempCellNo")
	public final ModbusChannel _maxTempCellNo = new ModbusChannelBuilder().nature(this).build();
	@IsChannel(id = "MaxTempCellVoltage")
	public final ModbusChannel _maxTempCellVoltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "MaxTempCellTemp")
	public final ModbusChannel _maxTempCellTemp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "MinTempCellNo")
	public final ModbusChannel _minTempCellNo = new ModbusChannelBuilder().nature(this).build();
	@IsChannel(id = "MinTempCellVoltage")
	public final ModbusChannel _minTempCellVoltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "MinTempCellTemp")
	public final ModbusChannel _minTempCellTemp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell1Voltage")
	public final ModbusChannel _cell1Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell2Voltage")
	public final ModbusChannel _cell2Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell3Voltage")
	public final ModbusChannel _cell3Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell4Voltage")
	public final ModbusChannel _cell4Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell5Voltage")
	public final ModbusChannel _cell5Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell6Voltage")
	public final ModbusChannel _cell6Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell7Voltage")
	public final ModbusChannel _cell7Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell8Voltage")
	public final ModbusChannel _cell8Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell9Voltage")
	public final ModbusChannel _cell9Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell10Voltage")
	public final ModbusChannel _cell10Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell11Voltage")
	public final ModbusChannel _cell11Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell12Voltage")
	public final ModbusChannel _cell12Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell13Voltage")
	public final ModbusChannel _cell13Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell14Voltage")
	public final ModbusChannel _cell14Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell15Voltage")
	public final ModbusChannel _cell15Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell16Voltage")
	public final ModbusChannel _cell16Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell17Voltage")
	public final ModbusChannel _cell17Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell18Voltage")
	public final ModbusChannel _cell18Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell19Voltage")
	public final ModbusChannel _cell19Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell20Voltage")
	public final ModbusChannel _cell20Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell21Voltage")
	public final ModbusChannel _cell21Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell22Voltage")
	public final ModbusChannel _cell22Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell23Voltage")
	public final ModbusChannel _cell23Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell24Voltage")
	public final ModbusChannel _cell24Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell25Voltage")
	public final ModbusChannel _cell25Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell26Voltage")
	public final ModbusChannel _cell26Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell27Voltage")
	public final ModbusChannel _cell27Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell28Voltage")
	public final ModbusChannel _cell28Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell29Voltage")
	public final ModbusChannel _cell29Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell30Voltage")
	public final ModbusChannel _cell30Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell31Voltage")
	public final ModbusChannel _cell31Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell32Voltage")
	public final ModbusChannel _cell32Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell33Voltage")
	public final ModbusChannel _cell33Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell34Voltage")
	public final ModbusChannel _cell34Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell35Voltage")
	public final ModbusChannel _cell35Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell36Voltage")
	public final ModbusChannel _cell36Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell37Voltage")
	public final ModbusChannel _cell37Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell38Voltage")
	public final ModbusChannel _cell38Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell39Voltage")
	public final ModbusChannel _cell39Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell40Voltage")
	public final ModbusChannel _cell40Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell41Voltage")
	public final ModbusChannel _cell41Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell42Voltage")
	public final ModbusChannel _cell42Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell43Voltage")
	public final ModbusChannel _cell43Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell44Voltage")
	public final ModbusChannel _cell44Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell45Voltage")
	public final ModbusChannel _cell45Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell46Voltage")
	public final ModbusChannel _cell46Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell47Voltage")
	public final ModbusChannel _cell47Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell48Voltage")
	public final ModbusChannel _cell48Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell49Voltage")
	public final ModbusChannel _cell49Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell50Voltage")
	public final ModbusChannel _cell50Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell51Voltage")
	public final ModbusChannel _cell51Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell52Voltage")
	public final ModbusChannel _cell52Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell53Voltage")
	public final ModbusChannel _cell53Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell54Voltage")
	public final ModbusChannel _cell54Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell55Voltage")
	public final ModbusChannel _cell55Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell56Voltage")
	public final ModbusChannel _cell56Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell57Voltage")
	public final ModbusChannel _cell57Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell58Voltage")
	public final ModbusChannel _cell58Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell59Voltage")
	public final ModbusChannel _cell59Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell60Voltage")
	public final ModbusChannel _cell60Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell61Voltage")
	public final ModbusChannel _cell61Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell62Voltage")
	public final ModbusChannel _cell62Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell63Voltage")
	public final ModbusChannel _cell63Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();
	@IsChannel(id = "Cell64Voltage")
	public final ModbusChannel _cell64Voltage = new ModbusChannelBuilder().nature(this).unit("mV").build();

	@IsChannel(id = "Cell1Temp")
	public final ModbusChannel _cell1Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell2Temp")
	public final ModbusChannel _cell2Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell3Temp")
	public final ModbusChannel _cell3Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell4Temp")
	public final ModbusChannel _cell4Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell5Temp")
	public final ModbusChannel _cell5Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell6Temp")
	public final ModbusChannel _cell6Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell7Temp")
	public final ModbusChannel _cell7Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell8Temp")
	public final ModbusChannel _cell8Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell9Temp")
	public final ModbusChannel _cell9Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell10Temp")
	public final ModbusChannel _cell10Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell11Temp")
	public final ModbusChannel _cell11Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell12Temp")
	public final ModbusChannel _cell12Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell13Temp")
	public final ModbusChannel _cell13Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell14Temp")
	public final ModbusChannel _cell14Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell15Temp")
	public final ModbusChannel _cell15Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell16Temp")
	public final ModbusChannel _cell16Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell17Temp")
	public final ModbusChannel _cell17Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell18Temp")
	public final ModbusChannel _cell18Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell19Temp")
	public final ModbusChannel _cell19Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell20Temp")
	public final ModbusChannel _cell20Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell21Temp")
	public final ModbusChannel _cell21Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell22Temp")
	public final ModbusChannel _cell22Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell23Temp")
	public final ModbusChannel _cell23Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell24Temp")
	public final ModbusChannel _cell24Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell25Temp")
	public final ModbusChannel _cell25Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell26Temp")
	public final ModbusChannel _cell26Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell27Temp")
	public final ModbusChannel _cell27Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell28Temp")
	public final ModbusChannel _cell28Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell29Temp")
	public final ModbusChannel _cell29Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell30Temp")
	public final ModbusChannel _cell30Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell31Temp")
	public final ModbusChannel _cell31Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell32Temp")
	public final ModbusChannel _cell32Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell33Temp")
	public final ModbusChannel _cell33Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell34Temp")
	public final ModbusChannel _cell34Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell35Temp")
	public final ModbusChannel _cell35Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell36Temp")
	public final ModbusChannel _cell36Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell37Temp")
	public final ModbusChannel _cell37Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell38Temp")
	public final ModbusChannel _cell38Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell39Temp")
	public final ModbusChannel _cell39Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell40Temp")
	public final ModbusChannel _cell40Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell41Temp")
	public final ModbusChannel _cell41Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell42Temp")
	public final ModbusChannel _cell42Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell43Temp")
	public final ModbusChannel _cell43Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell44Temp")
	public final ModbusChannel _cell44Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell45Temp")
	public final ModbusChannel _cell45Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell46Temp")
	public final ModbusChannel _cell46Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell47Temp")
	public final ModbusChannel _cell47Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell48Temp")
	public final ModbusChannel _cell48Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell49Temp")
	public final ModbusChannel _cell49Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell50Temp")
	public final ModbusChannel _cell50Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell51Temp")
	public final ModbusChannel _cell51Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell52Temp")
	public final ModbusChannel _cell52Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell53Temp")
	public final ModbusChannel _cell53Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell54Temp")
	public final ModbusChannel _cell54Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell55Temp")
	public final ModbusChannel _cell55Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell56Temp")
	public final ModbusChannel _cell56Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell57Temp")
	public final ModbusChannel _cell57Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell58Temp")
	public final ModbusChannel _cell58Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell59Temp")
	public final ModbusChannel _cell59Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell60Temp")
	public final ModbusChannel _cell60Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell61Temp")
	public final ModbusChannel _cell61Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell62Temp")
	public final ModbusChannel _cell62Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell63Temp")
	public final ModbusChannel _cell63Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();
	@IsChannel(id = "Cell64Temp")
	public final ModbusChannel _cell64Temp = new ModbusChannelBuilder().nature(this).unit("°C").build();

	public FeneconCommercialEss(String thingId) {
		super(thingId);
	}

	@Override
	public Channel activePower() {
		return _activePower;
	}

	@Override
	public Channel allowedCharge() {
		return _allowedCharge;
	}

	@Override
	public Channel allowedDischarge() {
		return _allowedDischarge;
	}

	@Override
	public Channel apparentPower() {
		return _apparentPower;
	}

	@Override
	public Channel gridMode() {
		return _gridMode;
	}

	@Override
	public Channel minSoc() {
		return _minSoc;
	}

	@Override
	public Channel reactivePower() {
		return _reactivePower;
	}

	@Override
	public WriteableChannel setActivePower() {
		return _setActivePower;
	}

	@Override
	public void setMinSoc(Integer minSoc) {
		this._minSoc.updateValue(Long.valueOf(minSoc));
	}

	@Override
	public WriteableChannel setWorkState() {
		return _setWorkState;
	}

	@Override
	public Channel soc() {
		return _soc;
	}

	@Override
	public Channel systemState() {
		return _systemState;
	}

	@Override
	public String toString() {
		return "FeneconCommercialEss [setActivePower=" + _setActivePower + ", minSoc=" + _minSoc + ", soc=" + _soc
				+ "]";
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		return new ModbusProtocol( //
				new ModbusRange(0x0101, //
						new ElementBuilder().address(0x0101).channel(_systemState).build(), //
						new ElementBuilder().address(0x0102).channel(_controlMode).build(), //
						new ElementBuilder().address(0x0103).dummy().build(), // WorkMode: RemoteDispatch
						new ElementBuilder().address(0x0104).channel(_batteryMaintenanceState).build(), //
						new ElementBuilder().address(0x0105).channel(_inverterState).build(), //
						new ElementBuilder().address(0x0106).channel(_gridMode).build(), //
						new ElementBuilder().address(0x0107).dummy(0x0108 - 0x0107).build(), //
						new ElementBuilder().address(0x0108).channel(_protocolVersion).build(), //
						new ElementBuilder().address(0x0109).channel(_systemManufacturer).build(), //
						new ElementBuilder().address(0x010A).channel(_systemType).build(), //
						new ElementBuilder().address(0x010B).dummy(0x0110 - 0x010B).build(), //
						new ElementBuilder().address(0x0110).channel(_suggestiveInformation1).build(), //
						new ElementBuilder().address(0x0111).channel(_suggestiveInformation2).build(), //
						new ElementBuilder().address(0x0112).dummy(0x0125 - 0x0112).build(), //
						new ElementBuilder().address(0x0125).channel(_suggestiveInformation3).build(), //
						new ElementBuilder().address(0x0126).channel(_suggestiveInformation4).build(), //
						new ElementBuilder().address(0x0127).dummy(0x0150 - 0x0127).build(), //
						new ElementBuilder().address(0x0150).channel(_switchState).build()//
				), //
				new ModbusRange(0x0180, //
						new ElementBuilder().address(0x0180).channel(_abnormity1).build(), //
						new ElementBuilder().address(0x0181).dummy(0x0182 - 0x0181).build(), //
						new ElementBuilder().address(0x0182).channel(_abnormity2).build(), //
						new ElementBuilder().address(0x0183).channel(_abnormity3).build(), //
						new ElementBuilder().address(0x0184).channel(_abnormity4).build(), //
						new ElementBuilder().address(0x0185).channel(_abnormity5).build(), //
						new ElementBuilder().address(0x0186).channel(_suggestiveInformation5).build(), //
						new ElementBuilder().address(0x0187).channel(_suggestiveInformation6).build(), //
						new ElementBuilder().address(0x0188).channel(_suggestiveInformation7).build()),
				new ModbusRange(0x0200, //
						new ElementBuilder().address(0x0200).channel(_dcVoltage).signed().build(),
						new ElementBuilder().address(0x0201).channel(_dcCurrent).signed().build(),
						new ElementBuilder().address(0x0202).channel(_dcPower).signed().build(),
						new ElementBuilder().address(0x0203).dummy(0x0208 - 0x0203).build(),
						new ElementBuilder().address(0x0208).channel(_acChargeEnergy).doubleword().build(),
						new ElementBuilder().address(0x020A).channel(_acDischargeEnergy).doubleword().build(),
						new ElementBuilder().address(0x020C).dummy(0x0210 - 0x020C).build(),
						new ElementBuilder().address(0x0210).channel(_activePower).signed().build(),
						new ElementBuilder().address(0x0211).channel(_reactivePower).signed().build(),
						new ElementBuilder().address(0x0212).channel(_apparentPower).build(),
						new ElementBuilder().address(0x0213).channel(_currentPhase1).signed().build(),
						new ElementBuilder().address(0x0214).channel(_currentPhase2).signed().build(),
						new ElementBuilder().address(0x0215).channel(_currentPhase3).signed().build(),
						new ElementBuilder().address(0x0216).dummy(0x219 - 0x216).build(),
						new ElementBuilder().address(0x0219).channel(_voltagePhase1).build(),
						new ElementBuilder().address(0x021A).channel(_voltagePhase2).build(),
						new ElementBuilder().address(0x021B).channel(_voltagePhase3).build(),
						new ElementBuilder().address(0x021C).channel(_frequency).build()),
				new ModbusRange(0x0222, //
						new ElementBuilder().address(0x0222).channel(_inverterVoltagePhase1).build(),
						new ElementBuilder().address(0x0223).channel(_inverterVoltagePhase2).build(),
						new ElementBuilder().address(0x0224).channel(_inverterVoltagePhase3).build(),
						new ElementBuilder().address(0x0225).channel(_inverterCurrentPhase1).build(),
						new ElementBuilder().address(0x0226).channel(_inverterCurrentPhase2).build(),
						new ElementBuilder().address(0x0227).channel(_inverterCurrentPhase3).build(),
						new ElementBuilder().address(0x0228).channel(_inverterActivePower).signed().build(),
						new ElementBuilder().address(0x0229).dummy(0x230 - 0x229).build(),
						new ElementBuilder().address(0x0230).channel(_allowedCharge).signed().build(),
						new ElementBuilder().address(0x0231).channel(_allowedDischarge).build(),
						new ElementBuilder().address(0x0232).channel(_allowedApparent).build(),
						new ElementBuilder().address(0x0233).dummy(0x240 - 0x233).build(),
						new ElementBuilder().address(0x0240).channel(_ipmPhaseATemperature).signed().build(),
						new ElementBuilder().address(0x0241).channel(_ipmPhaseBTemperature).signed().build(),
						new ElementBuilder().address(0x0242).channel(_ipmPhaseCTemperature).signed().build(),
						new ElementBuilder().address(0x0243).dummy(0x0249 - 0x0243).build(),
						new ElementBuilder().address(0x0249).channel(_transformerPhaseBTemperature).signed().build()),
				new ModbusRange(0x0300, //
						new ElementBuilder().address(0x0300).channel(_totalEnergy).doubleword().build(),
						new ElementBuilder().address(0x0302).channel(_totalYearEnergy).doubleword().build(),
						new ElementBuilder().address(0x0304).channel(_totalMonthEnergy).doubleword().build(),
						new ElementBuilder().address(0x0306).channel(_totalDateEnergy).build(),
						new ElementBuilder().address(0x0307).channel(_totalHourEnergy0).build(),
						new ElementBuilder().address(0x0308).channel(_totalHourEnergy1).build(),
						new ElementBuilder().address(0x0309).channel(_totalHourEnergy2).build(),
						new ElementBuilder().address(0x030A).channel(_totalHourEnergy3).build(),
						new ElementBuilder().address(0x030B).channel(_totalHourEnergy4).build(),
						new ElementBuilder().address(0x030C).channel(_totalHourEnergy5).build(),
						new ElementBuilder().address(0x030D).channel(_totalHourEnergy6).build(),
						new ElementBuilder().address(0x030E).channel(_totalHourEnergy7).build(),
						new ElementBuilder().address(0x030F).channel(_totalHourEnergy8).build(),
						new ElementBuilder().address(0x0310).channel(_totalHourEnergy9).build(),
						new ElementBuilder().address(0x0311).channel(_totalHourEnergy10).build(),
						new ElementBuilder().address(0x0312).channel(_totalHourEnergy11).build(),
						new ElementBuilder().address(0x0313).channel(_totalHourEnergy12).build(),
						new ElementBuilder().address(0x0314).channel(_totalHourEnergy13).build(),
						new ElementBuilder().address(0x0315).channel(_totalHourEnergy14).build(),
						new ElementBuilder().address(0x0316).channel(_totalHourEnergy15).build(),
						new ElementBuilder().address(0x0317).channel(_totalHourEnergy16).build(),
						new ElementBuilder().address(0x0318).channel(_totalHourEnergy17).build(),
						new ElementBuilder().address(0x0319).channel(_totalHourEnergy18).build(),
						new ElementBuilder().address(0x031A).channel(_totalHourEnergy19).build(),
						new ElementBuilder().address(0x031B).channel(_totalHourEnergy20).build(),
						new ElementBuilder().address(0x031C).channel(_totalHourEnergy21).build(),
						new ElementBuilder().address(0x031D).channel(_totalHourEnergy22).build(),
						new ElementBuilder().address(0x031E).channel(_totalHourEnergy23).build()),
				new WritableModbusRange(0x0500, //
						new ElementBuilder().address(0x0500).channel(_setWorkState).build(),
						new ElementBuilder().address(0x0501).channel(_setActivePower).signed().build(), //
						new ElementBuilder().address(0x0502).channel(_setReactivePower).signed().build()),
				new ModbusRange(0x1100, //
						new ElementBuilder().address(0x1100).channel(_batteryStringWorkState).build(),
						new ElementBuilder().address(0x1101).channel(_batteryStringSwitchState).build(),
						new ElementBuilder().address(0x1102).channel(_batteryStringPeripheralIoState).build(),
						new ElementBuilder().address(0x1103).channel(_batteryStringSuggestiveInformation).build(),
						new ElementBuilder().address(0x1104).dummy().build(),
						new ElementBuilder().address(0x1105).channel(_batteryStringAbnormity1).build(),
						new ElementBuilder().address(0x1106).channel(_batteryStringAbnormity2).build()),
				new ModbusRange(0x1400, //
						new ElementBuilder().address(0x1400).channel(_batteryStringTotalVoltage).build(),
						new ElementBuilder().address(0x1401).channel(_batteryStringTotalCurrent).signed().build(),
						new ElementBuilder().address(0x1402).channel(_soc).build(),
						new ElementBuilder().address(0x1403).channel(_batteryStringSOH).build(),
						new ElementBuilder().address(0x1404).channel(_batteryStringCellAverageTemperature).signed()
								.build(),
						new ElementBuilder().address(0x1405).dummy().build(),
						new ElementBuilder().address(0x1406).channel(_batteryStringChargeCurrentLimit).build(),
						new ElementBuilder().address(0x1407).channel(_batteryStringDischargeCurrentLimit).build(),
						new ElementBuilder().address(0x1408).dummy(0x140A - 0x1408).build(),
						new ElementBuilder().address(0x140A).channel(_batteryChargeCycles).doubleword().build(),
						new ElementBuilder().address(0x140C).dummy(0x1418 - 0x140C).build(),
						new ElementBuilder().address(0x1418).channel(_batteryAccumulatedCharge).doubleword().build(),
						new ElementBuilder().address(0x141A).channel(_batteryAccumulatedDischarge).doubleword().build(),
						new ElementBuilder().address(0x141C).dummy(0x1420 - 0x141C).build(),
						new ElementBuilder().address(0x1420).channel(_batteryPower).signed().build(),
						new ElementBuilder().address(0x1421).dummy(0x1430 - 0x1421).build(),
						new ElementBuilder().address(0x1430).channel(_maxVoltageCellNo).build(),
						new ElementBuilder().address(0x1431).channel(_maxVoltageCellVoltage).build(),
						new ElementBuilder().address(0x1432).channel(_maxVoltageCellTemp).signed().build(),
						new ElementBuilder().address(0x1433).channel(_minVoltageCellNo).build(),
						new ElementBuilder().address(0x1434).channel(_minVoltageCellVoltage).build(),
						new ElementBuilder().address(0x1435).channel(_minVoltageCellTemp).signed().build(),
						new ElementBuilder().address(0x1436).dummy(0x143A - 0x1436).build(),
						new ElementBuilder().address(0x143A).channel(_maxTempCellNo).build(),
						new ElementBuilder().address(0x143B).channel(_maxTempCellTemp).signed().build(),
						new ElementBuilder().address(0x143C).channel(_maxTempCellVoltage).build(),
						new ElementBuilder().address(0x143D).channel(_minTempCellNo).build(),
						new ElementBuilder().address(0x143E).channel(_minTempCellTemp).signed().build(),
						new ElementBuilder().address(0x143F).channel(_minTempCellVoltage).build()), //
				new ModbusRange(0x1500, new ElementBuilder().address(0x1500).channel(_cell1Voltage).build(),
						new ElementBuilder().address(0x1501).channel(_cell2Voltage).build(),
						new ElementBuilder().address(0x1502).channel(_cell3Voltage).build(),
						new ElementBuilder().address(0x1503).channel(_cell4Voltage).build(),
						new ElementBuilder().address(0x1504).channel(_cell5Voltage).build(),
						new ElementBuilder().address(0x1505).channel(_cell6Voltage).build(),
						new ElementBuilder().address(0x1506).channel(_cell7Voltage).build(),
						new ElementBuilder().address(0x1507).channel(_cell8Voltage).build(),
						new ElementBuilder().address(0x1508).channel(_cell9Voltage).build(),
						new ElementBuilder().address(0x1509).channel(_cell10Voltage).build(),
						new ElementBuilder().address(0x150a).channel(_cell11Voltage).build(),
						new ElementBuilder().address(0x150b).channel(_cell12Voltage).build(),
						new ElementBuilder().address(0x150c).channel(_cell13Voltage).build(),
						new ElementBuilder().address(0x150d).channel(_cell14Voltage).build(),
						new ElementBuilder().address(0x150e).channel(_cell15Voltage).build(),
						new ElementBuilder().address(0x150f).channel(_cell16Voltage).build(),
						new ElementBuilder().address(0x1510).channel(_cell17Voltage).build(),
						new ElementBuilder().address(0x1511).channel(_cell18Voltage).build(),
						new ElementBuilder().address(0x1512).channel(_cell19Voltage).build(),
						new ElementBuilder().address(0x1513).channel(_cell20Voltage).build(),
						new ElementBuilder().address(0x1514).channel(_cell21Voltage).build(),
						new ElementBuilder().address(0x1515).channel(_cell22Voltage).build(),
						new ElementBuilder().address(0x1516).channel(_cell23Voltage).build(),
						new ElementBuilder().address(0x1517).channel(_cell24Voltage).build(),
						new ElementBuilder().address(0x1518).channel(_cell25Voltage).build(),
						new ElementBuilder().address(0x1519).channel(_cell26Voltage).build(),
						new ElementBuilder().address(0x151a).channel(_cell27Voltage).build(),
						new ElementBuilder().address(0x151b).channel(_cell28Voltage).build(),
						new ElementBuilder().address(0x151c).channel(_cell29Voltage).build(),
						new ElementBuilder().address(0x151d).channel(_cell30Voltage).build(),
						new ElementBuilder().address(0x151e).channel(_cell31Voltage).build(),
						new ElementBuilder().address(0x151f).channel(_cell32Voltage).build(),
						new ElementBuilder().address(0x1520).channel(_cell33Voltage).build(),
						new ElementBuilder().address(0x1521).channel(_cell34Voltage).build(),
						new ElementBuilder().address(0x1522).channel(_cell35Voltage).build(),
						new ElementBuilder().address(0x1523).channel(_cell36Voltage).build(),
						new ElementBuilder().address(0x1524).channel(_cell37Voltage).build(),
						new ElementBuilder().address(0x1525).channel(_cell38Voltage).build(),
						new ElementBuilder().address(0x1526).channel(_cell39Voltage).build(),
						new ElementBuilder().address(0x1527).channel(_cell40Voltage).build(),
						new ElementBuilder().address(0x1528).channel(_cell41Voltage).build(),
						new ElementBuilder().address(0x1529).channel(_cell42Voltage).build(),
						new ElementBuilder().address(0x152a).channel(_cell43Voltage).build(),
						new ElementBuilder().address(0x152b).channel(_cell44Voltage).build(),
						new ElementBuilder().address(0x152c).channel(_cell45Voltage).build(),
						new ElementBuilder().address(0x152d).channel(_cell46Voltage).build(),
						new ElementBuilder().address(0x152e).channel(_cell47Voltage).build(),
						new ElementBuilder().address(0x152f).channel(_cell48Voltage).build(),
						new ElementBuilder().address(0x1530).channel(_cell49Voltage).build(),
						new ElementBuilder().address(0x1531).channel(_cell50Voltage).build(),
						new ElementBuilder().address(0x1532).channel(_cell51Voltage).build(),
						new ElementBuilder().address(0x1533).channel(_cell52Voltage).build(),
						new ElementBuilder().address(0x1534).channel(_cell53Voltage).build(),
						new ElementBuilder().address(0x1535).channel(_cell54Voltage).build(),
						new ElementBuilder().address(0x1536).channel(_cell55Voltage).build(),
						new ElementBuilder().address(0x1537).channel(_cell56Voltage).build(),
						new ElementBuilder().address(0x1538).channel(_cell57Voltage).build(),
						new ElementBuilder().address(0x1539).channel(_cell58Voltage).build(),
						new ElementBuilder().address(0x153a).channel(_cell59Voltage).build(),
						new ElementBuilder().address(0x153b).channel(_cell60Voltage).build(),
						new ElementBuilder().address(0x153c).channel(_cell61Voltage).build(),
						new ElementBuilder().address(0x153d).channel(_cell62Voltage).build(),
						new ElementBuilder().address(0x153e).channel(_cell63Voltage).build(),
						new ElementBuilder().address(0x153f).channel(_cell64Voltage).build()),
				new ModbusRange(0x1700, //
						new ElementBuilder().address(0x1700).channel(_cell1Temp).build(),
						new ElementBuilder().address(0x1701).channel(_cell2Temp).build(),
						new ElementBuilder().address(0x1702).channel(_cell3Temp).build(),
						new ElementBuilder().address(0x1703).channel(_cell4Temp).build(),
						new ElementBuilder().address(0x1704).channel(_cell5Temp).build(),
						new ElementBuilder().address(0x1705).channel(_cell6Temp).build(),
						new ElementBuilder().address(0x1706).channel(_cell7Temp).build(),
						new ElementBuilder().address(0x1707).channel(_cell8Temp).build(),
						new ElementBuilder().address(0x1708).channel(_cell9Temp).build(),
						new ElementBuilder().address(0x1709).channel(_cell10Temp).build(),
						new ElementBuilder().address(0x170a).channel(_cell11Temp).build(),
						new ElementBuilder().address(0x170b).channel(_cell12Temp).build(),
						new ElementBuilder().address(0x170c).channel(_cell13Temp).build(),
						new ElementBuilder().address(0x170d).channel(_cell14Temp).build(),
						new ElementBuilder().address(0x170e).channel(_cell15Temp).build(),
						new ElementBuilder().address(0x170f).channel(_cell16Temp).build(),
						new ElementBuilder().address(0x1710).channel(_cell17Temp).build(),
						new ElementBuilder().address(0x1711).channel(_cell18Temp).build(),
						new ElementBuilder().address(0x1712).channel(_cell19Temp).build(),
						new ElementBuilder().address(0x1713).channel(_cell20Temp).build(),
						new ElementBuilder().address(0x1714).channel(_cell21Temp).build(),
						new ElementBuilder().address(0x1715).channel(_cell22Temp).build(),
						new ElementBuilder().address(0x1716).channel(_cell23Temp).build(),
						new ElementBuilder().address(0x1717).channel(_cell24Temp).build(),
						new ElementBuilder().address(0x1718).channel(_cell25Temp).build(),
						new ElementBuilder().address(0x1719).channel(_cell26Temp).build(),
						new ElementBuilder().address(0x171a).channel(_cell27Temp).build(),
						new ElementBuilder().address(0x171b).channel(_cell28Temp).build(),
						new ElementBuilder().address(0x171c).channel(_cell29Temp).build(),
						new ElementBuilder().address(0x171d).channel(_cell30Temp).build(),
						new ElementBuilder().address(0x171e).channel(_cell31Temp).build(),
						new ElementBuilder().address(0x171f).channel(_cell32Temp).build(),
						new ElementBuilder().address(0x1720).channel(_cell33Temp).build(),
						new ElementBuilder().address(0x1721).channel(_cell34Temp).build(),
						new ElementBuilder().address(0x1722).channel(_cell35Temp).build(),
						new ElementBuilder().address(0x1723).channel(_cell36Temp).build(),
						new ElementBuilder().address(0x1724).channel(_cell37Temp).build(),
						new ElementBuilder().address(0x1725).channel(_cell38Temp).build(),
						new ElementBuilder().address(0x1726).channel(_cell39Temp).build(),
						new ElementBuilder().address(0x1727).channel(_cell40Temp).build(),
						new ElementBuilder().address(0x1728).channel(_cell41Temp).build(),
						new ElementBuilder().address(0x1729).channel(_cell42Temp).build(),
						new ElementBuilder().address(0x172a).channel(_cell43Temp).build(),
						new ElementBuilder().address(0x172b).channel(_cell44Temp).build(),
						new ElementBuilder().address(0x172c).channel(_cell45Temp).build(),
						new ElementBuilder().address(0x172d).channel(_cell46Temp).build(),
						new ElementBuilder().address(0x172e).channel(_cell47Temp).build(),
						new ElementBuilder().address(0x172f).channel(_cell48Temp).build(),
						new ElementBuilder().address(0x1730).channel(_cell49Temp).build(),
						new ElementBuilder().address(0x1731).channel(_cell50Temp).build(),
						new ElementBuilder().address(0x1732).channel(_cell51Temp).build(),
						new ElementBuilder().address(0x1733).channel(_cell52Temp).build(),
						new ElementBuilder().address(0x1734).channel(_cell53Temp).build(),
						new ElementBuilder().address(0x1735).channel(_cell54Temp).build(),
						new ElementBuilder().address(0x1736).channel(_cell55Temp).build(),
						new ElementBuilder().address(0x1737).channel(_cell56Temp).build(),
						new ElementBuilder().address(0x1738).channel(_cell57Temp).build(),
						new ElementBuilder().address(0x1739).channel(_cell58Temp).build(),
						new ElementBuilder().address(0x173a).channel(_cell59Temp).build(),
						new ElementBuilder().address(0x173b).channel(_cell60Temp).build(),
						new ElementBuilder().address(0x173c).channel(_cell61Temp).build(),
						new ElementBuilder().address(0x173d).channel(_cell62Temp).build(),
						new ElementBuilder().address(0x173e).channel(_cell63Temp).build(),
						new ElementBuilder().address(0x173f).channel(_cell64Temp).build()));
	}
}
