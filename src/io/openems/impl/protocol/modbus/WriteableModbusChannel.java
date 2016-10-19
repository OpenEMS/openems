package io.openems.impl.protocol.modbus;

import java.math.BigInteger;
import java.util.Map;

import io.openems.api.channel.Channel;
import io.openems.api.channel.WriteableChannel;
import io.openems.api.device.nature.DeviceNature;

public class WriteableModbusChannel extends WriteableChannel {

	public WriteableModbusChannel(DeviceNature nature, String unit, BigInteger minValue, BigInteger maxValue,
			BigInteger multiplier, BigInteger delta, Map<BigInteger, String> labels, BigInteger minWriteValue,
			Channel minWriteValueChannel, BigInteger maxWriteValue, Channel maxWriteValueChannel) {
		super(nature, unit, minValue, maxValue, multiplier, delta, labels, minWriteValue, minWriteValueChannel,
				maxWriteValue, maxWriteValueChannel);
	}

	@Override
	protected void updateValue(BigInteger value) {
		super.updateValue(value);
	}
}
