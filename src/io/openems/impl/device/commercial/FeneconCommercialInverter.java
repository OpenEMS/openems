package io.openems.impl.device.commercial;

import io.openems.api.channel.WriteableChannel;
import io.openems.api.device.nature.PvInverterNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.WriteableModbusChannel;
import io.openems.impl.protocol.modbus.internal.ElementBuilder;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.ModbusRange;
import io.openems.impl.protocol.modbus.internal.channel.WriteableModbusChannelBuilder;

public class FeneconCommercialInverter extends ModbusDeviceNature implements PvInverterNature {

	private final WriteableModbusChannel _setPvLimit = new WriteableModbusChannelBuilder().nature(this).unit("W")
			.multiplier(100).minValue(0).maxValue(60000).build();

	public FeneconCommercialInverter(String thingId) {
		super(thingId);
		// TODO Auto-generated constructor stub
	}

	@Override
	public WriteableChannel setLimit() {
		return _setPvLimit;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		ModbusProtocol protocoll = new ModbusProtocol(//
				new ModbusRange(0x0503, //
						new ElementBuilder().address(0x0503).channel(_setPvLimit).build()//
				));
		return protocoll;
	}
}
