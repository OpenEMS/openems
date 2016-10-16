package io.openems.impl.device.pro;

import io.openems.api.device.nature.IsDeviceNature;
import io.openems.api.exception.OpenemsException;
import io.openems.api.thing.IsConfigParameter;
import io.openems.impl.protocol.modbus.device.ModbusDevice;

public class FeneconPro extends ModbusDevice {
	@IsDeviceNature
	public FeneconProEss ess = null;

	@IsDeviceNature
	public FeneconProMeter meter = null;

	public FeneconPro() throws OpenemsException {
		super();
	}

	@IsConfigParameter("ess")
	public void setEss(FeneconProEss ess) {
		this.ess = ess;
	}

	@IsConfigParameter("meter")
	public void setMeter(FeneconProMeter meter) {
		this.meter = meter;
	}

	@Override
	public String toString() {
		return "FeneconPro [ess=" + ess + ", meter=" + meter + ", modbusUnitId=" + getModbusUnitId() + ", getThingId()="
				+ getThingId() + "]";
	}
}
