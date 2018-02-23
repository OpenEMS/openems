/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
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
package io.openems.impl.device.streetscooter;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.device.Device;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusCoilReadChannel;
import io.openems.impl.protocol.modbus.ModbusCoilWriteChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.ModbusWriteLongChannel;
import io.openems.impl.protocol.modbus.internal.CoilElement;
import io.openems.impl.protocol.modbus.internal.FloatElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.UnsignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.range.ModbusCoilRange;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;
import io.openems.impl.protocol.modbus.internal.range.WriteableModbusCoilRange;
import io.openems.impl.protocol.modbus.internal.range.WriteableModbusRegisterRange;

@ThingInfo(title = "FENECON Pro ESS")
public class StreetscooterEss1 extends ModbusDeviceNature implements SymmetricEssNature {

	/*
	 * Constructors
	 */
	public StreetscooterEss1(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
		minSoc.addUpdateListener((channel, newValue) -> {
			// If chargeSoc was not set -> set it to minSoc minus 2
			if (channel == minSoc && !chargeSoc.valueOptional().isPresent()) {
				chargeSoc.updateValue((Integer) newValue.get() - 2, false);
			}
		});
	}

	/*
	 * Config
	 */
	private ConfigChannel<Integer> minSoc = new ConfigChannel<Integer>("minSoc", this);
	private ConfigChannel<Integer> chargeSoc = new ConfigChannel<Integer>("chargeSoc", this);

	private ThingStateChannels state = new ThingStateChannels(this);

	@Override
	public ConfigChannel<Integer> minSoc() {
		return minSoc;
	}

	@Override
	public ConfigChannel<Integer> chargeSoc() {
		return chargeSoc;
	}

	/*
	 * Inherited Channels
	 */
	// ESS
	private ModbusReadLongChannel allowedCharge;
	private ModbusReadLongChannel allowedDischarge;
	private ReadChannel<Long> gridMode;
	private ModbusReadLongChannel soc;
	private ModbusReadLongChannel systemState;
	private ModbusWriteLongChannel setWorkState;
	private ReadChannel<Long> allowedApparent;
	// RealTimeClock
	private StaticValueChannel<Long> capacity = new StaticValueChannel<>("capacity", this, 12000L).unit("Wh");
	private StaticValueChannel<Long> maxNominalPower = new StaticValueChannel<>("maxNominalPower", this, 9000L)
			.unit("VA");
	private ModbusWriteLongChannel setActivePower;

	@Override
	public ReadChannel<Long> allowedCharge() {
		return allowedCharge;
	}

	@Override
	public ReadChannel<Long> allowedDischarge() {
		return allowedDischarge;
	}

	@Override
	public ReadChannel<Long> gridMode() {
		return gridMode;
	}

	@Override
	public ReadChannel<Long> soc() {
		return soc;
	}

	@Override
	public ReadChannel<Long> systemState() {
		return systemState;
	}

	@Override
	public WriteChannel<Long> setWorkState() {
		return setWorkState;
	}

	@Override
	public ReadChannel<Long> allowedApparent() {
		return allowedApparent;
	}

