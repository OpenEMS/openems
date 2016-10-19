package io.openems.impl.device.socomec;

import io.openems.api.device.nature.IsDeviceNature;
import io.openems.api.exception.OpenemsException;
import io.openems.api.thing.IsConfig;
import io.openems.impl.protocol.modbus.ModbusDevice;

public class Socomec extends ModbusDevice {

	@IsDeviceNature
	public SocomecMeter meter = null;

	public Socomec() throws OpenemsException {
		super();
	}

	@IsConfig("meter")
	public void setEss(SocomecMeter meter) {
		this.meter = meter;
	}

}
