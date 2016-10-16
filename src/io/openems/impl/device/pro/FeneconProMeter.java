package io.openems.impl.device.pro;

import io.openems.api.channel.Channel;
import io.openems.api.device.nature.Meter;
import io.openems.api.exception.OpenemsModbusException;
import io.openems.impl.protocol.modbus.device.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;

public class FeneconProMeter extends ModbusDeviceNature implements Meter {

	private Channel activePower = new Channel();

	public FeneconProMeter(String thingId) {
		super(thingId);
	}

	@Override
	public Channel getActivePower() {
		return activePower;
	}

	@Override
	public String toString() {
		return "FeneconProMeter [getThingId()=" + getThingId() + "]";
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsModbusException {
		// TODO Auto-generated method stub
		return null;
	}
}