	/*
	 * This Channels
	 */
	public ModbusReadLongChannel batteryError;
	public ModbusReadLongChannel batteryCurrent;
	public ModbusReadLongChannel pwrRgnMax;
	public ModbusReadLongChannel soh;
	public ModbusReadLongChannel stBat;
	public ModbusReadLongChannel tMaxPack;
	public ModbusReadLongChannel tMinPack;
	public ModbusReadLongChannel batteryVoltage;
	public ModbusReadLongChannel warning;
	private ModbusReadLongChannel activePower;
	public ModbusReadLongChannel dc1FaultValue;
	public ModbusReadLongChannel dc2FaultValue;
	public ModbusReadLongChannel errorMessage1H;
	public ModbusReadLongChannel errorMessage1L;
	public ModbusReadLongChannel errorMessage2H;
	public ModbusReadLongChannel frequencyActive1;
	public ModbusReadLongChannel frequencyActive2;
	public ModbusReadLongChannel frequencyActive3;
	public ModbusReadLongChannel gridFrequency1FaultValue;
	public ModbusReadLongChannel gridFrequency2FaultValue;
	public ModbusReadLongChannel gridFrequency3FaultValue;
	public ModbusReadLongChannel gfciFaultValue;
	public ModbusReadLongChannel gridVoltage1FaultValue;
	public ModbusReadLongChannel gridVoltage2FaultValue;
	public ModbusReadLongChannel gridVoltage3FaultValue;
	public ModbusReadLongChannel powerActive;
	public ModbusReadLongChannel powerActive1;
	public ModbusReadLongChannel powerActive2;
	public ModbusReadLongChannel powerActive3;
	public ModbusReadLongChannel temperature;
	public ModbusReadLongChannel temperaturFaultValue;
	public ModbusReadLongChannel voltageActive1;
	public ModbusReadLongChannel voltageActive2;
	public ModbusReadLongChannel voltageActive3;
	public ModbusReadLongChannel voltagedc1;
	public ModbusReadLongChannel voltagedc2;
	public ModbusCoilReadChannel batteryConnected;
	public ModbusCoilReadChannel batteryOverload;
	public ModbusCoilReadChannel inverterConnected;
	public ModbusCoilWriteChannel icuEnabled;
	public ModbusCoilWriteChannel icuRun;
	public ModbusCoilWriteChannel icuRunstate;

