package io.openems.impl.device.janitza;

import io.openems.api.channel.numeric.NumericChannel;
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
	private final ModbusChannel _activePower = new ModbusChannelBuilder().nature(this).unit("W").build();
	private final ModbusChannel _apparentEnergy = new ModbusChannelBuilder().nature(this).unit("kVAh").build();
	private final ModbusChannel _apparentPower = new ModbusChannelBuilder().nature(this).unit("VA").build();
	private final ModbusChannel _reactiveNegativeEnergy = new ModbusChannelBuilder().nature(this).unit("kvarh").build();
	private final ModbusChannel _reactivePositiveEnergy = new ModbusChannelBuilder().nature(this).unit("kvarh").build();
	private final ModbusChannel _reactivePower = new ModbusChannelBuilder().nature(this).unit("var").build();

	public JanitzaUMG96RMEMeter(String thingId) {
		super(thingId);
	}

	@Override
	public NumericChannel activeNegativeEnergy() {
		return _activeNegativeEnergy;
	}

	@Override
	public NumericChannel activePositiveEnergy() {
		return _activePositiveEnergy;
	}

	@Override
	public NumericChannel activePower() {
		return _activePower;
	}

	@Override
	public NumericChannel apparentEnergy() {
		return _apparentEnergy;
	}

	@Override
	public NumericChannel apparentPower() {
		return _apparentPower;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		return new ModbusProtocol( //
				new ModbusRange(874, //
						new ElementBuilder().address(874).channel(_activePower).floatingPoint().doubleword().signed()
								.build(), //
						new ElementBuilder().address(876).dummy(882 - 876).build(), //
						new ElementBuilder().address(882).channel(_reactivePower).floatingPoint().doubleword().signed()
								.build(), //
						new ElementBuilder().address(884).dummy(890 - 884).build(), //
						new ElementBuilder().address(890).channel(_apparentPower).floatingPoint().doubleword().signed()
								.build()) //
		);
	}

	@Override
	public NumericChannel reactiveNegativeEnergy() {
		return _reactiveNegativeEnergy;
	}

	@Override
	public NumericChannel reactivePositiveEnergy() {
		return _reactivePositiveEnergy;
	}

	@Override
	public NumericChannel reactivePower() {
		return _reactivePower;
	}

}
