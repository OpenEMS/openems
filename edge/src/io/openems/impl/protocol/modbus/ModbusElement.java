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
package io.openems.impl.protocol.modbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.impl.protocol.modbus.internal.range.ModbusRange;

public abstract class ModbusElement<T> {
	protected final int address;
	protected final ModbusChannel<T> channel;
	protected final Logger log;
	protected ModbusRange range = null;

	public ModbusElement(int address, ModbusChannel<T> channel) {
		log = LoggerFactory.getLogger(this.getClass());
		this.address = address;
		this.channel = channel;
	}

	public int getAddress() {
		return address;
	}

	public Channel getChannel() {
		return channel;
	}

	public abstract int getLength();

	public ModbusRange getModbusRange() {
		return range;
	}

	/**
	 * Set the {@link ModbusRange}, where this Element belongs to. This is called during {@link ModbusRange}.add()
	 *
	 * @param range
	 */
	public void setModbusRange(ModbusRange range) {
		this.range = range;
	}

	protected void setValue(T value) {
		if (channel == null) {
			return;
		} else if (channel instanceof ModbusReadChannel) {
			((ModbusReadChannel<T>) channel).updateValue(value);
		} else if (channel instanceof ModbusWriteChannel) {
			((ModbusWriteChannel<T>) channel).updateValue(value);
		} else {
			log.error("Unable to set value [" + value + "]. Channel [" + channel.address()
					+ "] is no ModbusChannel or WritableModbusChannel.");
			new Throwable().printStackTrace();
		}
	}

	@Override
	public String toString() {
		return "ModbusElement: Implementation[" + this.getClass().getSimpleName() + "], ModbusAddress[" + address + "]"
				+ (channel != null ? ", ChannelAddress[" + channel.address() + "]" : "");
	}
}