	/*
	 * Methods
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		ModbusProtocol protokol = new ModbusProtocol(new ModbusRegisterRange(30001, //
				new UnsignedDoublewordElement(30001, batteryError = new ModbusReadLongChannel("BatteryError", this)),
				new FloatElement(30003, batteryCurrent = new ModbusReadLongChannel("BatteryCurrent", this)),
				new FloatElement(30005, allowedCharge = new ModbusReadLongChannel("AllowedCharge", this)),
				new FloatElement(30007, allowedDischarge = new ModbusReadLongChannel("AllowedDischarge", this)),
				new FloatElement(30009, pwrRgnMax = new ModbusReadLongChannel("PwrRgnMax", this)),
				new UnsignedDoublewordElement(30011, soc = new ModbusReadLongChannel("Soc", this)),
				new UnsignedDoublewordElement(30013, soh = new ModbusReadLongChannel("Soh", this)),
				new UnsignedDoublewordElement(30015, stBat = new ModbusReadLongChannel("StBat", this)),
				new FloatElement(30017, tMaxPack = new ModbusReadLongChannel("TMaxPack", this)),
				new FloatElement(30019, tMinPack = new ModbusReadLongChannel("TMinPack", this)),
				new FloatElement(30021, batteryVoltage = new ModbusReadLongChannel("BatteryVoltage", this)),
				new UnsignedDoublewordElement(30023, warning = new ModbusReadLongChannel("Warning", this))),
				new ModbusRegisterRange(32001, //
						new FloatElement(32001, activePower = new ModbusReadLongChannel("ActivePower", this)),
						new FloatElement(32003, dc1FaultValue = new ModbusReadLongChannel("DC1FaultValue", this)),
						new FloatElement(32005, dc2FaultValue = new ModbusReadLongChannel("DC2FaultValue", this)),
						new FloatElement(32007, errorMessage1H = new ModbusReadLongChannel("ErrorMessage1H", this)),
						new FloatElement(32009, errorMessage1L = new ModbusReadLongChannel("ErrorMessage1L", this)),
						new FloatElement(32011, errorMessage2H = new ModbusReadLongChannel("ErrorMessage2H", this)),
						new FloatElement(32013, errorMessage2H = new ModbusReadLongChannel("ErrorMessage2H", this)),
						new FloatElement(32015, frequencyActive1 = new ModbusReadLongChannel("FrequencyActive1", this)),
						new FloatElement(32017, frequencyActive2 = new ModbusReadLongChannel("FrequencyActive2", this)),
						new FloatElement(32019, frequencyActive3 = new ModbusReadLongChannel("FrequencyActive3", this)),
						new FloatElement(32021,
								gridFrequency1FaultValue = new ModbusReadLongChannel("GridFrequency1FaultValue", this)),
						new FloatElement(32023,
								gridFrequency2FaultValue = new ModbusReadLongChannel("GridFrequencyf2FaultValue",
										this)),
						new FloatElement(32025,
								gridFrequency3FaultValue = new ModbusReadLongChannel("GridFrequency3FaultValue", this)),
						new FloatElement(32027, gfciFaultValue = new ModbusReadLongChannel("GFCIFaultValue", this)),
						new FloatElement(32029,
								gridVoltage1FaultValue = new ModbusReadLongChannel("GridVoltage1FaultValue", this)),
						new FloatElement(32031,
								gridVoltage2FaultValue = new ModbusReadLongChannel("GridVoltage2FaultValue", this)),
						new FloatElement(32033,
								gridVoltage3FaultValue = new ModbusReadLongChannel("GridVoltage3FaultValue", this)),
						new FloatElement(32035, powerActive = new ModbusReadLongChannel("PowerActive", this)),
						new FloatElement(32037, powerActive1 = new ModbusReadLongChannel("PowerActive1", this)),
						new FloatElement(32039, powerActive2 = new ModbusReadLongChannel("PowerActive2", this)),
						new FloatElement(32041, powerActive3 = new ModbusReadLongChannel("PowerActive3", this)),
						new FloatElement(32043, temperature = new ModbusReadLongChannel("Temperature", this)),
						new FloatElement(32045,
								temperaturFaultValue = new ModbusReadLongChannel("TemperaturFaultValue", this)),
						new FloatElement(32047, voltageActive1 = new ModbusReadLongChannel("VoltageActive1", this)),
						new FloatElement(32049, voltageActive2 = new ModbusReadLongChannel("VoltageActive2", this)),
						new FloatElement(32051, voltageActive3 = new ModbusReadLongChannel("VoltageActive3", this)),
						new FloatElement(32053, voltagedc1 = new ModbusReadLongChannel("Voltagedc1", this)),
						new FloatElement(32055, voltagedc2 = new ModbusReadLongChannel("Voltagedc2", this))),
				new ModbusCoilRange(10001,
						new CoilElement(10001, batteryConnected = new ModbusCoilReadChannel("BatteryConnected", this)),
						new CoilElement(10002, batteryOverload = new ModbusCoilReadChannel("BatteryOverload", this))),

				new ModbusCoilRange(12001,
						new CoilElement(12001,
								inverterConnected = new ModbusCoilReadChannel("Inverteronnected", this))),
				new WriteableModbusRegisterRange(44001, //
						new FloatElement(44001, setActivePower = new ModbusWriteLongChannel("ICUSetPower", this))),

				new WriteableModbusCoilRange(4001,
						new CoilElement(4001, icuEnabled = new ModbusCoilWriteChannel("ICUEnabled", this)),
						new CoilElement(4003, icuRun = new ModbusCoilWriteChannel("ICURun", this))),
				new WriteableModbusCoilRange(14001,
						new CoilElement(14001, icuRunstate = new ModbusCoilWriteChannel("ICURunstate", this))));

		return protokol;
	}

	@Override
	public StaticValueChannel<Long> capacity() {
		return capacity;
	}

	@Override
	public ReadChannel<Long> maxNominalPower() {
		return maxNominalPower;
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return state;
	}

	@Override
	public ReadChannel<Long> activePower() {
		return activePower;
	}

	@Override
	public ReadChannel<Long> apparentPower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReadChannel<Long> reactivePower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WriteChannel<Long> setActivePower() {
		return setActivePower;
	}

	@Override
	public WriteChannel<Long> setReactivePower() {
		// TODO Auto-generated method stub
		return null;
	}

}
