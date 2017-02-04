package io.openems.impl.device.simulator;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.simulator.SimulatorDevice;

@ThingInfo("Represents a Simulated device")
public class Simulator extends SimulatorDevice {

	/*
	 * Config
	 */
	@ConfigInfo(title = "Sets the ess nature", type = SimulatorEss.class)
	public final ConfigChannel<SimulatorEss> ess = new ConfigChannel<SimulatorEss>("ess", this);
	@ConfigInfo(title = "Sets the meter nature", type = SimulatorMeter.class)
	public final ConfigChannel<SimulatorMeter> meter = new ConfigChannel<SimulatorMeter>("meter", this);

	public Simulator() throws OpenemsException {
		super();
	}

	@Override
	protected Set<DeviceNature> getDeviceNatures() {
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
