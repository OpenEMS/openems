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
package io.openems.impl.device.kmtronic;

import io.openems.api.device.Device;
import io.openems.api.device.nature.io.OutputNature;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusCoilWriteChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.internal.CoilElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.range.WriteableModbusCoilRange;

@ThingInfo(title = "KMTronic Relay board Output")
public class KMTronicRelayOutput extends ModbusDeviceNature implements OutputNature {

	/*
	 * Constructors
	 */
	public KMTronicRelayOutput(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
		outputs = new ModbusCoilWriteChannel[8];
	}

	/*
	 * Fields
	 */
	private ModbusCoilWriteChannel[] outputs;

	/*
	 * Methods
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		return new ModbusProtocol(
				new WriteableModbusCoilRange(0, new CoilElement(0, outputs[0] = new ModbusCoilWriteChannel("1", this))),
				new WriteableModbusCoilRange(1, new CoilElement(1, outputs[1] = new ModbusCoilWriteChannel("2", this))),
				new WriteableModbusCoilRange(2, new CoilElement(2, outputs[2] = new ModbusCoilWriteChannel("3", this))),
				new WriteableModbusCoilRange(3, new CoilElement(3, outputs[3] = new ModbusCoilWriteChannel("4", this))),
				new WriteableModbusCoilRange(4, new CoilElement(4, outputs[4] = new ModbusCoilWriteChannel("5", this))),
				new WriteableModbusCoilRange(5, new CoilElement(5, outputs[5] = new ModbusCoilWriteChannel("6", this))),
				new WriteableModbusCoilRange(6, new CoilElement(6, outputs[6] = new ModbusCoilWriteChannel("7", this))),
				new WriteableModbusCoilRange(7,
						new CoilElement(7, outputs[7] = new ModbusCoilWriteChannel("8", this))));
	}

	@Override
	public ModbusCoilWriteChannel[] setOutput() {
		return outputs;
	}

}
