package io.openems.edge.bridge.mccomms.packet;

import java.nio.ByteBuffer;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Channel;

/**
 * A subclass of {@link MCCommsElement} specially adapted to handle scaler duplex values, as illustrated below
 * <p>
 * {@code
 *    a sign  b sign
 *       |       |
 *       V       V 
 *      |_|_|_|_|_|_|_|_|
 *         ^ ^ ^ | ^ ^ ^
 *        a value| b value
 *        bits   | bits
 * }
 * </p>
 */
public class MCCommsScalerDuplexElement extends MCCommsElement{
	private final Channel<Double> firstScalerChannel, secondScalerChannel;

	/**
	 * Constructor
	 * 
	 * @param byteAddress the byte address within the packet buffer where the scaler duplex element resides
	 * @param firstScalerChannel the channel to assign the first scaler value to
	 * @param secondScalerChannel the channel to assign the second scaler value to
	 */
	private MCCommsScalerDuplexElement(int byteAddress, Channel<Double> firstScalerChannel, Channel<Double> secondScalerChannel) {
		super(byteAddress);
		this.firstScalerChannel = firstScalerChannel;
		this.secondScalerChannel = secondScalerChannel;
	}
	
	public static MCCommsScalerDuplexElement newInstanceFromChannels(int byteAddress, Channel<Double> firstScalerChannel, Channel<Double> secondScalerChannel) {
		return new MCCommsScalerDuplexElement(byteAddress, firstScalerChannel, secondScalerChannel);
	}

	/**
	 * @param bytes the bytes to assign to the internal {@link ByteBuffer}; this subclass also immediately assigns the supplied value to the bound channel.
	 * @return the current instance
	 * @throws OpenemsException if the internal {@link ByteBuffer} has a capacity
	 *                          not equal the number of supplied bytes
	 */
	@Override
	MCCommsElement setBytes(byte[] bytes) throws OpenemsException {
		if (bytes.length != getValueBuffer().capacity()) {
			throw new OpenemsException("Byte array does not meet required length");
		}
		ByteBuffer buffer = ByteBuffer.wrap(bytes).asReadOnlyBuffer();
		int combined = Byte.toUnsignedInt(buffer.get(0));
		int a = ((combined & 0x000000F0) << 24) >> 28;
		int b = ((combined & 0x0000000F) << 28) >> 28;
		double firstScaler = Math.pow(10, a);
		double secondScaler = Math.pow(10, b);
	    firstScalerChannel.setNextValue(firstScaler);
	    firstScalerChannel.nextProcessImage();
	    secondScalerChannel.setNextValue(secondScaler);
	    secondScalerChannel.nextProcessImage();
		return this;
	}

	/**
	 * Overridden; does nothing
	 * @return the current instance
	 */
	@Override
	public MCCommsElement setUnsigned(boolean unsigned) {
		return this;
	}
	
	/**
	 * Overridden; does nothing
	 */
	@Override
	public void assignValueToChannel() throws OpenemsException {
		return;
	}
}
