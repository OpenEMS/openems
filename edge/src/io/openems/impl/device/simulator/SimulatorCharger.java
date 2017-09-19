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
package io.openems.impl.device.simulator;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.charger.ChargerNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusWriteLongChannel;
import io.openems.impl.protocol.simulator.SimulatorDeviceNature;
import io.openems.impl.protocol.simulator.SimulatorReadChannel;

@ThingInfo(title = "Simulator Charger")
public class SimulatorCharger extends SimulatorDeviceNature implements ChargerNature {

	/*
	 * Constructors
	 */
	public SimulatorCharger(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
	}

	/*
	 * Config-Channels
	 */
	@ChannelInfo(title = "PowerConfig", type = Long.class)
	public ConfigChannel<Long> powerConfig = new ConfigChannel<>("powerConfig", this);

	/*
	 * Inherited Channels
	 */
	private SimulatorReadChannel<Long> voltage = new SimulatorReadChannel<Long>("InputVoltage", this).unit("mV");
	private SimulatorReadChannel<Long> power = new SimulatorReadChannel<Long>("ActualPower", this).unit("W");
	private StaticValueChannel<Long> nominalPower = new StaticValueChannel<Long>("NominalPower", this, 60000l);
	private ModbusWriteLongChannel setMaxPower = new ModbusWriteLongChannel("SetMaxPower", this);

	/*
	 * Fields
	 */
	private long lastVoltage = 0;

	private final ConfigChannel<Long> maxActualPower = new ConfigChannel<Long>("maxActualPower", this);

	@Override
	public ConfigChannel<Long> maxActualPower() {
		return maxActualPower;
	}

	/*
	 * Methods
	 */
	@Override
	protected void update() {
		long power = SimulatorTools.addRandomLong(powerConfig.valueOptional().orElse(0L), 0,
				(long) (powerConfig.valueOptional().orElse(0L) * 1.10), 100);
		lastVoltage = SimulatorTools.addRandomLong(lastVoltage, 300000, 600000, 100);
		this.voltage.updateValue(lastVoltage);
		this.power.updateValue(power);
	}

	@Override
	public WriteChannel<Long> setMaxPower() {
		return setMaxPower;
	}

	@Override
	public ReadChannel<Long> getActualPower() {
		return power;
	}

	@Override
	public ReadChannel<Long> getNominalPower() {
		return nominalPower;
	}

	@Override
	public ReadChannel<Long> getInputVoltage() {
		return voltage;
	}

}
