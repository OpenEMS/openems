package io.openems.impl.device.pro;

import io.openems.api.channel.Channel;
import io.openems.api.device.nature.MeterNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.internal.ElementBuilder;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.ModbusRange;
import io.openems.impl.protocol.modbus.internal.channel.ModbusChannelBuilder;

public class FeneconProNapMeter extends ModbusDeviceNature implements MeterNature {

	private final ModbusChannel _reactivePower = new ModbusChannelBuilder().nature(this).unit("var").build();
	private final ModbusChannel _activePower = new ModbusChannelBuilder().nature(this).unit("W").build();

	public FeneconProNapMeter(String thingId) {
		super(thingId);
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
		return _activePower;
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
		return _reactivePower;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		return new ModbusProtocol( //
				new ModbusRange(139, //
						new ElementBuilder().address(139).channel(_activePower).signed().build(), //
						new ElementBuilder().address(140).channel(_reactivePower).signed().build() //
				));
	}

}
