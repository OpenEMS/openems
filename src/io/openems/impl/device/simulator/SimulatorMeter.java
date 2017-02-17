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

import java.util.concurrent.ThreadLocalRandom;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.device.nature.meter.SymmetricMeterNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.core.utilities.ControllerUtils;
import io.openems.impl.protocol.simulator.SimulatorDeviceNature;
import io.openems.impl.protocol.simulator.SimulatorReadChannel;

@ThingInfo("Simulated meter")
public class SimulatorMeter extends SimulatorDeviceNature implements SymmetricMeterNature {

	public SimulatorMeter(String thingId) throws ConfigException {
		super(thingId);
	}

	/*
	 * Config
	 */
	private final ConfigChannel<String> type = new ConfigChannel<String>("type", this);

	@Override
	public ConfigChannel<String> type() {
		return type;
	}

	/*
	 * Inherited Channels
	 */
	private SimulatorReadChannel soc = new SimulatorReadChannel("Soc", this).unit("%");
	private SimulatorReadChannel activePower = new SimulatorReadChannel("ActivePower", this);
	private SimulatorReadChannel apparentPower = new SimulatorReadChannel("ApparentPower", this);
	private SimulatorReadChannel reactivePower = new SimulatorReadChannel("ReactivePower", this);
	private SimulatorReadChannel frequency = new SimulatorReadChannel("Frequency", this);
	private SimulatorReadChannel voltage = new SimulatorReadChannel("Voltage", this);

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
	protected void update() {
		soc.updateValue(getRandom(0, 100));
		long apparentPower = getRandom(-10000, 10000);
		double cosPhi = ThreadLocalRandom.current().nextDouble(-1.5, 1.5);
		long activePower = ControllerUtils.calculateActivePowerFromApparentPower(apparentPower, cosPhi);
		long reactivePower = ControllerUtils.calculateReactivePower(activePower, cosPhi);
		this.activePower.updateValue(activePower);
		this.reactivePower.updateValue(reactivePower);
		this.apparentPower.updateValue(apparentPower);
		long voltage = getRandom(220000, 240000);
		this.voltage.updateValue(voltage);
		this.frequency.updateValue(getRandom(48000, 52000));
	}

	private long getRandom(int min, int max) {
		return ThreadLocalRandom.current().nextLong(min, max + 1);
	}

	@Override
	public ReadChannel<Long> frequency() {
		return frequency;
	}

	@Override
	public ReadChannel<Long> voltage() {
		return voltage;
	}

}
