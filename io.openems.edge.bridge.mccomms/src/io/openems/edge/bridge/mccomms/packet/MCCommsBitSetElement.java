package io.openems.edge.bridge.mccomms.packet;

import com.google.common.collect.Range;
import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Channel;

import java.util.BitSet;

public class MCCommsBitSetElement extends MCCommsElement {
	
	private final Channel<Boolean>[] channels;
	private BitSet bitSet;
	
	protected MCCommsBitSetElement(Range<Integer> addressRange, Channel<Boolean>...channels) throws OpenemsException {
		super(addressRange, true, 1.0, null);
		if (((addressRange.upperEndpoint() - addressRange.lowerEndpoint()) * 8) < channels.length) {
			throw new OpenemsException("Number of channels exceeds number of bits");
		}
		this.channels = channels;
	}
	
	public static MCCommsBitSetElement newInstanceFromChannels(int startAddress, int numBytes, Channel<Boolean>...channels) throws OpenemsException {
		return new MCCommsBitSetElement(Range.closed(startAddress, (startAddress + numBytes -1)), channels);
	}
	
	@Override
	public MCCommsElement setUnsigned(boolean absolute) {
		return this;
	}
	
	@Override
	public MCCommsElement setScaleFactor(double scaleFactor) {
		return this;
	}
	
	@Override
	public Channel getChannel() throws NotImplementedException {
		throw new NotImplementedException("Not supported for BitSet elements");
	}
	
	@Override
	public MCCommsElement setChannel(Channel channel) throws NotImplementedException {
		throw new NotImplementedException("Not supported for BitSet elements");
	}
	
	@Override
	public void assignValueToChannel() throws OpenemsException {
		BitSet bitSet = BitSet.valueOf(getValueBuffer());
		for (int i = 0; i < channels.length && i < bitSet.length(); i++) {
			if (channels[i] != null) {
				channels[i].setNextValue(bitSet.get(i));
			}
		}
	}
	
	@Override
	public void getValueFromChannel() throws OpenemsException {
		BitSet bitSet = new BitSet(getValueBuffer().capacity());
		for (int i = 0; i < channels.length && i < bitSet.length(); i++) {
			if (channels[i] != null) {
				bitSet.set(i, channels[i].value().get());
				channels[i].setNextValue(bitSet.get(i));
			}
		}
		if (bitSet.length() > 0) {
			this.getValueBuffer().put(bitSet.toByteArray());
		}
	}
}
