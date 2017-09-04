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
package io.openems.api.device;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.bridge.Bridge;
import io.openems.api.bridge.BridgeReadTask;
import io.openems.api.bridge.BridgeWriteTask;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.OpenemsException;
import io.openems.api.thing.Thing;

public abstract class Device implements Thing {
	public final static String THINGID_PREFIX = "_device";
	private static int instanceCounter = 0;
	protected final Logger log;
	private Bridge bridge = null;
	private final String thingId;

	public Device(Bridge parent) throws OpenemsException {
		this.thingId = THINGID_PREFIX + instanceCounter++;
		log = LoggerFactory.getLogger(this.getClass());
		this.bridge = parent;
	}

	public Bridge getBridge() {
		return bridge;
	}

	@Override
	public String id() {
		return this.thingId;
	}

	@Override
	public void init() {
		Thing.super.init();
		for (DeviceNature nature : getDeviceNatures()) {
			nature.init();
		}
	}

	protected abstract Set<DeviceNature> getDeviceNatures();

	public List<BridgeReadTask> getRequiredReadTasks() {
		List<BridgeReadTask> readTasks = new ArrayList<>();
		for (DeviceNature nature : getDeviceNatures()) {
			List<BridgeReadTask> natureRequiredReadTasks = nature.getRequiredReadTasks();
			if (natureRequiredReadTasks != null) {
				readTasks.addAll(natureRequiredReadTasks);
			}
		}
		return readTasks;
	}

	public List<BridgeReadTask> getReadTasks() {
		List<BridgeReadTask> readTasks = new ArrayList<>();
		for (DeviceNature nature : getDeviceNatures()) {
			List<BridgeReadTask> natureReadTasks = nature.getReadTasks();
			if (natureReadTasks != null) {
				readTasks.addAll(natureReadTasks);
			}
		}
		return readTasks;
	}

	public List<BridgeWriteTask> getWriteTasks() {
		List<BridgeWriteTask> writeTasks = new ArrayList<>();
		for (DeviceNature nature : getDeviceNatures()) {
			List<BridgeWriteTask> natureWriteTasks = nature.getWriteTasks();
			if (natureWriteTasks != null) {
				writeTasks.addAll(natureWriteTasks);
			}
		}
		return writeTasks;
	}
}
