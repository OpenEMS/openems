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
package io.openems.impl.protocol.system;

import java.util.ArrayList;
import java.util.List;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.Device;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;

@ThingInfo(title = "Operating system")
public class SystemBridge extends Bridge {
	protected volatile SystemDevice[] systemdevices = new SystemDevice[0];

	@Override
	public void triggerWrite() {
		// not implemented
	}

	@Override
	protected void dispose() {
		// nothing to dispose
	}

	@Override
	protected void forever() {
		for (SystemDevice device : systemdevices) {
			device.update();
		}
	}

	@Override
	protected boolean initialize() {
		/*
		 * Wait a little bit, because the simulator is much faster than real hardware.
		 * Otherwise the system waits 10 seconds to call initialize() again.
		 */
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		/*
		 * Copy and cast devices to local simulatordevices array
		 */
		if (devices.isEmpty()) {
			return false;
		}
		List<SystemDevice> systemdevices = new ArrayList<>();
		for (Device device : devices) {
			if (device instanceof SystemDevice) {
				systemdevices.add((SystemDevice) device);
			}
		}
		SystemDevice[] newSystemdevices = systemdevices.stream().toArray(SystemDevice[]::new);
		if (newSystemdevices == null) {
			newSystemdevices = new SystemDevice[0];
		}
		this.systemdevices = newSystemdevices;
		return true;
	}

	private ConfigChannel<Integer> cycleTime = new ConfigChannel<Integer>("cycleTime", this).defaultValue(10000);

	@Override
	@ConfigInfo(title = "Sets the duration of each cycle in milliseconds", type = Integer.class)
	public ConfigChannel<Integer> cycleTime() {
		return cycleTime;
	}
}
