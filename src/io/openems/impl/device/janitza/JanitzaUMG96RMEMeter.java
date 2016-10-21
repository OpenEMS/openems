package io.openems.impl.device.janitza;

import io.openems.api.channel.Channel;
import io.openems.api.device.nature.MeterNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.internal.ElementBuilder;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.ModbusRange;
import io.openems.impl.protocol.modbus.internal.channel.ModbusChannelBuilder;

public class JanitzaUMG96RMEMeter extends ModbusDeviceNature implements MeterNature {

	// TODO multiplier 0.1
	private final ModbusChannel _activeNegativeEnergy = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _activePositiveEnergy = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _activePower = new ModbusChannelBuilder().nature(this).unit("W").multiplier(10).build();
	private final ModbusChannel _apparentEnergy = new ModbusChannelBuilder().nature(this).unit("kVAh").build();
	private final ModbusChannel _apparentPower = new ModbusChannelBuilder().nature(this).unit("VA").multiplier(10)
			.build();
	private final ModbusChannel _reactiveNegativeEnergy = new ModbusChannelBuilder().nature(this).unit("kvarh").build();
	private final ModbusChannel _reactivePositiveEnergy = new ModbusChannelBuilder().nature(this).unit("kvarh").build();
	private final ModbusChannel _reactivePower = new ModbusChannelBuilder().nature(this).unit("var").multiplier(10)
			.build();

	public JanitzaUMG96RMEMeter(String thingId) {
		super(thingId);
	}

	@Override
	public Channel activeNegativeEnergy() {
		return _activeNegativeEnergy;
	}

	@Override
	public Channel activePositiveEnergy() {
		return _activePositiveEnergy;
	}

	@Override
	public Channel activePower() {
		return _activePower;
	}

	@Override
	public Channel apparentEnergy() {
		return _apparentEnergy;
	}

	@Override
	public Channel apparentPower() {
		return _apparentPower;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		return new ModbusProtocol( //
				new ModbusRange(3923, //
						new ElementBuilder().address(3923).channel(_activePower).signed().build(), //
						new ElementBuilder().address(3927).channel(_reactivePower).signed().build(), //
						new ElementBuilder().address(3931).channel(_apparentPower).build()) //
		);
	}

	@Override
	public Channel reactiveNegativeEnergy() {
		return _reactiveNegativeEnergy;
	}

	@Override
	public Channel reactivePositiveEnergy() {
		return _reactivePositiveEnergy;
	}

	@Override
	public Channel reactivePower() {
		return _reactivePower;
	}

}
