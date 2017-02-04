package io.openems.impl.device.system;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.system.SystemDevice;

@ThingInfo("Represents the system device")
public class System extends SystemDevice {

	@ConfigInfo(title = "Sets the system nature", type = SystemNature.class)
	public final ConfigChannel<SystemNature> system = new ConfigChannel<SystemNature>("system", this);

	public System() throws OpenemsException {
		super();
	}

	@Override
	protected Set<DeviceNature> getDeviceNatures() {
		Set<DeviceNature> natures = new HashSet<>();
		if (system.valueOptional().isPresent()) {
			natures.add(system.valueOptional().get());
		}
		return natures;
	}

}
