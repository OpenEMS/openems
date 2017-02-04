package io.openems.impl.protocol.system;

import java.util.ArrayList;
import java.util.List;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.Device;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;

@ThingInfo("Bridge to the system")
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
