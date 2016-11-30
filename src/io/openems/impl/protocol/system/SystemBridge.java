package io.openems.impl.protocol.system;

import java.util.ArrayList;
import java.util.List;

import io.openems.api.bridge.Bridge;
import io.openems.api.device.Device;

public class SystemBridge extends Bridge {
	protected volatile SystemDevice[] systemdevices = new SystemDevice[0];

	@Override public void triggerWrite() {
		// not implemented
	}

	@Override protected void dispose() {
		// nothing to dispose
	}

	@Override protected void forever() {
		for (SystemDevice device : systemdevices) {
			device.update();
		}
	}

	@Override protected boolean initialize() {
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

	@Override protected int getCycleTime() {
		return 10000;
	}

}
