package io.openems.impl.device.commercial;

import io.openems.api.channel.Channel;
import io.openems.api.device.nature.Ess;
import io.openems.api.exception.OpenemsModbusException;
import io.openems.impl.protocol.modbus.device.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.internal.ElementBuilder;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.Range;

public class FeneconCommercialEss extends ModbusDeviceNature implements Ess {
	private final Channel activePower = new Channel();
	private final Channel minSoc = new Channel();
	private final Channel soc = new Channel();

	public FeneconCommercialEss(String thingId) {
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
		// TODO
	}

	@Override
	public String toString() {
		return "FeneconCommercialEss [minSoc=" + minSoc + ", getThingId()=" + getThingId() + "]";
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsModbusException {
		return new ModbusProtocol( //
				new Range(0x1402, //
						new ElementBuilder().address(0x1402).channel(getSoc()).build() //
				));
	}
}
