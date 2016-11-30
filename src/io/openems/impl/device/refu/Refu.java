package io.openems.impl.device.refu;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.modbus.ModbusDevice;

public class Refu extends ModbusDevice {

	/*
	 * Config
	 */
	public final ConfigChannel<RefuEss> ess = new ConfigChannel<RefuEss>("ess", this, RefuEss.class);

	public Refu() throws OpenemsException {
		super();
	}

	@Override public String toString() {
		return "FeneconCommercialAC [ess=" + ess + ", getThingId()=" + id() + "]";
	}

	@Override protected Set<DeviceNature> getDeviceNatures() {
		Set<DeviceNature> natures = new HashSet<>();
		if (ess.valueOptional().isPresent()) {
			natures.add(ess.valueOptional().get());
		}
		return natures;
	}

}
