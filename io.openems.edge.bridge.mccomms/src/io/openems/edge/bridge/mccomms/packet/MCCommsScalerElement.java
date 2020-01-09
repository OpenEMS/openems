package io.openems.edge.bridge.mccomms.packet;

import com.google.common.collect.Range;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Channel;

public class MCCommsScalerElement extends MCCommsElement{

	private MCCommsScalerElement(Range<Integer> addressRange, Channel<Double> channel) {
		super(addressRange, false, null, channel);
	}
	
	public static MCCommsScalerElement newInstanceFromChannel(int startAddress, Channel<Double> channel) {
		return new MCCommsScalerElement(Range.closed(startAddress, startAddress), channel);
	}
	
	@Override
	MCCommsElement setBytes(byte[] bytes) throws OpenemsException {
		super.setBytes(bytes);
		this.assignValueToChannel();
		this.getBoundChannel().nextProcessImage();
		return this;
	}

	@Override
	public MCCommsElement setUnsigned(boolean unsigned) {
		return this;
	}
}
