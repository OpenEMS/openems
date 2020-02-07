package io.openems.edge.bridge.mccomms.packet;

import com.google.common.collect.Range;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Channel;

import java.nio.ByteBuffer;
import java.util.Optional;

public class MCCommsScalerElement extends MCCommsElement{

	private MCCommsScalerElement(Range<Integer> addressRange, Channel<Double> channel) {
		super(addressRange, false, null, channel);
	}
	
	public static MCCommsScalerElement newInstanceFromChannel(int startAddress, Channel<Double> channel) {
		return new MCCommsScalerElement(Range.closed(startAddress, startAddress), channel);
	}

	/**
	 *
	 * @param bytes the bytes to assign to the internal {@link ByteBuffer}
	 * @return the current element instance
	 * @throws OpenemsException
	 */
	@Override
	MCCommsElement setBytes(byte[] bytes) throws OpenemsException {
		if (bytes.length != getValueBuffer().capacity()) {
			throw new OpenemsException("Byte array does not meet required length");
		}
		super.setBytes(bytes);
		assignValueToChannel();
		return this;
	}

	/**
	 * Overridden; does nothing
	 * @param unsigned not used
	 * @return the current element instance
	 */
	@Override
	public MCCommsElement setUnsigned(boolean unsigned) {
		return this;
	}

	/**
	 * Reads the element value from the internal {@link ByteBuffer}, and
	 * pushes it to the bound {@link Channel}
	 *
	 * @throws OpenemsException if  there is no channel bound to the current instance
	 */
	@Override
	public void assignValueToChannel() throws OpenemsException {
		if (Optional.ofNullable(getBoundChannel()).isPresent()) {
			this.getBoundChannel().setNextValue(Math.pow(10, getValueBuffer().get(0)));
		} else {
			throw new OpenemsException("No channel mapping for the current element");
		}
	}

}
