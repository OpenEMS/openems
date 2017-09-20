package io.openems.impl.device.custom.riedmann;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.modbus.ModbusDevice;

@ThingInfo(title = "Custom: Riedmann PLC")
public class Riedmann extends ModbusDevice {

	@ChannelInfo(title = "", type = RiedmannNatureImpl.class)
	public final ConfigChannel<RiedmannNatureImpl> device = new ConfigChannel<RiedmannNatureImpl>("device", this);

	public Riedmann(Bridge parent) throws OpenemsException {
		super(parent);
	}

	@Override
	protected Set<DeviceNature> getDeviceNatures() {
		Set<DeviceNature> natures = new HashSet<>();
		if (device.valueOptional().isPresent()) {
			natures.add(device.valueOptional().get());
		}
		return natures;
	}

}
