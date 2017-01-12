package io.openems.impl.device.kmtronic;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.modbus.ModbusDevice;

public class KMTronicRelay extends ModbusDevice {

	public final ConfigChannel<KMTronicRelayOutput> output = new ConfigChannel<KMTronicRelayOutput>("output", this,
			KMTronicRelayOutput.class);

	public KMTronicRelay() throws OpenemsException {
		super();
	}

	@Override protected Set<DeviceNature> getDeviceNatures() {
		Set<DeviceNature> natures = new HashSet<>();
		if (output.valueOptional().isPresent()) {
			natures.add(output.valueOptional().get());
		}
		return natures;
	}

}
