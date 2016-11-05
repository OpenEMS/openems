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
package io.openems.impl.protocol.simulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.ConfigException;

public abstract class SimulatorDeviceNature implements DeviceNature {
	protected final Logger log;
	private final String thingId;

	public SimulatorDeviceNature(String thingId) throws ConfigException {
		this.thingId = thingId;
		log = LoggerFactory.getLogger(this.getClass());
	}

	@Override public String id() {
		return thingId;
	}

	@Override
	/**
	 * Sets a Channel as required. The Range with this Channel will be added to ModbusProtocol.RequiredRanges.
	 */
	public void setAsRequired(Channel channel) {
		// ignore
	}

	protected abstract void update();
}
