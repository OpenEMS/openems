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

	private final ModbusChannel _abnormity1 = new ModbusChannelBuilder().nature(this) //
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
	private final ModbusChannel _abnormity2 = new ModbusChannelBuilder().nature(this) //
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
	private final ModbusChannel _abnormity3 = new ModbusChannelBuilder().nature(this) //
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
	private final ModbusChannel _abnormity4 = new ModbusChannelBuilder().nature(this) //
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
	private final ModbusChannel _abnormity5 = new ModbusChannelBuilder().nature(this) //
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
	private final ModbusChannel _acChargeEnergy = new ModbusChannelBuilder().nature(this).unit("Wh").multiplier(100)
			.build();

	private final ModbusChannel _acDischargeEnergy = new ModbusChannelBuilder().nature(this).unit("Wh").multiplier(100)
			.build();
	private final ModbusChannel _activePower = new ModbusChannelBuilder().nature(this).unit("W").multiplier(100)
			.build();
	private final ModbusChannel _allowedApparent = new ModbusChannelBuilder().nature(this).unit("VA").multiplier(100)
			.build();
	private final ModbusChannel _allowedCharge = new ModbusChannelBuilder().nature(this).unit("W").multiplier(100)
			.build();
	private final ModbusChannel _allowedDischarge = new ModbusChannelBuilder().nature(this).unit("W").multiplier(100)
			.build();
	private final ModbusChannel _apparentPower = new ModbusChannelBuilder().nature(this).unit("VA").multiplier(100)
			.build();
	private final ModbusChannel _batteryAccumulatedCharge = new ModbusChannelBuilder().nature(this).unit("Wh").build();
	private final ModbusChannel _batteryAccumulatedDischarge = new ModbusChannelBuilder().nature(this).unit("Wh")
			.build();
	private final ModbusChannel _batteryChargeCycles = new ModbusChannelBuilder().nature(this).build();
	private final ModbusChannel _batteryMaintenanceState = new ModbusChannelBuilder().nature(this) //
			.label(0, "Off") //
			.label(1, "On").build();
	private final ModbusChannel _batteryPower = new ModbusChannelBuilder().nature(this).unit("W").multiplier(100)
			.build();
	private final ModbusChannel _batterySteringTotalCurrent = new ModbusChannelBuilder().nature(this).unit("mA")
			.multiplier(100).build();
	private final ModbusChannel _batteryStringAbnormity1 = new ModbusChannelBuilder().nature(this) //
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
	private final ModbusChannel _batteryStringAbnormity2 = new ModbusChannelBuilder().nature(this) //
			.label(4, "Battery string severe overtemperature") //
			.label(128, "Battery string smog fault") //
			.label(256, "Battery string blown fuse indicator fault") //
			.label(1024, "Battery string general leakage") //
			.label(2048, "Battery string severe leakage") //
			.label(4096, "Communication between BECU and periphery CAN disconnected") //
			.label(16384, "Battery string power supply relay contactor disconnected").build();
	private final ModbusChannel _batteryStringCellAverageTemperature = new ModbusChannelBuilder().nature(this)
			.unit("°C").multiplier(100).build();
	private final ModbusChannel _batteryStringChargeCurrentLimit = new ModbusChannelBuilder().nature(this).unit("mA")
			.multiplier(100).build();
	private final ModbusChannel _batteryStringDischargeCurrentLimit = new ModbusChannelBuilder().nature(this).unit("mA")
			.multiplier(100).build();
	private final ModbusChannel _batteryStringPeripheralIoState = new ModbusChannelBuilder().nature(this)
			.label(1, "Fuse state") //
			.label(2, "Isolated switch state").build();
	private final ModbusChannel _batteryStringSOH = new ModbusChannelBuilder().nature(this).unit("%").multiplier(100)
			.build();
	private final ModbusChannel _batteryStringSuggestiveInformation = new ModbusChannelBuilder().nature(this)
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
	private final ModbusChannel _batteryStringSwitchState = new ModbusChannelBuilder().nature(this)
			.label(1, "Main contactor") //
			.label(2, "Precharge contactor") //
			.label(4, "FAN contactor") //
			.label(8, "BMU power supply relay") //
			.label(16, "Middle relay").build();
	private final ModbusChannel _batteryStringTotalVoltage = new ModbusChannelBuilder().nature(this).unit("mV")
			.multiplier(100).build();
	private final ModbusChannel _batteryStringWorkState = new ModbusChannelBuilder().nature(this) //
			.label(1, "Initial") //
			.label(2, "Stop") //
			.label(4, "Starting up") //
			.label(8, "Running") //
			.label(16, "Fault").build();
	private final ModbusChannel _controlMode = new ModbusChannelBuilder().nature(this) //
			.label(1, "Remote") //
			.label(2, "Local").build();
	private final ModbusChannel _currentPhase1 = new ModbusChannelBuilder().nature(this).unit("mA").multiplier(100)
			.build();
	private final ModbusChannel _currentPhase2 = new ModbusChannelBuilder().nature(this).unit("mA").multiplier(100)
			.build();
	private final ModbusChannel _currentPhase3 = new ModbusChannelBuilder().nature(this).unit("mA").multiplier(100)
			.build();
	private final ModbusChannel _dcCurrent = new ModbusChannelBuilder().nature(this).unit("mA").multiplier(100).build();
	private final ModbusChannel _dcPower = new ModbusChannelBuilder().nature(this).unit("W").multiplier(100).build();
	private final ModbusChannel _dcVoltage = new ModbusChannelBuilder().nature(this).unit("mV").multiplier(100).build();
	private final ModbusChannel _frequency = new ModbusChannelBuilder().nature(this).unit("mHZ").multiplier(10).build();
	private final ModbusChannel _gridMode = new ModbusChannelBuilder().nature(this) //
			.label(1, OFF_GRID) //
			.label(2, ON_GRID).build();
	private final ModbusChannel _inverterActivePower = new ModbusChannelBuilder().nature(this).unit("W").multiplier(100)
			.build();
	private final ModbusChannel _inverterCurrentPhase1 = new ModbusChannelBuilder().nature(this).unit("mA")
			.multiplier(100).build();
	private final ModbusChannel _inverterCurrentPhase2 = new ModbusChannelBuilder().nature(this).unit("mA")
			.multiplier(100).build();
	private final ModbusChannel _inverterCurrentPhase3 = new ModbusChannelBuilder().nature(this).unit("mA")
			.multiplier(100).build();
	private final ModbusChannel _inverterState = new ModbusChannelBuilder().nature(this) //
			.label(0, "Init") //
			.label(2, "Fault") //
			.label(4, STOP) //
			.label(8, STANDBY) //
			.label(16, "Grid-Monitor") // ,
			.label(32, "Ready") //
			.label(64, START) //
			.label(128, "Debug").build();
	private final ModbusChannel _inverterVoltagePhase1 = new ModbusChannelBuilder().nature(this).unit("mV")
			.multiplier(100).build();
	private final ModbusChannel _inverterVoltagePhase2 = new ModbusChannelBuilder().nature(this).unit("mV")
			.multiplier(100).build();
	private final ModbusChannel _inverterVoltagePhase3 = new ModbusChannelBuilder().nature(this).unit("mV")
			.multiplier(100).build();
	private final ModbusChannel _ipmPhaseATemperature = new ModbusChannelBuilder().nature(this).unit("°C").build();
	private final ModbusChannel _ipmPhaseBTemperature = new ModbusChannelBuilder().nature(this).unit("°C").build();
	private final ModbusChannel _ipmPhaseCTemperature = new ModbusChannelBuilder().nature(this).unit("°C").build();
	private final ConfigChannel _minSoc = new ConfigChannelBuilder().nature(this).defaultValue(DEFAULT_MINSOC)
			.percentType().build();
	private final ModbusChannel _protocolVersion = new ModbusChannelBuilder().nature(this).build();
	private final ModbusChannel _reactivePower = new ModbusChannelBuilder().nature(this).unit("var").multiplier(100)
			.build();
	private final WriteableModbusChannel _setActivePower = new WriteableModbusChannelBuilder().nature(this).unit("W")
			.multiplier(100).minWriteValue(_allowedCharge).maxWriteValue(_allowedDischarge).build();
	private final WriteableModbusChannel _setPvLimit = new WriteableModbusChannelBuilder().nature(this).unit("W")
			.multiplier(100).minValue(0).maxValue(60000).build();
	private final WriteableModbusChannel _setReactivePower = new WriteableModbusChannelBuilder().nature(this)
			.unit("var").multiplier(100).minWriteValue(_allowedCharge).maxWriteValue(_allowedDischarge).build();
	private final WriteableModbusChannel _setWorkState = new WriteableModbusChannelBuilder().nature(this) //
			.label(4, STOP) //
			.label(32, STANDBY) //
			.label(64, START).build();
	private final ModbusChannel _soc = new ModbusChannelBuilder().nature(this).percentType().build();
	private final ModbusChannel _suggestiveInformation1 = new ModbusChannelBuilder().nature(this) //
			.label(4, "EmergencyStop") //
			.label(64, "KeyManualStop").build();
	private final ModbusChannel _suggestiveInformation2 = new ModbusChannelBuilder().nature(this) //
			.label(4, "EmergencyStop") //
			.label(64, "KeyManualStop").build();
	private final ModbusChannel _suggestiveInformation3 = new ModbusChannelBuilder().nature(this) //
			.label(1, "Inverter communication abnormity") //
			.label(2, "Battery stack communication abnormity") //
			.label(4, "Multifunctional ammeter communication abnormity") //
			.label(16, "Remote communication abnormity").build();
	private final ModbusChannel _suggestiveInformation4 = new ModbusChannelBuilder().nature(this) //
			.label(8, "Transformer severe overtemperature").build();
	private final ModbusChannel _suggestiveInformation5 = new ModbusChannelBuilder().nature(this) //
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
	private final ModbusChannel _suggestiveInformation6 = new ModbusChannelBuilder().nature(this) //
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
	private final ModbusChannel _suggestiveInformation7 = new ModbusChannelBuilder().nature(this) //
			.label(1, "Communication between inverter and BSMU disconnected") //
			.label(2, "Communication between inverter and Master disconnected") //
			.label(4, "Communication between inverter and UC disconnected") //
			.label(8, "BMS start overtime controlled by PCS") //
			.label(16, "BMS stop overtime controlled by PCS") //
			.label(32, "Sync signal invalidation") //
			.label(64, "Sync signal continuous caputure fault") //
			.label(128, "Sync signal several times caputure fault").build();
	private final ModbusChannel _switchState = new ModbusChannelBuilder().nature(this) //
			.label(2, "DC main contactor state") //
			.label(4, "DC precharge contactor state") //
			.label(8, "AC breaker state") //
			.label(16, "AC main contactor state") //
			.label(32, "AC precharge contactor state").build();
	private final ModbusChannel _systemManufacturer = new ModbusChannelBuilder().nature(this) //
			.label(1, "BYD").build();
	private final ModbusChannel _systemState = new ModbusChannelBuilder().nature(this) //
			.label(8, "TransformertPH1TempSensInvalidation") //
			.label(8192, "SDCardInvalidation").build();
	private final ModbusChannel _systemType = new ModbusChannelBuilder().nature(this) //
			.label(1, "CESS").build();
	private final ModbusChannel _totalDateEnergy = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalEnergy = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy0 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy1 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy10 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy11 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy12 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy13 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy14 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy15 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy16 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy17 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy18 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy19 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy2 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy20 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy21 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy22 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy23 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy3 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy4 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy5 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy6 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy7 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy8 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalHourEnergy9 = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalMonthEnergy = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _totalYearEnergy = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _transformerPhaseBTemperature = new ModbusChannelBuilder().nature(this).unit("°C")
			.build();
	private final ModbusChannel _voltagePhase1 = new ModbusChannelBuilder().nature(this).unit("mV").multiplier(100)
			.build();
	private final ModbusChannel _voltagePhase2 = new ModbusChannelBuilder().nature(this).unit("mV").multiplier(100)
			.build();
	private final ModbusChannel _voltagePhase3 = new ModbusChannelBuilder().nature(this).unit("mV").multiplier(100)
			.build();

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
						new ElementBuilder().address(0x0188).channel(_suggestiveInformation7).build() //
				), new ModbusRange(0x0210, //
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
						new ElementBuilder().address(0x021C).channel(_frequency).build(),
						new ElementBuilder().address(0x021D).dummy(0x222 - 0x21D).build(),
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
						new ElementBuilder().address(0x0502).channel(_setReactivePower).signed().build(), //
						new ElementBuilder().address(0x0503).channel(_setPvLimit).build()),
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
						new ElementBuilder().address(0x1401).channel(_batterySteringTotalCurrent).signed().build(),
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
						new ElementBuilder().address(0x1420).channel(_batteryPower).signed().build())//
		);
	}
}
