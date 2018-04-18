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
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.device.Device;
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

// TODO Single Phase Meter should implement a SinglePhaseMeterNature which should have a configChannel on which phase (L1/L2/L3) it is connected. This setting should be reflected in the UI.

@ThingInfo(title = "Socomec Single Phase Meter")
public class SocomecSinglePhaseMeter extends ModbusDeviceNature implements SymmetricMeterNature {

	private ThingStateChannels thingState;

	/*
	 * Constructors
	 */
	public SocomecSinglePhaseMeter(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
		this.thingState = new ThingStateChannels(this);
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
	private ModbusReadLongChannel frequency;
	private ModbusReadLongChannel voltageL1;

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
	public ReadChannel<Long> frequency() {
		return frequency;
	}

	/*
	 * This Channels
	 */
	public ModbusReadLongChannel activeNegativeEnergy;
	public ModbusReadLongChannel activePositiveEnergy;
	public ModbusReadLongChannel reactiveNegativeEnergy;
	public ModbusReadLongChannel reactivePositiveEnergy;
	public ModbusReadLongChannel apparentEnergy;
	public ModbusReadLongChannel current;

	/*
	 * Methods
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		return new ModbusProtocol( //
				new ModbusRegisterRange(0xc558, //
						new UnsignedDoublewordElement(0xc558, //
								voltageL1 = new ModbusReadLongChannel("VoltageL1", this).unit("mV").multiplier(1)),
						new DummyElement(0xc55A, 0xc55D),
						new UnsignedDoublewordElement(0xc55E, //
								frequency = new ModbusReadLongChannel("Frequency", this).unit("mHZ").multiplier(1)),
						new DummyElement(0xc560, 0xc565),
						new UnsignedDoublewordElement(0xc566, //
								current = new ModbusReadLongChannel("Current", this).unit("mA")),
						new SignedDoublewordElement(0xc568, //
								activePower = new ModbusReadLongChannel("ActivePower", this).unit("W").multiplier(1)),
						new SignedDoublewordElement(0xc56A, //
								reactivePower = new ModbusReadLongChannel("ReactivePower", this).unit("var")
								.multiplier(1)),
						new SignedDoublewordElement(0xc56C, //
								apparentPower = new ModbusReadLongChannel("ApparentPower", this).unit("VA")
								.multiplier(1))),
				new ModbusRegisterRange(0xc652, //
						new UnsignedDoublewordElement(0xc652, //
								activePositiveEnergy = new ModbusReadLongChannel(
										"ActivePositiveEnergy", this).unit("kWh")),
						new UnsignedDoublewordElement(0xc654, //
								reactivePositiveEnergy = new ModbusReadLongChannel(
										"ReactivePositiveEnergy", this).unit("kvarh")),
						new UnsignedDoublewordElement(0xc656, //
								apparentEnergy = new ModbusReadLongChannel("ApparentEnergy", this).unit("kVAh")),
						new UnsignedDoublewordElement(0xc658, //
								activeNegativeEnergy = new ModbusReadLongChannel(
										"ActiveNegativeEnergy", this).unit("kWh")),
						new UnsignedDoublewordElement(0xc65a, //
								reactiveNegativeEnergy = new ModbusReadLongChannel("ReactiveNegativeEnergy", this)
								.unit("kvarh"))));
	}

	@Override
	public ReadChannel<Long> voltage() {
		return this.voltageL1;
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}
}
