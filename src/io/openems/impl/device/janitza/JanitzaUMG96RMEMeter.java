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
package io.openems.impl.device.janitza;

import io.openems.api.channel.ReadChannel;
import io.openems.api.device.nature.meter.SymmetricMeterNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadChannel;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.FloatElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.ModbusRange;

public class JanitzaUMG96RMEMeter extends ModbusDeviceNature implements SymmetricMeterNature {

	public JanitzaUMG96RMEMeter(String thingId) throws ConfigException {
		super(thingId);
	}

	/*
	 * Inherited Channels
	 */
	private ModbusReadChannel activePower;
	public ModbusReadChannel activePowerL1;
	public ModbusReadChannel activePowerL2;
	public ModbusReadChannel activePowerL3;
	private ModbusReadChannel apparentPower;
	private ModbusReadChannel reactivePower;
	public ModbusReadChannel reactivePowerL1;
	public ModbusReadChannel reactivePowerL2;
	public ModbusReadChannel reactivePowerL3;
	public ModbusReadChannel current;
	private ModbusReadChannel frequency;
	public ModbusReadChannel voltageL1;
	public ModbusReadChannel voltageL2;
	public ModbusReadChannel voltageL3;
	public ModbusReadChannel currentL1;
	public ModbusReadChannel currentL2;
	public ModbusReadChannel currentL3;

	@Override public ReadChannel<Long> activePower() {
		return activePower;
	}

	@Override public ReadChannel<Long> apparentPower() {
		return apparentPower;
	}

	@Override public ReadChannel<Long> reactivePower() {
		return reactivePower;
	}

	@Override public ReadChannel<Long> frequency() {
		return frequency;
	}

	@Override protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		return new ModbusProtocol( //
				new ModbusRange(800, //
						new FloatElement(800, //
								frequency = new ModbusReadChannel("Frequency", this).unit("mHz")) //
										.multiplier(3),
						new DummyElement(802, 807),
						new FloatElement(808, voltageL1 = new ModbusReadChannel("VoltageL1", this).unit("mV"))
								.multiplier(3),
						new FloatElement(810, voltageL2 = new ModbusReadChannel("VoltageL2", this).unit("mV"))
								.multiplier(3),
						new FloatElement(812, voltageL3 = new ModbusReadChannel("VoltageL3", this).unit("mV"))
								.multiplier(3),
						new DummyElement(814, 859),
						new FloatElement(860, //
								currentL1 = new ModbusReadChannel("CurrentL1", this).unit("mA")).multiplier(3),
						new FloatElement(862, //
								currentL2 = new ModbusReadChannel("CurrentL2", this).unit("mA")).multiplier(3),
						new FloatElement(864, //
								currentL3 = new ModbusReadChannel("CurrentL3", this).unit("mA")).multiplier(3),
						new FloatElement(866, //
								current = new ModbusReadChannel("Current", this).unit("mA")).multiplier(3),
						new FloatElement(868, //
								activePowerL1 = new ModbusReadChannel("ActivePowerL1", this) //
										.unit("W")), //
						new FloatElement(870, //
								activePowerL2 = new ModbusReadChannel("ActivePowerL2", this) //
										.unit("W")), //
						new FloatElement(872, //
								activePowerL3 = new ModbusReadChannel("ActivePowerL3", this) //
										.unit("W")), //
						new FloatElement(874, //
								activePower = new ModbusReadChannel("ActivePower", this) //
										.unit("W")), //
						new FloatElement(876, //
								reactivePowerL1 = new ModbusReadChannel("ReactivePowerL1", this) //
										.unit("Var")), //
						new FloatElement(878, //
								reactivePowerL2 = new ModbusReadChannel("ReactivePowerL2", this) //
										.unit("Var")), //
						new FloatElement(880, //
								reactivePowerL3 = new ModbusReadChannel("ReactivePowerL3", this) //
										.unit("Var")), //
						new FloatElement(882, //
								reactivePower = new ModbusReadChannel("ReactivePower", this) //
										.unit("Var")), //
						new DummyElement(884, 889),
						new FloatElement(890, //
								apparentPower = new ModbusReadChannel("ApparentPower", this) //
										.unit("VA")) //
				));
	}

	@Override public ReadChannel<Long> voltage() {
		return voltageL1;
	}

}
