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
package io.openems.impl.device.wago;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.io.OutputNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.impl.protocol.modbus.ModbusCoilWriteChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.internal.CoilElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.range.ModbusCoilRange;
import io.openems.impl.protocol.modbus.internal.range.ModbusRange;
import io.openems.impl.protocol.modbus.internal.range.WriteableModbusCoilRange;

@ThingInfo(title = "WAGO I/O Output")
public class WagoFBOutput extends ModbusDeviceNature implements OutputNature {

	/*
	 * Constructors
	 */
	public WagoFBOutput(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "IP", description = "IP address of the WAGO device.", type = Inet4Address.class)
	public ConfigChannel<Inet4Address> ip = new ConfigChannel<Inet4Address>("ip", this);

	/*
	 * This Channels
	 */
	private List<ModbusCoilWriteChannel> channel = new ArrayList<>();

	/*
	 * Methods
	 */
	@Override
	public ModbusCoilWriteChannel[] setOutput() {
		return channel.toArray(new ModbusCoilWriteChannel[channel.size()]);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		List<ModbusCoilRange> ranges = new ArrayList<>();
		HashMap<String, List<String>> channels;
		try {
			channels = WagoFB.getConfig(ip.value());
			for (String key : channels.keySet()) {
				switch (key) {
				case "DO": {
					List<CoilElement> elements = new ArrayList<>();
					int count = 0;
					for (@SuppressWarnings("unused") String channel : channels.get(key)) {
						ModbusCoilWriteChannel ch = new ModbusCoilWriteChannel(Integer.toString(count), this);
						this.channel.add(ch);
						elements.add(new CoilElement(512 + count, ch));
						count++;
						if (count % 63 == 0) {
							ranges.add(new ModbusCoilRange(elements.get(0).getAddress(),
									elements.toArray(new CoilElement[elements.size()])));
							elements.clear();
						}
					}
					if (this.channel.size() > 0) {
						ranges.add(new WriteableModbusCoilRange(elements.get(0).getAddress(),
								elements.toArray(new CoilElement[elements.size()])));
					}
				}
					break;
				}
			}
		} catch (InvalidValueException e) {
			log.error("Ip-Address is Invalid", e);
		}
		ModbusProtocol protocol = new ModbusProtocol(ranges.toArray(new ModbusRange[ranges.size()]));
		return protocol;
	}

}
