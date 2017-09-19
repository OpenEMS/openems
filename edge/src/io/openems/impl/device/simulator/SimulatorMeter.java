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
import io.openems.api.device.Device;
import io.openems.api.device.nature.meter.SymmetricMeterNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.simulator.SimulatorDeviceNature;

@ThingInfo(title = "Simulator Meter")
public abstract class SimulatorMeter extends SimulatorDeviceNature implements SymmetricMeterNature {

	/*
	 * Constructors
	 */
	public SimulatorMeter(String thingId, Device parent) throws ConfigException {
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
	@ChannelInfo(type = Long.class, title = "Frequency")
	public ConfigChannel<Long> frequency = new ConfigChannel<Long>("frequency", this);
	@ChannelInfo(type = Long.class, title = "Voltage")
	public ConfigChannel<Long> voltage = new ConfigChannel<Long>("voltage", this);

	@Override
	public ReadChannel<Long> frequency() {
		return frequency;
	}

	@Override
	public ReadChannel<Long> voltage() {
		return voltage;
	}

	/*
	 * Fields
	 */
	// private long lastApparentPower = 0;
	// private double lastCosPhi = 0.9;
	// private long lastVoltage = 230000;
	// private long lastFrequency = 50000;

	/*
	 * Methods
	 */
	@Override
	protected void update() {
		// lastApparentPower = SimulatorTools.addRandomLong(lastApparentPower, getMinApparentPower(),
		// getMaxApparentPower(), 3000);
		// lastCosPhi = SimulatorTools.addRandomDouble(lastCosPhi, getMinCosPhi(), getMaxCosPhi(), 0.1);
		// long activePower = ControllerUtils.calculateActivePowerFromApparentPower(lastApparentPower, lastCosPhi);
		// long reactivePower = ControllerUtils.calculateReactivePower(activePower, lastCosPhi);
		// this.activePower.updateValue(activePower);
		// this.reactivePower.updateValue(reactivePower);
		// this.apparentPower.updateValue(ControllerUtils.calculateApparentPower(activePower, reactivePower));
		// lastVoltage = SimulatorTools.addRandomLong(lastVoltage, 220000, 240000, 1000);
		// this.voltage.updateValue(lastVoltage);
		// lastFrequency = SimulatorTools.addRandomLong(lastFrequency, 48000, 52000, 1000);
		// this.frequency.updateValue(lastFrequency);
	}
}
