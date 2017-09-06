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
package io.openems.impl.device.socomec;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.meter.AsymmetricMeterNature;
import io.openems.api.device.nature.meter.SymmetricMeterNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.SignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;

@ThingInfo(title = "Socomec B30 Meter")
public class SocomecB30Meter extends ModbusDeviceNature implements SymmetricMeterNature, AsymmetricMeterNature {

	/*
	 * Constructors
	 */
	public SocomecB30Meter(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
	}

	/*
	 * Config
	 */
	private final ConfigChannel<String> type = new ConfigChannel<String>("type", this);

	@Override
	public ConfigChannel<String> type() {
		return type;
	}

	private final ConfigChannel<Long> maxActivePower = new ConfigChannel<Long>("maxActivePower", this);

	@Override
	public ConfigChannel<Long> maxActivePower() {
		return maxActivePower;
	}

	private final ConfigChannel<Long> minActivePower = new ConfigChannel<Long>("minActivePower", this);

	@Override
	public ConfigChannel<Long> minActivePower() {
		return minActivePower;
	}

	/*
	 * Inherited Channels
	 */
	private ModbusReadLongChannel activePower;
	private ModbusReadLongChannel apparentPower;
	private ModbusReadLongChannel reactivePower;
	private ModbusReadLongChannel activePowerL1;
	private ModbusReadLongChannel activePowerL2;
	private ModbusReadLongChannel activePowerL3;
	private ModbusReadLongChannel reactivePowerL1;
	private ModbusReadLongChannel reactivePowerL2;
	private ModbusReadLongChannel reactivePowerL3;
	private ModbusReadLongChannel voltageL1;
	private ModbusReadLongChannel voltageL2;
	private ModbusReadLongChannel voltageL3;
	private ModbusReadLongChannel currentL1;
	private ModbusReadLongChannel currentL2;
	private ModbusReadLongChannel currentL3;
	private ModbusReadLongChannel frequency;

	@Override
	public ModbusReadLongChannel activePower() {
		return activePower;
	}

	@Override
	public ModbusReadLongChannel apparentPower() {
		return apparentPower;
	}

	@Override
	public ModbusReadLongChannel reactivePower() {
		return reactivePower;
	}

	@Override
	public ReadChannel<Long> activePowerL1() {
		return activePowerL1;
	}

	@Override
	public ReadChannel<Long> activePowerL2() {
		return activePowerL2;
	}

	@Override
	public ReadChannel<Long> activePowerL3() {
		return activePowerL3;
	}

	@Override
	public ReadChannel<Long> reactivePowerL1() {
		return reactivePowerL1;
	}

	@Override
	public ReadChannel<Long> reactivePowerL2() {
		return reactivePowerL2;
	}

	@Override
	public ReadChannel<Long> reactivePowerL3() {
		return reactivePowerL3;
	}

	@Override
	public ReadChannel<Long> currentL1() {
		return currentL1;
	}

	@Override
	public ReadChannel<Long> currentL2() {
		return currentL2;
	}

	@Override
	public ReadChannel<Long> currentL3() {
		return currentL3;
	}

	@Override
	public ReadChannel<Long> voltageL1() {
		return voltageL1;
	}

	@Override
	public ReadChannel<Long> voltageL2() {
		return voltageL2;
	}

	@Override
	public ReadChannel<Long> voltageL3() {
		return voltageL3;
	}

	@Override
	public ReadChannel<Long> frequency() {
		return frequency;
	}

	@Override
	public ReadChannel<Long> voltage() {
		return voltageL1;
	}

	/*
	 * This Channels
	 */
	public ModbusReadLongChannel activeNegativeEnergy;
	public ModbusReadLongChannel activePositiveEnergy;
	public ModbusReadLongChannel reactiveNegativeEnergy;
	public ModbusReadLongChannel reactivePositiveEnergy;
	public ModbusReadLongChannel apparentEnergy;

