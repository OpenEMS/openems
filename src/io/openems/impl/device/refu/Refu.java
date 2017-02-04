package io.openems.impl.device.refu;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.modbus.ModbusDevice;

@ThingInfo("Represents a REFU battery inverter device")
public class Refu extends ModbusDevice {

	/*
	 * Config
	 */
	@ConfigInfo(title = "Sets the ess nature", type = RefuEss.class)
	public final ConfigChannel<RefuEss> ess = new ConfigChannel<RefuEss>("ess", this);

	public Refu() throws OpenemsException {
		super();
	}

	@Override
	public String toString() {
		return "FeneconCommercialAC [ess=" + ess + ", getThingId()=" + id() + "]";
	}

	@Override
	protected Set<DeviceNature> getDeviceNatures() {
		Set<DeviceNature> natures = new HashSet<>();
		if (ess.valueOptional().isPresent()) {
			natures.add(ess.valueOptional().get());
		}
		return natures;
	}

}
