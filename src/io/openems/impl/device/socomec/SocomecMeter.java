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

import io.openems.api.device.nature.meter.SymmetricMeterNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadChannel;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.ModbusRange;
import io.openems.impl.protocol.modbus.internal.SignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedDoublewordElement;

public class SocomecMeter extends ModbusDeviceNature implements SymmetricMeterNature {

	public SocomecMeter(String thingId) throws ConfigException {
		super(thingId);
	}

	/*
	 * Inherited Channels
	 */
	private ModbusReadChannel activeNegativeEnergy;
	private ModbusReadChannel activePositiveEnergy;
	private ModbusReadChannel activePower;
	private ModbusReadChannel apparentEnergy;
	private ModbusReadChannel apparentPower;
	private ModbusReadChannel reactiveNegativeEnergy;
	private ModbusReadChannel reactivePositiveEnergy;
	private ModbusReadChannel reactivePower;

	@Override public ModbusReadChannel activeNegativeEnergy() {
		return activeNegativeEnergy;
	}

	@Override public ModbusReadChannel activePositiveEnergy() {
		return activePositiveEnergy;
	}

	@Override public ModbusReadChannel activePower() {
		return activePower;
	}

	@Override public ModbusReadChannel apparentEnergy() {
		return apparentEnergy;
	}

	@Override public ModbusReadChannel apparentPower() {
		return apparentPower;
	}

	@Override public ModbusReadChannel reactiveNegativeEnergy() {
		return reactiveNegativeEnergy;
	}

	@Override public ModbusReadChannel reactivePositiveEnergy() {
		return reactivePositiveEnergy;
	}

	@Override public ModbusReadChannel reactivePower() {
		return reactivePower;
	}

	/*
	 * This Channels
	 */
	public ModbusReadChannel activePowerL1;
	public ModbusReadChannel activePowerL2;
	public ModbusReadChannel activePowerL3;
	public ModbusReadChannel reactivePowerL1;
	public ModbusReadChannel reactivePowerL2;
	public ModbusReadChannel reactivePowerL3;

	@Override protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		return new ModbusProtocol( //
				new ModbusRange(0xc568, //
						new SignedDoublewordElement(0xc568, //
								activePower = new ModbusReadChannel("ActivePower", this).unit("W").multiplier(10)),
						new SignedDoublewordElement(0xc56A, //
								reactivePower = new ModbusReadChannel("ReactivePower", this).unit("var")
										.multiplier(10)),
						new SignedDoublewordElement(0xc56C, //
								apparentPower = new ModbusReadChannel("ApparentPower", this).unit("VA").multiplier(10)),
						new DummyElement(0xc56E, 0xc56F),
						new SignedDoublewordElement(0xc570, //
								activePowerL1 = new ModbusReadChannel("ActivePowerL1", this).unit("W").multiplier(10)),
						new SignedDoublewordElement(0xc572, //
								activePowerL2 = new ModbusReadChannel("ActivePowerL2", this).unit("W").multiplier(10)),
						new SignedDoublewordElement(0xc574, //
								activePowerL3 = new ModbusReadChannel("ActivePowerL3", this).unit("W").multiplier(10)),
						new SignedDoublewordElement(0xc576, //
								reactivePowerL1 = new ModbusReadChannel("ReactivePowerL1", this).unit("var")
										.multiplier(10)),
						new SignedDoublewordElement(0xc578, //
								reactivePowerL2 = new ModbusReadChannel("ReactivePowerL2", this).unit("var")
										.multiplier(10)),
						new SignedDoublewordElement(0xc57A, //
								reactivePowerL3 = new ModbusReadChannel("ReactivePowerL3", this).unit("var")
										.multiplier(10))),
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
}
