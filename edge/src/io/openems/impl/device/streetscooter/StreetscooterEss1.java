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
import io.openems.core.utilities.power.symmetric.SymmetricPower;
import io.openems.impl.protocol.modbus.ModbusCoilReadChannel;
import io.openems.impl.protocol.modbus.ModbusCoilWriteChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.ModbusWriteLongChannel;
import io.openems.impl.protocol.modbus.internal.FloatElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.WordOrder;
import io.openems.impl.protocol.modbus.internal.range.ModbusInputRegisterRange;

@ThingInfo(title = "Streetscooter ESS")
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

	private ModbusReadLongChannel soc;
	private StaticValueChannel<Long> systemState = new StaticValueChannel<>("SystemState", this, 0l);
	private ModbusWriteLongChannel setWorkState;
	private ReadChannel<Long> allowedApparent;
	// RealTimeClock
	private StaticValueChannel<Long> capacity = new StaticValueChannel<>("capacity", this, 12000L).unit("Wh");
	private StaticValueChannel<Long> maxNominalPower = new StaticValueChannel<>("maxNominalPower", this, 9000L)
			.unit("VA");

	private ReadChannel<Long> gridMode = new StaticValueChannel<>("ReactivePower", this, 0l);
	private StaticValueChannel<Long> reactivePower = new StaticValueChannel<>("ReactivePower", this, 0l);
	private StaticValueChannel<Long> apparentPower = new StaticValueChannel<>("ApparentPower", this, 0l);

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
	public ModbusReadLongChannel errorMessage2L;
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
		ModbusProtocol protocol = new ModbusProtocol( //
				new ModbusInputRegisterRange(0, //
						new FloatElement(0,
								batteryError = new ModbusReadLongChannel("BatteryError", this)),
						new FloatElement(2, batteryCurrent = new ModbusReadLongChannel("BatteryCurrent", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(4, allowedCharge = new ModbusReadLongChannel("AllowedCharge", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(6, allowedDischarge = new ModbusReadLongChannel("AllowedDischarge", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(8, pwrRgnMax = new ModbusReadLongChannel("PwrRgnMax", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(10, soc = new ModbusReadLongChannel("Soc", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(12, soh = new ModbusReadLongChannel("Soh", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(14, stBat = new ModbusReadLongChannel("StBat", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(16, tMaxPack = new ModbusReadLongChannel("TMaxPack", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(18, tMinPack = new ModbusReadLongChannel("TMinPack", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(20, batteryVoltage = new ModbusReadLongChannel("BatteryVoltage", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(22, warning = new ModbusReadLongChannel("Warning", this)).wordOrder(WordOrder.LSWMSW)),
				new ModbusInputRegisterRange(2000, //
						// TODO tausche activePower und PowerActive
						new FloatElement(2000, activePower = new ModbusReadLongChannel("ActivePower", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2002, dc1FaultValue = new ModbusReadLongChannel("DC1FaultValue", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2004, dc2FaultValue = new ModbusReadLongChannel("DC2FaultValue", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2006, errorMessage1H = new ModbusReadLongChannel("ErrorMessage1H", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2008, errorMessage1L = new ModbusReadLongChannel("ErrorMessage1L", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2010, errorMessage2H = new ModbusReadLongChannel("ErrorMessage2H", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2012, errorMessage2L = new ModbusReadLongChannel("ErrorMessage2L", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2014, frequencyActive1 = new ModbusReadLongChannel("FrequencyActive1", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2016, frequencyActive2 = new ModbusReadLongChannel("FrequencyActive2", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2018, frequencyActive3 = new ModbusReadLongChannel("FrequencyActive3", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2020,
								gridFrequency1FaultValue = new ModbusReadLongChannel("GridFrequency1FaultValue", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2022,
								gridFrequency2FaultValue = new ModbusReadLongChannel("GridFrequencyf2FaultValue",
										this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2024,
								gridFrequency3FaultValue = new ModbusReadLongChannel("GridFrequency3FaultValue", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2026, gfciFaultValue = new ModbusReadLongChannel("GFCIFaultValue", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2028,
								gridVoltage1FaultValue = new ModbusReadLongChannel("GridVoltage1FaultValue", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2030,
								gridVoltage2FaultValue = new ModbusReadLongChannel("GridVoltage2FaultValue", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2032,
								gridVoltage3FaultValue = new ModbusReadLongChannel("GridVoltage3FaultValue", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2034, powerActive = new ModbusReadLongChannel("PowerActive", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2036, powerActive1 = new ModbusReadLongChannel("PowerActive1", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2038, powerActive2 = new ModbusReadLongChannel("PowerActive2", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2040, powerActive3 = new ModbusReadLongChannel("PowerActive3", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2042, temperature = new ModbusReadLongChannel("Temperature", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2044,
								temperaturFaultValue = new ModbusReadLongChannel("TemperaturFaultValue", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2046, voltageActive1 = new ModbusReadLongChannel("VoltageActive1", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2048, voltageActive2 = new ModbusReadLongChannel("VoltageActive2", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2050, voltageActive3 = new ModbusReadLongChannel("VoltageActive3", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2052, voltagedc1 = new ModbusReadLongChannel("Voltagedc1", this)).wordOrder(WordOrder.LSWMSW),
						new FloatElement(2054, voltagedc2 = new ModbusReadLongChannel("Voltagedc2", this)).wordOrder(WordOrder.LSWMSW))
				// new ModbusCoilRange(10001,
				// new CoilElement(10001, batteryConnected = new ModbusCoilReadChannel("BatteryConnected", this)),
				// new CoilElement(10002, batteryOverload = new ModbusCoilReadChannel("BatteryOverload", this))),
				//
				// new ModbusCoilRange(12001,
				// new CoilElement(12001,
				// inverterConnected = new ModbusCoilReadChannel("Inverteronnected", this))),
				// new WriteableModbusRegisterRange(44001, //
				// // TODO use ICUSetPower for setActivePower
				// new FloatElement(44001, new ModbusWriteLongChannel("ICUSetPower", this))),
				//
				// new WriteableModbusCoilRange(4001,
				// new CoilElement(4001, icuEnabled = new ModbusCoilWriteChannel("ICUEnabled", this)),
				// new CoilElement(4003, icuRun = new ModbusCoilWriteChannel("ICURun", this))),
				// new WriteableModbusCoilRange(14001,
				// new CoilElement(14001, icuRunstate = new ModbusCoilWriteChannel("ICURunstate", this))));
				);
		return protocol;
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
		return this.apparentPower;
	}

	@Override
	public ReadChannel<Long> reactivePower() {
		return this.reactivePower;
	}

	@Override
	public SymmetricPower getPower() {
		// TODO Auto-generated method stub
		return null;
	}
}
