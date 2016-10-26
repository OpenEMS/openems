package io.openems.impl.device.pro;

import io.openems.api.channel.Channel;
import io.openems.api.device.nature.MeterNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.channel.ModbusChannelBuilder;

public class FeneconProPvMeter extends ModbusDeviceNature implements MeterNature {

	private final ModbusChannel _reactivePower = new ModbusChannelBuilder().nature(this).unit("var").multiplier(10)
			.build();
	private final ModbusChannel _apparentPower = new ModbusChannelBuilder().nature(this).unit("VA").multiplier(10)
			.build();
	private final ModbusChannel _activePower = new ModbusChannelBuilder().nature(this).unit("W").multiplier(10).build();

	public FeneconProPvMeter(String thingId) {
		super(thingId);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Channel activeNegativeEnergy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel activePositiveEnergy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel activePower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel apparentEnergy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel apparentPower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel reactiveNegativeEnergy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel reactivePositiveEnergy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel reactivePower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		// TODO Auto-generated method stub
		return null;
	}

}
