package io.openems.impl.protocol.modbus;

import io.openems.api.device.Device;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.OpenemsException;
import io.openems.api.thing.IsConfig;

public abstract class ModbusDevice extends Device {
	private Integer modbusUnitId = null;

	public ModbusDevice() throws OpenemsException {
		super();
	}

	@IsConfig("modbusUnitId")
	public void setModbusUnitId(Integer modbusUnitId) {
		this.modbusUnitId = modbusUnitId;
	}

	public final void update(ModbusBridge modbusBridge) throws OpenemsException {
		if (modbusUnitId == null) {
			throw new OpenemsException("No ModbusUnitId configured for Device[" + this.getThingId() + "]");
		}
		for (DeviceNature nature : getDeviceNatures()) {
			if (nature instanceof ModbusDeviceNature) {
				((ModbusDeviceNature) nature).update(modbusUnitId, modbusBridge);
			}
		}
	}

	protected int getModbusUnitId() {
		return modbusUnitId;
	}
}
