package io.openems.impl.device.system;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.system.SystemDevice;

public class System extends SystemDevice {

	public final ConfigChannel<SystemNature> system = new ConfigChannel<SystemNature>("system", this,
			SystemNature.class);

	public System() throws OpenemsException {
		super();
	}

	@Override protected Set<DeviceNature> getDeviceNatures() {
		Set<DeviceNature> natures = new HashSet<>();
		if (system.valueOptional().isPresent()) {
			natures.add(system.valueOptional().get());
		}
		return natures;
	}

}
