package io.openems.impl.device.pro;

import io.openems.api.channel.Channel;
import io.openems.api.device.nature.Ess;
import io.openems.api.exception.OpenemsModbusException;
import io.openems.impl.protocol.modbus.device.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;

public class FeneconProEss extends ModbusDeviceNature implements Ess {
	private final Channel activePower = new Channel();
	private final Channel minSoc = new Channel();
	private final Channel soc = new Channel();

	public FeneconProEss(String thingId) {
		super(thingId);
	}

	@Override
	public Channel getActivePower() {
		return activePower;
	}

	@Override
	public Channel getSoc() {
		return soc;
	}

	@Override
	public void setMinSoc(Integer minSoc) {
		// TODO Auto-generated method stub
	}

	@Override
	public String toString() {
		return "FeneconProEss [minSoc=" + minSoc + ", getThingId()=" + getThingId() + "]";
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsModbusException {
		// TODO Auto-generated method stub
		return null;
	}

}
