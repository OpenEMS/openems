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
package io.openems.impl.protocol.studer;

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
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.OpenemsException;
import io.openems.api.thing.ThingChannelsUpdatedListener;
import io.openems.impl.protocol.studer.internal.StuderProtocol;
import io.openems.impl.protocol.studer.internal.property.ReadProperty;
import io.openems.impl.protocol.studer.internal.property.StuderProperty;
import io.openems.impl.protocol.studer.internal.property.WriteProperty;

@ThingInfo(title = "Studer")
public abstract class StuderDeviceNature implements DeviceNature, ChannelChangeListener {

	private final Device parent;

	/*
	 * Constructors
	 */
	public StuderDeviceNature(String thingId, Device parent) throws ConfigException {
		this.parent = parent;
		this.thingId = thingId;
		log = LoggerFactory.getLogger(this.getClass());
		this.listeners = new ArrayList<>();
	}

	/*
	 * Fields
	 */
	protected final Logger log;
	private StuderProtocol protocol = null;
	private final String thingId;
	private List<ThingChannelsUpdatedListener> listeners;
	private List<BridgeReadTask> readTasks;
	private List<BridgeReadTask> requiredReadTasks;
	private List<BridgeWriteTask> writeTasks;
	@ChannelInfo(isOptional=true,title="Alias",description="The Alias to display for the device.", type=String.class)
	public ConfigChannel<String> alias = new ConfigChannel<>("alias", this);

	/*
	 * Abstract Methods
	 */
	protected abstract StuderProtocol defineStuderProtocol() throws ConfigException;

	/*
	 * Methods
	 */
	private StuderProtocol getProtocol() {
		if (protocol == null) {
			createStuderProtocol();
		}
		return this.protocol;
	}

	@Override
	public String getAlias() {
		return alias.valueOptional().orElse(id());
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
	public void init() {
		DeviceNature.super.init();
		createStuderProtocol();
	}

	@Override
	public String id() {
		return thingId;
	}

	@Override
	public Device getParent() {
		return parent;
	}

	@Override
	public List<BridgeReadTask> getReadTasks() {
		return readTasks;
	}

	@Override
	public List<BridgeReadTask> getRequiredReadTasks() {
		return requiredReadTasks;
	}

	@Override
	public List<BridgeWriteTask> getWriteTasks() {
		return writeTasks;
	}

	@Override
	/**
	 * Sets a Channel as required. The Range with this Channel will be added to StuderProtocol.RequiredRanges.
	 */
	public void setAsRequired(Channel channel) {
		StuderProperty<?> property = getProtocol().getPropertyByChannel(channel);
		Iterator<BridgeReadTask> i = readTasks.iterator();
		while (i.hasNext()) {
			BridgeReadTask task = i.next();
			if (((StuderBridgeReadTask) task).getProperty().equals(property)) {
				this.requiredReadTasks.add(task);
				i.remove();
			}
		}
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		createStuderProtocol();
	}

	private void createStuderProtocol() {
		try {
			this.protocol = defineStuderProtocol();
			if (this.parent instanceof StuderDevice) {
				StuderDevice parent = (StuderDevice) this.parent;
				if (parent.getBridge() instanceof StuderBridge) {
					StuderBridge bridge = (StuderBridge) parent.getBridge();
					// create WriteTasks
					writeTasks = Collections.synchronizedList(new ArrayList<>());
					for (WriteProperty<?> property : protocol.getWritableProperties()) {
						writeTasks.add(new StuderBridgeWriteTask(property, bridge.getSrcAddress(),
								parent.getDstAddress(), bridge));
					}
					// create ReadTasks
					readTasks = Collections.synchronizedList(new ArrayList<>());
					requiredReadTasks = Collections.synchronizedList(new ArrayList<>());
					for (ReadProperty<?> property : protocol.getReadProperties()) {
						readTasks.add(new StuderBridgeReadTask(property, bridge.getSrcAddress(), parent.getDstAddress(),
								bridge));
					}
				} else {
					log.error("Invalid Bridge Type. The bridge needs to inherit from ModbusBridge.");
				}
			} else {
				log.error("Invalid Device Type. The Device needs to inherit from ModbusDevice");
			}
			for (ThingChannelsUpdatedListener listener : this.listeners) {
				listener.thingChannelsUpdated(this);
			}
		} catch (OpenemsException e) {
			log.error("Failed to define modbus protocol!", e);
		}
	}
}
