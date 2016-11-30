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

public class SocomecMeter extends ModbusDeviceNature implements SymmetricMeterNature, AsymmetricMeterNature {

	public SocomecMeter(String thingId) throws ConfigException {
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
	private ModbusReadChannel current;
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
				new ModbusRange(0xc558, //

						new UnsignedDoublewordElement(0xc558, //
								voltageL1 = new ModbusReadChannel("VoltageL1", this).unit("mV").multiplier(1)),
						new UnsignedDoublewordElement(0xc55A, //
								voltageL2 = new ModbusReadChannel("VoltageL2", this).unit("mV").multiplier(1)),
						new UnsignedDoublewordElement(0xc55C, //
								voltageL3 = new ModbusReadChannel("VoltageL3", this).unit("mV").multiplier(1)),
						new UnsignedDoublewordElement(0xc55E, //
								frequency = new ModbusReadChannel("Frequency", this).unit("mHZ").multiplier(1)),
						new UnsignedDoublewordElement(0xc560, //
								currentL1 = new ModbusReadChannel("CurrentL1", this).unit("mA")),
						new UnsignedDoublewordElement(0xc562, //
								currentL2 = new ModbusReadChannel("CurrentL2", this).unit("mA")),
						new UnsignedDoublewordElement(0xc564, //
								currentL3 = new ModbusReadChannel("CurrentL3", this).unit("mA")),
						new UnsignedDoublewordElement(0xc566, //
								current = new ModbusReadChannel("Current", this).unit("mA")),
						new SignedDoublewordElement(0xc568, //
								activePower = new ModbusReadChannel("ActivePower", this).unit("W").multiplier(1)),
						new SignedDoublewordElement(0xc56A, //
								reactivePower = new ModbusReadChannel("ReactivePower", this).unit("var").multiplier(1)),
						new SignedDoublewordElement(0xc56C, //
								apparentPower = new ModbusReadChannel("ApparentPower", this).unit("VA").multiplier(1)),
						new DummyElement(0xc56E, 0xc56F),
						new SignedDoublewordElement(0xc570, //
								activePowerL1 = new ModbusReadChannel("ActivePowerL1", this).unit("W").multiplier(1)),
						new SignedDoublewordElement(0xc572, //
								activePowerL2 = new ModbusReadChannel("ActivePowerL2", this).unit("W").multiplier(1)),
						new SignedDoublewordElement(0xc574, //
								activePowerL3 = new ModbusReadChannel("ActivePowerL3", this).unit("W").multiplier(1)),
						new SignedDoublewordElement(0xc576, //
								reactivePowerL1 = new ModbusReadChannel("ReactivePowerL1", this).unit("var")
										.multiplier(1)),
						new SignedDoublewordElement(0xc578, //
								reactivePowerL2 = new ModbusReadChannel("ReactivePowerL2", this).unit("var")
										.multiplier(1)),
						new SignedDoublewordElement(0xc57A, //
								reactivePowerL3 = new ModbusReadChannel("ReactivePowerL3", this).unit("var")
										.multiplier(1))),
				new ModbusRange(0xc652, //
						new UnsignedDoublewordElement(0xc652, //
								activePositiveEnergy = new ModbusReadChannel("ActivePositiveEnergy", this).unit("kWh")),
						new UnsignedDoublewordElement(0xc654, //
								reactivePositiveEnergy = new ModbusReadChannel("ReactivePositiveEnergy", this)
										.unit("kvarh")),
						new UnsignedDoublewordElement(0xc656, //
								apparentEnergy = new ModbusReadChannel("ApparentEnergy", this).unit("kVAh")),
						new UnsignedDoublewordElement(0xc658, //
								activeNegativeEnergy = new ModbusReadChannel("ActiveNegativeEnergy", this).unit("kWh")),
						new UnsignedDoublewordElement(0xc65a, //
								reactiveNegativeEnergy = new ModbusReadChannel("ReactiveNegativeEnergy", this)
										.unit("kvarh"))));
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

	@Override public ReadChannel<Long> current() {
		return current;
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
