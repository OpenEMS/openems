package io.openems.impl.device.commercial;

import io.openems.api.device.nature.IsDeviceNature;
import io.openems.api.exception.OpenemsException;
import io.openems.api.thing.IsConfigParameter;
import io.openems.impl.protocol.modbus.ModbusDevice;

public class FeneconCommercial extends ModbusDevice {

	@IsDeviceNature
	public FeneconCommercialEss ess = null;

	public FeneconCommercial() throws OpenemsException {
		super();
	}

	@IsConfigParameter("ess")
	public void setEss(FeneconCommercialEss ess) {
		this.ess = ess;
	}

	@Override
	public String toString() {
		return "FeneconPro [ess=" + ess + ", modbusUnitId=" + getModbusUnitId() + ", getThingId()=" + getThingId()
				+ "]";
	}
}
