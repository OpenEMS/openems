package io.openems.impl.device.bcontrol;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.modbus.ModbusDevice;

@ThingInfo(title = "B-Control Energy Meter")
public class BControl extends ModbusDevice {

	public BControl(Bridge parent) throws OpenemsException {
		super(parent);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Meter", description = "Sets the meter nature.", type = BControlMeter.class)
	public final ConfigChannel<BControlMeter> meter = new ConfigChannel<>("meter", this);

	/*
	 * Methods
	 */
	@Override
	protected Set<DeviceNature> getDeviceNatures() {
		Set<DeviceNature> natures = new HashSet<>();
		if (meter.valueOptional().isPresent()) {
			natures.add(meter.valueOptional().get());
		}
		return natures;
	}
}
