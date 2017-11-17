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
package io.openems.impl.protocol.keba;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.api.bridge.BridgeReadTask;
import io.openems.api.bridge.BridgeWriteTask;
import io.openems.api.channel.Channel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.ConfigException;
import io.openems.api.thing.ThingChannelsUpdatedListener;
import io.openems.common.exceptions.OpenemsException;

public abstract class KebaDeviceNature implements DeviceNature {

	private KebaDevice parent;
	protected final Logger log;

	private final String thingId;
	private List<ThingChannelsUpdatedListener> listeners;
	private final List<BridgeReadTask> readTasks = new ArrayList<>();
	private final List<BridgeReadTask> requiredReadTasks = new ArrayList<>();
	private final List<BridgeWriteTask> writeTasks = new ArrayList<>();

	public KebaDeviceNature(String thingId, KebaDevice parent) throws ConfigException {
		this.thingId = thingId;
		this.parent = parent;
		log = LoggerFactory.getLogger(this.getClass());
		this.listeners = new ArrayList<>();
		this.readTasks.add(new BridgeReadTask() {

			private final static int REPORT_1_SECONDS = 60; // TODO increase
			private final static int REPORT_2_SECONDS = 30; // TODO increase
			private final static int REPORT_3_SECONDS = 10; // TODO increase

			private LocalDateTime nextReport1 = LocalDateTime.MIN;
			private LocalDateTime nextReport2 = LocalDateTime.MIN;
			private LocalDateTime nextReport3 = LocalDateTime.MIN;

			@Override
			protected void run() throws InterruptedException {
				try {
					// REPORT 1
					if (this.nextReport1.isBefore(LocalDateTime.now())) {
						this.nextReport1 = LocalDateTime.now().plusSeconds(REPORT_1_SECONDS);
						parent.send("report 1");
					}
					// REPORT 2
					if (this.nextReport2.isBefore(LocalDateTime.now())) {
						this.nextReport2 = LocalDateTime.now().plusSeconds(REPORT_2_SECONDS);
						parent.send("report 2");
					}
					// REPORT 3
					if (this.nextReport3.isBefore(LocalDateTime.now())) {
						this.nextReport3 = LocalDateTime.now().plusSeconds(REPORT_3_SECONDS);
						parent.send("report 3");
					}
				} catch (OpenemsException e) {
					log.error(e.getMessage());
				}
			}
		});
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
	public String id() {
		return thingId;
	}

	@Override
	public List<BridgeReadTask> getReadTasks() {
		return this.readTasks;
	}

	@Override
	public List<BridgeWriteTask> getWriteTasks() {
		return this.writeTasks;
	}

	@Override
	public List<BridgeReadTask> getRequiredReadTasks() {
		return this.requiredReadTasks;
	}

	@Override
	public Device getParent() {
		return parent;
	}

	@Override
	public void setAsRequired(Channel channel) {
		// ignore. All channels/reports are polled by default
	}

	@Override
	public void init() {
		for (ThingChannelsUpdatedListener listener : this.listeners) {
			listener.thingChannelsUpdated(this);
		}
	}

	protected abstract List<String> getWriteMessages();

	protected abstract void receive(JsonObject jMessage);
}
