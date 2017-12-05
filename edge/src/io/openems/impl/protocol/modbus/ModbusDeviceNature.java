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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.bridge.BridgeReadTask;
import io.openems.api.bridge.BridgeWriteTask;
import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.thing.ThingChannelsUpdatedListener;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.range.ModbusRange;
import io.openems.impl.protocol.modbus.internal.range.WriteableModbusRange;

public abstract class ModbusDeviceNature implements DeviceNature, ChannelChangeListener {
	protected final Logger log;
	private ModbusProtocol protocol = null;
	private final String thingId;
	private List<ThingChannelsUpdatedListener> listeners;
	private List<BridgeReadTask> otherReadTasks;
	private List<BridgeReadTask> readTasks;
	private List<BridgeWriteTask> writeTasks;
	private Device parent;

	@ChannelInfo(isOptional=true,title="Alias",description="The Alias to display for the device.", type=String.class)
	public ConfigChannel<String> alias = new ConfigChannel<>("alias", this);

	public ModbusDeviceNature(String thingId, Device parent) throws ConfigException {
		this.parent = parent;
		this.thingId = thingId;
		log = LoggerFactory.getLogger(this.getClass());
		// this.protocol = defineModbusProtocol();
		this.listeners = new ArrayList<>();
	}

	private ModbusProtocol getProtocol() {
		if (protocol == null) {
			createModbusProtocol();
		}
		return this.protocol;
	}

	@Override
	public String getAlias() {
		return alias.valueOptional().orElse(id());
	}

	@Override
	public Device getParent() {
		return parent;
	}

	@Override
	public void addListener(ThingChannelsUpdatedListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void removeListener(ThingChannelsUpdatedListener listener) {
		this.listeners.remove(listener);
	}

	@Override
	public List<BridgeReadTask> getReadTasks() {
		return otherReadTasks;
	}

	@Override
	public List<BridgeReadTask> getRequiredReadTasks() {
		return readTasks;
	}

	@Override
	public List<BridgeWriteTask> getWriteTasks() {
		return writeTasks;
	}

	@Override
	public void init() {
		createModbusProtocol();
	}

	@Override
	public String id() {
		return thingId;
	}

	@Override
	/**
	 * Sets a Channel as required. The Range with this Channel will be added to ModbusProtocol.RequiredRanges.
	 */
	public void setAsRequired(Channel channel) {
		ModbusRange range = getProtocol().getRangeByChannel(channel);
		Iterator<BridgeReadTask> i = otherReadTasks.iterator();
		while (i.hasNext()) {
			BridgeReadTask task = i.next();
			if (((ModbusBridgeReadTask) task).getRange().equals(range)) {
				this.readTasks.add(task);
				i.remove();
			}
		}
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		createModbusProtocol();
	}

	private void createModbusProtocol() {
		try {
			this.protocol = defineModbusProtocol();
			for (ThingChannelsUpdatedListener listener : this.listeners) {
				listener.thingChannelsUpdated(this);
			}
			if (this.parent instanceof ModbusDevice) {
				ModbusDevice parent = (ModbusDevice) this.parent;
				if (parent.getBridge() instanceof ModbusBridge) {
					ModbusBridge bridge = (ModbusBridge) parent.getBridge();
					// create WriteTasks
					writeTasks = Collections.synchronizedList(new ArrayList<>());
					for (WriteableModbusRange range : protocol.getWritableRanges()) {
						writeTasks.add(new ModbusBridgeWriteTask(parent.getModbusUnitId(), bridge, range));
					}
					// create ReadTasks
					readTasks = Collections.synchronizedList(new ArrayList<>());
					otherReadTasks = Collections.synchronizedList(new ArrayList<>());
					for (ModbusRange range : protocol.getReadRanges()) {
						otherReadTasks.add(new ModbusBridgeReadTask(parent.getModbusUnitId(), bridge, range));
					}
				} else {
					log.error("Invalid Bridge Type. The bridge needs to inherit from ModbusBridge.");
				}
			} else {
				log.error("Invalid Device Type. The Device needs to inherit from ModbusDevice");
			}
		} catch (ConfigException e) {
			log.error("Failed to define modbus protocol!", e);
		} catch (Throwable t) {
			log.error("Some error occured while create ModbusProtocol", t);
		}
	}

	protected abstract ModbusProtocol defineModbusProtocol() throws ConfigException;

}
