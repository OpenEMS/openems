package io.openems.impl.device.pro;

import io.openems.api.channel.Channel;
import io.openems.api.device.nature.Ess;
import io.openems.api.exception.OpenemsModbusException;
import io.openems.impl.protocol.modbus.ModbusChannel;
import io.openems.impl.protocol.modbus.ModbusChannelBuilder;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;

public class FeneconProEss extends ModbusDeviceNature implements Ess {
	public final ModbusChannel soc = new ModbusChannelBuilder().unit("%").minValue(0).build();
	private final ModbusChannel activePower = new ModbusChannelBuilder().build();
	private final ModbusChannel minSoc = new ModbusChannelBuilder().build();

	public FeneconProEss(String thingId) {
		super(thingId);
	}

	@Override
	public Channel getActivePower() {
		return activePower;
	}

	@Override
	public Channel getSoc() {
		// TODO return soc;
		return null;
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
