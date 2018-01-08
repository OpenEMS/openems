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
package io.openems.api.channel;

import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusReadChannel;

public class StatusBitChannel extends ModbusReadChannel<Long> {

	private final ThingStateChannel thingState;

	public StatusBitChannel(String id, DeviceNature nature, ThingStateChannel thingStateChannel) {
		super(id, nature);
		this.thingState = thingStateChannel;
	}

	public StatusBitChannel warningBit(int i, int j) throws ConfigException {
		thingState.addWarningChannel(new BitToBooleanChannel("Warning\\"+i, this.parent(), this, j));
		return this;
	}

	public StatusBitChannel faultBit(int i, int j) throws ConfigException {
		thingState.addFaultChannel(new BitToBooleanChannel("Fault\\"+i, this.parent(), this, j));
		return this;
	}

}
