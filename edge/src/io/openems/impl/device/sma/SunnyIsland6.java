package io.openems.impl.device.sma;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.modbus.ModbusDevice;

@ThingInfo(title="SMA SunnyIsland 6.0H")
public class SunnyIsland6 extends ModbusDevice{

	public SunnyIsland6(Bridge parent) throws OpenemsException {
		super(parent);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess nature.", type = SunnyIsland6Ess.class)
	public final ConfigChannel<SunnyIsland6Ess> ess = new ConfigChannel<>("ess", this);

	/*
	 * Methods
	 */
	@Override
	public String toString() {
		return "FeneconMini [ess=" + ess + ", getThingId()=" + id() + "]";
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
