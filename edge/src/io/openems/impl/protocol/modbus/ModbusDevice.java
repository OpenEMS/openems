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
package io.openems.impl.protocol.modbus;

import java.util.Optional;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.Device;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.OpenemsException;

public abstract class ModbusDevice extends Device {

	/*
	 * Constructors
	 */
	public ModbusDevice(Bridge parent) throws OpenemsException {
		super(parent);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Unit-ID", description = "Sets the Modbus unit-id.", type = Integer.class)
	public final ConfigChannel<Integer> modbusUnitId = new ConfigChannel<Integer>("modbusUnitId", this);

	/*
	 * Methods
	 */

	protected int getModbusUnitId() throws ConfigException {
		Optional<Integer> modbusUnitId = this.modbusUnitId.valueOptional();
		if (modbusUnitId.isPresent()) {
			return modbusUnitId.get();
		} else {
			throw new ConfigException("No ModbusUnitId configured for Device[" + this.id() + "]");
		}
	}
}
