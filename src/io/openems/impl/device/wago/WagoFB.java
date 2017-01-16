package io.openems.impl.device.wago;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.modbus.ModbusDevice;

public class WagoFB extends ModbusDevice {

	@ConfigInfo(title = "Output configuration", type = WagoFBOutput.class)
	public final ConfigChannel<WagoFBOutput> output = new ConfigChannel<WagoFBOutput>("output", this);

	@ConfigInfo(title = "Input configuration", type = WagoFBInput.class)
	public final ConfigChannel<WagoFBInput> input = new ConfigChannel<WagoFBInput>("output", this);

	public WagoFB() throws OpenemsException {
		super();
	}

	@Override
	protected Set<DeviceNature> getDeviceNatures() {
		Set<DeviceNature> natures = new HashSet<>();
		if (output.valueOptional().isPresent()) {
			natures.add(output.valueOptional().get());
		}
		if (input.valueOptional().isPresent()) {
			natures.add(input.valueOptional().get());
		}
		return natures;
	}

}
