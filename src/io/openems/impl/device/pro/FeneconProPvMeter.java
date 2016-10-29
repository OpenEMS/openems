package io.openems.impl.device.pro;

import io.openems.api.channel.NumericChannel;
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
	public NumericChannel activeNegativeEnergy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NumericChannel activePositiveEnergy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NumericChannel activePower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NumericChannel apparentEnergy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NumericChannel apparentPower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NumericChannel reactiveNegativeEnergy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NumericChannel reactivePositiveEnergy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NumericChannel reactivePower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		// TODO Auto-generated method stub
		return null;
	}

}
