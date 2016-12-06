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
package io.openems.impl.device.socomec;

import io.openems.api.channel.ReadChannel;
import io.openems.api.device.nature.meter.AsymmetricMeterNature;
import io.openems.api.device.nature.meter.SymmetricMeterNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadChannel;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.ModbusRange;
import io.openems.impl.protocol.modbus.internal.SignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedDoublewordElement;

public class SocomecB30Meter extends ModbusDeviceNature implements SymmetricMeterNature, AsymmetricMeterNature {

	public SocomecB30Meter(String thingId) throws ConfigException {
		super(thingId);
	}

	/*
	 * Inherited Channels
	 */
	private ModbusReadChannel activePower;
	private ModbusReadChannel apparentPower;
	private ModbusReadChannel reactivePower;
	private ModbusReadChannel activePowerL1;
	private ModbusReadChannel activePowerL2;
	private ModbusReadChannel activePowerL3;
	private ModbusReadChannel reactivePowerL1;
	private ModbusReadChannel reactivePowerL2;
	private ModbusReadChannel reactivePowerL3;
	private ModbusReadChannel voltageL1;
	private ModbusReadChannel voltageL2;
	private ModbusReadChannel voltageL3;
	private ModbusReadChannel currentL1;
	private ModbusReadChannel currentL2;
	private ModbusReadChannel currentL3;
	private ModbusReadChannel frequency;

	@Override public ModbusReadChannel activePower() {
		return activePower;
	}

	@Override public ModbusReadChannel apparentPower() {
		return apparentPower;
	}

	@Override public ModbusReadChannel reactivePower() {
		return reactivePower;
	}

	/*
	 * This Channels
	 */
	public ModbusReadChannel activeNegativeEnergy;
	public ModbusReadChannel activePositiveEnergy;
	public ModbusReadChannel reactiveNegativeEnergy;
	public ModbusReadChannel reactivePositiveEnergy;
	public ModbusReadChannel apparentEnergy;

	@Override protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		return new ModbusProtocol( //
				new ModbusRange(0x480A, //
						new UnsignedDoublewordElement(0x480A, //
								frequency = new ModbusReadChannel("Frequency", this).unit("mHZ")),
						new UnsignedDoublewordElement(0x480C, //
								voltageL1 = new ModbusReadChannel("VoltageL1", this).unit("mV").multiplier(1)),
						new UnsignedDoublewordElement(0x480E, //
								voltageL2 = new ModbusReadChannel("VoltageL2", this).unit("mV").multiplier(1)),
						new UnsignedDoublewordElement(0x4810, //
								voltageL3 = new ModbusReadChannel("VoltageL3", this).unit("mV").multiplier(1)),
						new DummyElement(0x4812, 0x4819),
						new UnsignedDoublewordElement(0x481A, //
								currentL1 = new ModbusReadChannel("CurrentL1", this).unit("mA")),
						new UnsignedDoublewordElement(0x481C, //
								currentL2 = new ModbusReadChannel("CurrentL2", this)
										.unit("mA")),
						new UnsignedDoublewordElement(0x481E, //
								currentL3 = new ModbusReadChannel("CurrentL3", this).unit("mA"))),
				new ModbusRange(0x482C, //
						new SignedDoublewordElement(0x482C, //
								activePower = new ModbusReadChannel("ActivePower", this).unit("W")),
						new SignedDoublewordElement(0x482E, //
								reactivePower = new ModbusReadChannel("ReactivePower", this).unit("Var")),
						new DummyElement(0x4830, 0x4833),
						new SignedDoublewordElement(0x4834, //
								apparentPower = new ModbusReadChannel("ApparentPower", this).unit("VA")),
						new DummyElement(0x4836, 0x4837),
						new SignedDoublewordElement(0x4838, //
								activePowerL1 = new ModbusReadChannel("ActivePowerL1", this).unit("W")),
						new SignedDoublewordElement(0x483A, //
								activePowerL2 = new ModbusReadChannel("ActivePowerL2", this).unit("W")),
						new SignedDoublewordElement(0x483C, //
								activePowerL3 = new ModbusReadChannel("ActivePowerL3", this).unit("W")),
						new SignedDoublewordElement(0x483E, //
								reactivePowerL1 = new ModbusReadChannel("ReactivePowerL1", this).unit("Var")),
						new SignedDoublewordElement(0x4840, //
								reactivePowerL2 = new ModbusReadChannel("ReactivePowerL2", this).unit("Var")),
						new SignedDoublewordElement(0x4842, //
								reactivePowerL3 = new ModbusReadChannel("ReactivePowerL3", this).unit("Var"))),
				new ModbusRange(0x4D83, //
						new UnsignedDoublewordElement(0x4D83, //
								activePositiveEnergy = new ModbusReadChannel("ActivePositiveEnergy", this).unit("kWh")),
						new DummyElement(0x4D85),
						new UnsignedDoublewordElement(0x4D86, //
								activeNegativeEnergy = new ModbusReadChannel("ActiveNegativeEnergy", this).unit("kWh")),
						new DummyElement(0x4D88),
						new UnsignedDoublewordElement(0x4D89, //
								reactivePositiveEnergy = new ModbusReadChannel("ReactivePositiveEnergy", this)
										.unit("kvarh")),
						new DummyElement(0x4D8B),
						new UnsignedDoublewordElement(0x4D8C, //
								reactiveNegativeEnergy = new ModbusReadChannel("ReactiveNegativeEnergy", this)
										.unit("kvarh")),
						new DummyElement(0x4D8E), new UnsignedDoublewordElement(0x4D8F, //
								apparentEnergy = new ModbusReadChannel("ApparentEnergy", this).unit("kVAh")))

		);
	}

	@Override public ReadChannel<Long> activePowerL1() {
		return activePowerL1;
	}

	@Override public ReadChannel<Long> activePowerL2() {
		return activePowerL2;
	}

	@Override public ReadChannel<Long> activePowerL3() {
		return activePowerL3;
	}

	@Override public ReadChannel<Long> reactivePowerL1() {
		return reactivePowerL1;
	}

	@Override public ReadChannel<Long> reactivePowerL2() {
		return reactivePowerL2;
	}

	@Override public ReadChannel<Long> reactivePowerL3() {
		return reactivePowerL3;
	}

	@Override public ReadChannel<Long> currentL1() {
		return currentL1;
	}

	@Override public ReadChannel<Long> currentL2() {
		return currentL2;
	}

	@Override public ReadChannel<Long> currentL3() {
		return currentL3;
	}

	@Override public ReadChannel<Long> voltageL1() {
		return voltageL1;
	}

	@Override public ReadChannel<Long> voltageL2() {
		return voltageL2;
	}

	@Override public ReadChannel<Long> voltageL3() {
		return voltageL3;
	}

	@Override public ReadChannel<Long> frequency() {
		return frequency;
	}

	@Override public ReadChannel<Long> voltage() {
		return voltageL1;
	}
}
