package io.openems.impl.protocol.simulator;

import java.util.ArrayList;
import java.util.List;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.Device;

public class SimulatorBridge extends Bridge {
	protected volatile SimulatorDevice[] simulatordevices = new SimulatorDevice[0];

	@Override public void triggerWrite() {
		// not implemented
	}

	@Override protected void dispose() {
		// nothing to dispose
	}

	@Override protected void forever() {
		for (SimulatorDevice device : simulatordevices) {
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
		List<SimulatorDevice> simulatordevices = new ArrayList<>();
		for (Device device : devices) {
			if (device instanceof SimulatorDevice) {
				simulatordevices.add((SimulatorDevice) device);
			}
		}
		SimulatorDevice[] newSimulatordevices = simulatordevices.stream().toArray(SimulatorDevice[]::new);
		if (newSimulatordevices == null) {
			newSimulatordevices = new SimulatorDevice[0];
		}
		this.simulatordevices = newSimulatordevices;
		return true;
	}

	private ConfigChannel<Integer> cycleTime = new ConfigChannel<Integer>("cycleTime", this, Integer.class)
			.defaultValue(1000);

	@Override public ConfigChannel<Integer> cycleTime() {
		return cycleTime;
	}
}
