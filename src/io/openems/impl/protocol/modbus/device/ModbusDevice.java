package io.openems.impl.protocol.modbus.device;

import com.ghgande.j2mod.modbus.io.ModbusTransaction;

import io.openems.api.device.Device;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.OpenemsException;
import io.openems.api.thing.IsConfigParameter;

public abstract class ModbusDevice extends Device {
	private Integer modbusUnitId = null;

	public ModbusDevice() throws OpenemsException {
		super();
	}

	@IsConfigParameter("modbusUnitId")
	public void setModbusUnitId(Integer modbusUnitId) {
		this.modbusUnitId = modbusUnitId;
	}

	public final void update(ModbusTransaction modbusTransaction) throws OpenemsException {
		for (DeviceNature nature : getDeviceNatures()) {
			if (nature instanceof ModbusDeviceNature) {
				((ModbusDeviceNature) nature).update(modbusTransaction);
			}
		}
	}

	protected int getModbusUnitId() {
		return modbusUnitId;
	}
}
