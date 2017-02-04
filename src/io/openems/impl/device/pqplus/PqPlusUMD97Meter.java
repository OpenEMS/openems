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
package io.openems.impl.device.pqplus;

import io.openems.api.channel.ReadChannel;
import io.openems.api.device.nature.meter.SymmetricMeterNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadLongChannel;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.FloatElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;

public class PqPlusUMD97Meter extends ModbusDeviceNature implements SymmetricMeterNature {

	public PqPlusUMD97Meter(String thingId) throws ConfigException {
		super(thingId);
	}

	/*
	 * Inherited Channels
	 */
	private ModbusReadLongChannel activePower;
	public ModbusReadLongChannel activePowerL1;
	public ModbusReadLongChannel activePowerL2;
	public ModbusReadLongChannel activePowerL3;
	private ModbusReadLongChannel apparentPower;
	private ModbusReadLongChannel reactivePower;
	public ModbusReadLongChannel reactivePowerL1;
	public ModbusReadLongChannel reactivePowerL2;
	public ModbusReadLongChannel reactivePowerL3;
	public ModbusReadLongChannel current;
	private ModbusReadLongChannel frequency;
	public ModbusReadLongChannel voltageL1;
	public ModbusReadLongChannel voltageL2;
	public ModbusReadLongChannel voltageL3;
	public ModbusReadLongChannel currentL1;
	public ModbusReadLongChannel currentL2;
	public ModbusReadLongChannel currentL3;

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
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		return new ModbusProtocol( //
				new ModbusRegisterRange(19000, //
						new FloatElement(19000, voltageL1 = new ModbusReadLongChannel("VoltageL1", this).unit("mV"))
								.multiplier(3),
						new FloatElement(19002, voltageL2 = new ModbusReadLongChannel("VoltageL2", this).unit("mV"))
								.multiplier(3),
						new FloatElement(19004, voltageL3 = new ModbusReadLongChannel("VoltageL3", this).unit("mV"))
								.multiplier(3),
						new DummyElement(19006, 19011),
						new FloatElement(19012, //
								currentL1 = new ModbusReadLongChannel("CurrentL1", this).unit("mA")).multiplier(3),
						new FloatElement(19014, //
								currentL2 = new ModbusReadLongChannel("CurrentL2", this).unit("mA")).multiplier(3),
						new FloatElement(19016, //
								currentL3 = new ModbusReadLongChannel("CurrentL3", this).unit("mA")).multiplier(3),
						new FloatElement(19018, //
								current = new ModbusReadLongChannel("Current", this).unit("mA")).multiplier(3),
						new FloatElement(19020, //
								activePowerL1 = new ModbusReadLongChannel("ActivePowerL1", this) //
										.unit("W")), //
						new FloatElement(19022, //
								activePowerL2 = new ModbusReadLongChannel("ActivePowerL2", this) //
										.unit("W")), //
						new FloatElement(19024, //
								activePowerL3 = new ModbusReadLongChannel("ActivePowerL3", this) //
										.unit("W")), //
						new FloatElement(19026, //
								activePower = new ModbusReadLongChannel("ActivePower", this) //
										.unit("W")), //
						new DummyElement(19028, 19033),
						new FloatElement(19034, //
								apparentPower = new ModbusReadLongChannel("ApparentPower", this) //
										.unit("VA")), //
						new FloatElement(19036, //
								reactivePowerL1 = new ModbusReadLongChannel("ReactivePowerL1", this) //
										.unit("Var")), //
						new FloatElement(19038, //
								reactivePowerL2 = new ModbusReadLongChannel("ReactivePowerL2", this) //
										.unit("Var")), //
						new FloatElement(19040, //
								reactivePowerL3 = new ModbusReadLongChannel("ReactivePowerL3", this) //
										.unit("Var")), //
						new FloatElement(19042, //
								reactivePower = new ModbusReadLongChannel("ReactivePower", this) //
										.unit("Var")), //
						new DummyElement(19044, 19049),
						new FloatElement(19050, //
								frequency = new ModbusReadLongChannel("Frequency", this).unit("mHz")) //
										.multiplier(3)));
	}

	@Override
	public ReadChannel<Long> voltage() {
		return voltageL1;
	}

}
