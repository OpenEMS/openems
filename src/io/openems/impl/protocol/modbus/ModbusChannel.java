package io.openems.impl.protocol.modbus;

import java.math.BigInteger;
import java.util.Map;

import io.openems.api.channel.Channel;
import io.openems.api.device.nature.DeviceNature;

public class ModbusChannel extends Channel {
	public ModbusChannel(DeviceNature nature, String unit, BigInteger minValue, BigInteger maxValue,
			BigInteger multiplier, BigInteger delta, Map<BigInteger, String> labels) {
		super(nature, unit, minValue, maxValue, multiplier, delta, labels);
	}

	@Override
	protected void updateValue(BigInteger value) {
		super.updateValue(value);
	}
}
