package io.openems.impl.device.pro;

import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.modbus.ModbusDevice;

public class FeneconPro extends ModbusDevice {
	// @IsDeviceNature
	// public FeneconProEss ess = null;

	// @IsDeviceNature
	// public FeneconProMeter meter = null;

	public FeneconPro() throws OpenemsException {
		super();
	}

	// @IsConfig("ess")
	// public void setEss(FeneconProEss ess) {
	// this.ess = ess;
	// }

	// @IsConfig("meter")
	// public void setMeter(FeneconProMeter meter) {
	// this.meter = meter;
	// }

	@Override
	public String toString() {
		return "FeneconPro []";
	}
}