	/*
	 * Methods
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		return new ModbusProtocol( //
				new ModbusRegisterRange(0x480A, //
						new UnsignedDoublewordElement(0x480A, //
								frequency = new ModbusReadLongChannel("Frequency", this).unit("mHZ")),
						new UnsignedDoublewordElement(0x480C, //
								voltageL1 = new ModbusReadLongChannel("VoltageL1", this).unit("mV").multiplier(1)),
						new UnsignedDoublewordElement(0x480E, //
								voltageL2 = new ModbusReadLongChannel("VoltageL2", this).unit("mV").multiplier(1)),
						new UnsignedDoublewordElement(0x4810, //
								voltageL3 = new ModbusReadLongChannel("VoltageL3", this).unit("mV").multiplier(1)),
						new DummyElement(0x4812, 0x4819), new UnsignedDoublewordElement(0x481A, //
								currentL1 = new ModbusReadLongChannel("CurrentL1", this).unit("mA")),
						new UnsignedDoublewordElement(0x481C, //
								currentL2 = new ModbusReadLongChannel("CurrentL2", this).unit("mA")),
						new UnsignedDoublewordElement(0x481E, //
								currentL3 = new ModbusReadLongChannel("CurrentL3", this).unit("mA"))),
				new ModbusRegisterRange(0x482C, //
						new SignedDoublewordElement(0x482C, //
								activePower = new ModbusReadLongChannel("ActivePower", this).unit("W")),
						new SignedDoublewordElement(0x482E, //
								reactivePower = new ModbusReadLongChannel("ReactivePower", this).unit("Var")),
						new DummyElement(0x4830, 0x4833), new SignedDoublewordElement(0x4834, //
								apparentPower = new ModbusReadLongChannel("ApparentPower", this).unit("VA")),
						new DummyElement(0x4836, 0x4837), new SignedDoublewordElement(0x4838, //
								activePowerL1 = new ModbusReadLongChannel("ActivePowerL1", this).unit("W")),
						new SignedDoublewordElement(0x483A, //
								activePowerL2 = new ModbusReadLongChannel("ActivePowerL2", this).unit("W")),
						new SignedDoublewordElement(0x483C, //
								activePowerL3 = new ModbusReadLongChannel("ActivePowerL3", this).unit("W")),
						new SignedDoublewordElement(0x483E, //
								reactivePowerL1 = new ModbusReadLongChannel("ReactivePowerL1", this).unit("Var")),
						new SignedDoublewordElement(0x4840, //
								reactivePowerL2 = new ModbusReadLongChannel("ReactivePowerL2", this).unit("Var")),
						new SignedDoublewordElement(0x4842, //
								reactivePowerL3 = new ModbusReadLongChannel("ReactivePowerL3", this).unit("Var"))),
				new ModbusRegisterRange(0x4D83, //
						new UnsignedDoublewordElement(0x4D83, //
								activePositiveEnergy = new ModbusReadLongChannel("ActivePositiveEnergy",
										this).unit("kWh")),
						new DummyElement(0x4D85), new UnsignedDoublewordElement(0x4D86, //
								activeNegativeEnergy = new ModbusReadLongChannel("ActiveNegativeEnergy",
										this).unit("kWh")),
						new DummyElement(0x4D88), new UnsignedDoublewordElement(0x4D89, //
								reactivePositiveEnergy = new ModbusReadLongChannel("ReactivePositiveEnergy",
										this).unit("kvarh")),
						new DummyElement(0x4D8B), new UnsignedDoublewordElement(0x4D8C, //
								reactiveNegativeEnergy = new ModbusReadLongChannel("ReactiveNegativeEnergy", this)
										.unit("kvarh")),
						new DummyElement(0x4D8E), new UnsignedDoublewordElement(0x4D8F, //
								apparentEnergy = new ModbusReadLongChannel("ApparentEnergy", this).unit("kVAh")))

		);
	}
}
