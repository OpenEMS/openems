package io.openems.impl.device.simulator;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.simulator.SimulatorDevice;

public class Simulator extends SimulatorDevice {

	/*
	 * Config
	 */
	public final ConfigChannel<SimulatorEss> ess = new ConfigChannel<SimulatorEss>("ess", this, SimulatorEss.class);
	public final ConfigChannel<SimulatorMeter> meter = new ConfigChannel<SimulatorMeter>("meter", this,
			SimulatorMeter.class);

	public Simulator() throws OpenemsException {
		super();
	}

	@Override protected Set<DeviceNature> getDeviceNatures() {
		Set<DeviceNature> natures = new HashSet<>();
		if (ess.valueOptional().isPresent()) {
			natures.add(ess.valueOptional().get());
		}
		if (meter.valueOptional().isPresent()) {
			natures.add(meter.valueOptional().get());
		}
		return natures;
	}

}
