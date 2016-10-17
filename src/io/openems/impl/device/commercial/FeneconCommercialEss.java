package io.openems.impl.device.commercial;

import java.math.BigInteger;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ConfigChannelBuilder;
import io.openems.api.channel.WriteableChannel;
import io.openems.api.device.nature.Ess;
import io.openems.api.exception.OpenemsModbusException;
import io.openems.impl.protocol.modbus.ModbusChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.WriteableModbusChannel;
import io.openems.impl.protocol.modbus.internal.ElementBuilder;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.ModbusRange;
import io.openems.impl.protocol.modbus.internal.channel.ModbusChannelBuilder;
import io.openems.impl.protocol.modbus.internal.channel.WriteableModbusChannelBuilder;

public class FeneconCommercialEss extends ModbusDeviceNature implements Ess {
	private final WriteableModbusChannel _activePower = new WriteableModbusChannelBuilder().unit("W").build();
	private final ModbusChannel _allowedCharge = new ModbusChannelBuilder().unit("W").build();
	private final ModbusChannel _allowedDischarge = new ModbusChannelBuilder().unit("W").build();
	private final ConfigChannel _minSoc = new ConfigChannelBuilder().defaultValue(DEFAULT_MINSOC).percentType().build();
	private final ModbusChannel _soc = new ModbusChannelBuilder().percentType().build();

	public FeneconCommercialEss(String thingId) {
		super(thingId);
	}

	@Override
	public Channel allowedCharge() {
		return _allowedCharge;
	}

	@Override
	public Channel allowedDischarge() {
		return _allowedDischarge;
	}

	@Override
	public Channel minSoc() {
		return _minSoc;
	}

	@Override
	public WriteableChannel setActivePower() {
		return _activePower;
	}

	@Override
	public void setMinSoc(Integer minSoc) {
		this._minSoc.updateValue(BigInteger.valueOf(minSoc));
	}

	@Override
	public Channel soc() {
		return _soc;
	}

	@Override
	public String toString() {
		return "FeneconCommercialEss [activePower=" + _activePower.toSimpleString() + ", minSoc="
				+ _minSoc.toSimpleString() + ", soc=" + _soc.toSimpleString() + "]";
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsModbusException {

		return new ModbusProtocol( //
				new ModbusRange(0x0230, //
						new ElementBuilder().address(0x0230).channel(_allowedCharge).multiplier(100).signed().build(),
						new ElementBuilder().address(0x0231).channel(_allowedDischarge).multiplier(100).build()),
				new ModbusRange(0x1402, //
						new ElementBuilder().address(0x1402).channel(_soc).build()),
				new ModbusRange(0x0501, //
						new ElementBuilder().address(0x0501).channel(_activePower).multiplier(100).signed().build() //
				));
	}
}
