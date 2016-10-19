package io.openems.impl.protocol.modbus;

import io.openems.api.device.Device;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InjectionException;
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

	protected int getModbusUnitId() {
		return modbusUnitId;
	}

	protected final void update(ModbusBridge modbusBridge) throws ConfigException, InjectionException {
		checkModbusUnitId();
		for (DeviceNature nature : getDeviceNatures()) {
			if (nature instanceof ModbusDeviceNature) {
				((ModbusDeviceNature) nature).update(modbusUnitId, modbusBridge);
			}
		}
	}

	protected final void write(ModbusBridge modbusBridge) throws ConfigException, InjectionException {
		checkModbusUnitId();
		for (DeviceNature nature : getDeviceNatures()) {
			if (nature instanceof ModbusDeviceNature) {
				((ModbusDeviceNature) nature).write(modbusUnitId, modbusBridge);
			}
		}
	}

	private void checkModbusUnitId() throws ConfigException {
		if (modbusUnitId == null) {
			throw new ConfigException("No ModbusUnitId configured for Device[" + this.getThingId() + "]");
		}
	}
}
