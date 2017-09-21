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
package io.openems.impl.device.janitza;

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
import io.openems.impl.protocol.modbus.internal.FloatElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;

@ThingInfo(title = "Janitza UMG96RM Meter")
public class JanitzaUMG96RMEMeter extends ModbusDeviceNature implements SymmetricMeterNature, AsymmetricMeterNature {

	/*
	 * Constructors
	 */
	public JanitzaUMG96RMEMeter(String thingId, Device parent) throws ConfigException {
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
	private ModbusReadLongChannel activePowerL1;
	private ModbusReadLongChannel activePowerL2;
	private ModbusReadLongChannel activePowerL3;
	private ModbusReadLongChannel apparentPower;
	private ModbusReadLongChannel reactivePower;
	private ModbusReadLongChannel reactivePowerL1;
	private ModbusReadLongChannel reactivePowerL2;
	private ModbusReadLongChannel reactivePowerL3;
	private ModbusReadLongChannel frequency;
	private ModbusReadLongChannel voltageL1;
	private ModbusReadLongChannel voltageL2;
	private ModbusReadLongChannel voltageL3;
	private ModbusReadLongChannel currentL1;
	private ModbusReadLongChannel currentL2;
	private ModbusReadLongChannel currentL3;

	/*
	 * Channels
	 */
	public ModbusReadLongChannel current;

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

	/*
	 * Methods
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		return new ModbusProtocol( //
				new ModbusRegisterRange(800, //
						new FloatElement(800, //
								frequency = new ModbusReadLongChannel("Frequency", this).unit("mHz")) //
										.multiplier(3),
						new DummyElement(802, 807),
						new FloatElement(808, voltageL1 = new ModbusReadLongChannel("VoltageL1", this).unit("mV"))
								.multiplier(3),
						new FloatElement(810, voltageL2 = new ModbusReadLongChannel("VoltageL2", this).unit("mV"))
								.multiplier(3),
						new FloatElement(812, voltageL3 = new ModbusReadLongChannel("VoltageL3", this).unit("mV"))
								.multiplier(3),
						new DummyElement(814, 859), new FloatElement(860, //
								currentL1 = new ModbusReadLongChannel("CurrentL1", this).unit("mA")).multiplier(3),
						new FloatElement(862, //
								currentL2 = new ModbusReadLongChannel("CurrentL2", this).unit("mA")).multiplier(3),
						new FloatElement(864, //
								currentL3 = new ModbusReadLongChannel("CurrentL3", this).unit("mA")).multiplier(3),
						new FloatElement(866, //
								current = new ModbusReadLongChannel("Current", this).unit("mA")).multiplier(3),
						new FloatElement(868, //
								activePowerL1 = new ModbusReadLongChannel("ActivePowerL1", this) //
										.unit("W")), //
						new FloatElement(870, //
								activePowerL2 = new ModbusReadLongChannel("ActivePowerL2", this) //
										.unit("W")), //
						new FloatElement(872, //
								activePowerL3 = new ModbusReadLongChannel("ActivePowerL3", this) //
										.unit("W")), //
						new FloatElement(874, //
								activePower = new ModbusReadLongChannel("ActivePower", this) //
										.unit("W")), //
						new FloatElement(876, //
								reactivePowerL1 = new ModbusReadLongChannel("ReactivePowerL1", this) //
										.unit("Var")), //
						new FloatElement(878, //
								reactivePowerL2 = new ModbusReadLongChannel("ReactivePowerL2", this) //
										.unit("Var")), //
						new FloatElement(880, //
								reactivePowerL3 = new ModbusReadLongChannel("ReactivePowerL3", this) //
										.unit("Var")), //
						new FloatElement(882, //
								reactivePower = new ModbusReadLongChannel("ReactivePower", this) //
										.unit("Var")), //
						new DummyElement(884, 889), new FloatElement(890, //
								apparentPower = new ModbusReadLongChannel("ApparentPower", this) //
										.unit("VA")) //
				));
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
}
