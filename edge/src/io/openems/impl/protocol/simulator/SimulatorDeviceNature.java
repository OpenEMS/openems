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
package io.openems.impl.protocol.simulator;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.bridge.BridgeReadTask;
import io.openems.api.bridge.BridgeWriteTask;
import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.thing.ThingChannelsUpdatedListener;

@ThingInfo(title = "Simulator")
public abstract class SimulatorDeviceNature implements DeviceNature {

	private Device parent;
	private List<BridgeReadTask> readTasks = new ArrayList<>();

	/*
	 * Constructors
	 */
	public SimulatorDeviceNature(String thingId, Device parent) throws ConfigException {
		this.thingId = thingId;
		this.parent = parent;
		log = LoggerFactory.getLogger(this.getClass());
		this.listeners = new ArrayList<>();
		readTasks.add(new BridgeReadTask() {

			@Override
			protected void run() throws Exception {
				SimulatorDeviceNature.this.update();
			}
		});
	}

	/*
	 * Fields
	 */
	protected final Logger log;
	private final String thingId;
	private List<ThingChannelsUpdatedListener> listeners;

	@ChannelInfo(isOptional=true,title="Alias",description="The Alias to display for the device.", type=String.class)
	public ConfigChannel<String> alias = new ConfigChannel<>("alias", this);

	/*
	 * Methods
	 */
	@Override
	public String id() {
		return thingId;
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
	public List<BridgeReadTask> getReadTasks() {
		// not required
		return null;
	}

	@Override
	public List<BridgeReadTask> getRequiredReadTasks() {
		return readTasks;
	}

	@Override
	public List<BridgeWriteTask> getWriteTasks() {
		// not required
		return null;
	}

	@Override
	public Device getParent() {
		return this.parent;
	}

	@Override
	/**
	 * Sets a Channel as required. The Range with this Channel will be added to ModbusProtocol.RequiredRanges.
	 */
	public void setAsRequired(Channel channel) {
		// ignore
	}

	protected abstract void update();

	@Override
	public void init() {
		for (ThingChannelsUpdatedListener listener : this.listeners) {
			listener.thingChannelsUpdated(this);
		}
	}
}
