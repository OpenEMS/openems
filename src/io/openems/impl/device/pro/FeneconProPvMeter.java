package io.openems.impl.device.pro;

import io.openems.api.channel.IsChannel;
import io.openems.api.channel.numeric.NumericChannel;
import io.openems.api.channel.numeric.NumericChannelBuilder;
import io.openems.api.channel.numeric.NumericChannelBuilder.Aggregation;
import io.openems.api.controller.IsThingMap;
import io.openems.api.device.nature.MeterNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.internal.ElementBuilder;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.ModbusRange;
import io.openems.impl.protocol.modbus.internal.channel.ModbusChannelBuilder;

@IsThingMap(type = MeterNature.class)
public class FeneconProPvMeter extends ModbusDeviceNature implements MeterNature {

	@IsChannel(id = "ActivePowerPhaseA")
	private final ModbusChannel _activePowerPhaseA = new ModbusChannelBuilder().nature(this).unit("W").multiplier(10)
			.build();
	@IsChannel(id = "ActivePowerPhaseB")
	private final ModbusChannel _activePowerPhaseB = new ModbusChannelBuilder().nature(this).unit("W").multiplier(10)
			.build();
	@IsChannel(id = "ActivePowerPhaseC")
	private final ModbusChannel _activePowerPhaseC = new ModbusChannelBuilder().nature(this).unit("W").multiplier(10)
			.build();
	private final NumericChannel _activePower = new NumericChannelBuilder<>().nature(this).unit("W")
			.channel(_activePowerPhaseA, _activePowerPhaseB, _activePowerPhaseC).aggregate(Aggregation.SUM).build();

	public FeneconProPvMeter(String thingId) {
		super(thingId);
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
		return _activePower;
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
		return null;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		return new ModbusProtocol( //
				new ModbusRange(143, //
						new ElementBuilder().address(143).channel(_activePowerPhaseA).build(),
						new ElementBuilder().address(144).channel(_activePowerPhaseB).build(),
						new ElementBuilder().address(145).channel(_activePowerPhaseC).build()));
	}

}
