package io.openems.impl.device.janitza;

import io.openems.api.device.nature.IsDeviceNature;
import io.openems.api.exception.OpenemsException;
import io.openems.api.thing.IsConfig;
import io.openems.impl.protocol.modbus.ModbusDevice;

public class JanitzaUMG96RME extends ModbusDevice {

	@IsDeviceNature
	public JanitzaUMG96RMEMeter meter = null;

	public JanitzaUMG96RME() throws OpenemsException {
		super();
	}

	@IsConfig("meter")
	public void setMeter(JanitzaUMG96RMEMeter meter) {
		this.meter = meter;
	}

}
