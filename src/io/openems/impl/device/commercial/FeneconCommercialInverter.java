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
package io.openems.impl.device.commercial;

import io.openems.api.device.nature.PvInverterNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.ModbusWriteLongChannel;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.UnsignedWordElement;
import io.openems.impl.protocol.modbus.internal.range.ModbusRegisterRange;

public class FeneconCommercialInverter extends ModbusDeviceNature implements PvInverterNature {

	/*
	 * Inherited Channels
	 */

	private ModbusWriteLongChannel setPvLimit;

	@Override public ModbusWriteLongChannel setLimit() {
		return setPvLimit;
	}

	public FeneconCommercialInverter(String thingId) throws ConfigException {
		super(thingId);
	}

	@Override protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		ModbusProtocol protocol = new ModbusProtocol(//
				new ModbusRegisterRange(0x0503, //
						new UnsignedWordElement(0x0503, //
								setPvLimit = new ModbusWriteLongChannel("SetPvLimit", this).unit("W").multiplier(100)))
		// TODO .minValue(0).maxValue(60000)
		);
		return protocol;
	}
}
