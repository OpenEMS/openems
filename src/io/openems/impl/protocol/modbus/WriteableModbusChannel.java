package io.openems.impl.protocol.modbus;

import java.math.BigInteger;

import io.openems.api.channel.WriteableChannel;

public class WriteableModbusChannel extends WriteableChannel {

	public WriteableModbusChannel(String unit, BigInteger minValue, BigInteger maxValue, BigInteger multiplier,
			BigInteger delta) {
		super(unit, minValue, maxValue, multiplier, delta);
	}

	@Override
	protected void updateValue(BigInteger value) {
		super.updateValue(value);
	}
}
