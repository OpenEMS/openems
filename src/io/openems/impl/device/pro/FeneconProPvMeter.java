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
package io.openems.impl.device.pro;

import io.openems.api.channel.FunctionalChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.device.nature.meter.AsymmetricMeterNature;
import io.openems.api.device.nature.meter.SymmetricMeterNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadChannel;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.UnsignedDoublewordElement;
import io.openems.impl.protocol.modbus.internal.UnsignedWordElement;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;

@ThingInfo("FENECON Pro meter")
public class FeneconProPvMeter extends ModbusDeviceNature implements AsymmetricMeterNature, SymmetricMeterNature {

	public FeneconProPvMeter(String thingId) throws ConfigException {
		super(thingId);
	}

	/*
	 * Inherited Channels
	 */
	private ModbusReadChannel<Long> activePowerL1;
	private ModbusReadChannel<Long> activePowerL2;
	private ModbusReadChannel<Long> activePowerL3;
	private ReadChannel<Long> reactivePowerL1 = new ReadChannel<Long>("ReactivePowerL1", this);
	private ReadChannel<Long> reactivePowerL2 = new ReadChannel<Long>("ReactivePowerL2", this);
	private ReadChannel<Long> reactivePowerL3 = new ReadChannel<Long>("ReactivePowerL3", this);
	private ModbusReadLongChannel voltageL1;
	private ModbusReadLongChannel voltageL2;
	private ModbusReadLongChannel voltageL3;
	private ReadChannel<Long> currentL1 = new ReadChannel<>("currentL1", this);
	private ReadChannel<Long> currentL2 = new ReadChannel<>("currentL2", this);
	private ReadChannel<Long> currentL3 = new ReadChannel<>("currentL3", this);
	private ReadChannel<Long> activePower;
	private ReadChannel<Long> reactivePower;
	private ReadChannel<Long> apparentPower = new ReadChannel<>("apparentPower", this);
	private ReadChannel<Long> frequency = new ReadChannel<>("frequency", this);

	/*
	 * This Channels
	 */
	public ModbusReadLongChannel activeEnergyL1;
	public ModbusReadLongChannel activeEnergyL2;
	public ModbusReadLongChannel activeEnergyL3;

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		ModbusProtocol protocol = new ModbusProtocol( //
				new ModbusRegisterRange(121,
						new UnsignedWordElement(121, //
								voltageL1 = new ModbusReadLongChannel("VoltageL1", this).unit("mV").multiplier(2)),
						new UnsignedWordElement(122, //
								voltageL2 = new ModbusReadLongChannel("VoltageL2", this).unit("mV").multiplier(2)),
						new UnsignedWordElement(123, //
								voltageL3 = new ModbusReadLongChannel("VoltageL3", this).unit("mV").multiplier(2))),
				new ModbusRegisterRange(2035, //
						new UnsignedDoublewordElement(2035, //
								activeEnergyL1 = new ModbusReadLongChannel("ActiveEnergyL1", this).unit("Wh")
										.multiplier(2)),
						new DummyElement(2037, 2065),
						new UnsignedWordElement(2066, //
								activePowerL1 = new ModbusReadLongChannel("ActivePowerL1", this).unit("W")
										.delta(10000L))),
				new ModbusRegisterRange(2135, //
						new UnsignedDoublewordElement(2135, //
								activeEnergyL2 = new ModbusReadLongChannel("ActiveEnergyL2", this).unit("Wh")
										.multiplier(2)),
						new DummyElement(2137, 2165),
						new UnsignedWordElement(2166, //
								activePowerL2 = new ModbusReadLongChannel("ActivePowerL2", this).unit("W")
										.delta(10000L))),
				new ModbusRegisterRange(2235, //
						new UnsignedDoublewordElement(2235, //
								activeEnergyL3 = new ModbusReadLongChannel("ActiveEnergyL3", this).unit("Wh")
										.multiplier(2)),
						new DummyElement(2237, 2265),
						new UnsignedWordElement(2266, //
								activePowerL3 = new ModbusReadLongChannel("ActivePowerL3", this).unit("W")
										.delta(10000L))));
		activePower = new FunctionalChannel<Long>("reactive", this, (channels) -> {
			ReadChannel<Long> L1 = channels[0];
			ReadChannel<Long> L2 = channels[1];
			ReadChannel<Long> L3 = channels[2];
			try {
				return L1.value() + L2.value() + L3.value();
			} catch (InvalidValueException e) {
				return null;
			}
		}, activePowerL1, activePowerL2, activePowerL3);
		reactivePower = new FunctionalChannel<Long>("reactive", this, (channels) -> {
			ReadChannel<Long> L1 = channels[0];
			ReadChannel<Long> L2 = channels[1];
			ReadChannel<Long> L3 = channels[2];
			try {
				return L1.value() + L2.value() + L3.value();
			} catch (InvalidValueException e) {
				return null;
			}
		}, reactivePowerL1, reactivePowerL2, reactivePowerL3);
		return protocol;
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
	public ReadChannel<Long> activePower() {
		return activePower;
	}

	@Override
	public ReadChannel<Long> apparentPower() {
		return apparentPower;
	}

	@Override
	public ReadChannel<Long> reactivePower() {
		return reactivePower;
	}

	@Override
	public ReadChannel<Long> frequency() {
		return frequency;
	}

	@Override
	public ReadChannel<Long> voltage() {
		return voltageL1;
	}
}
