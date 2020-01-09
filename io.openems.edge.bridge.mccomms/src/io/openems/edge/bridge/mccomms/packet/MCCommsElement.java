package io.openems.edge.bridge.mccomms.packet;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;

import com.google.common.collect.Range;
import com.google.common.primitives.UnsignedBytes;
import com.google.common.primitives.UnsignedInts;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Channel;

/**
 * Class for translating a serial of bytes within an {@link MCCommsPacket}
 * buffer into a value and mapping it to a {@link Channel}, and vice versa
 */
public class MCCommsElement {
	/**
	 * the address range of this element within the packet byte order
	 */
	private Range<Integer> addressRange;
	/**
	 * buffer containing raw byte values for this element
	 */
	private ByteBuffer valueBuffer;
	/**
	 * Whether to treat values as unsigned during buffer operations
	 */
	private boolean isUnsigned;
	/**
	 * Channel for the scaler values for the current element's value
	 */
	private final Channel<Double> scalerChannel;
	/**
	 * {@link Channel} bound to this element
	 */
	private final Channel<?> channel;

	/**
	 * Constructor
	 * 
	 * @param addressRange the address range of this element within the packet byte
	 *                     order
	 * @param isUnsigned   Whether to treat values as unsigned during buffer
	 *                     operations
	 * @param scaleFactor  Channel for the scaler values for the current element's
	 * 					   value
	 * @param channel      {@link Channel} to bind to this element
	 */
	protected MCCommsElement(Range<Integer> addressRange, boolean isUnsigned, Channel<Double> scalerChannel, Channel<?> channel) {
		this.addressRange = addressRange;
		this.valueBuffer = ByteBuffer.allocate(addressRange.upperEndpoint() - addressRange.lowerEndpoint() + 1);
		this.isUnsigned = isUnsigned;
		this.scalerChannel = scalerChannel;
		this.channel = channel;
	}
	
	/**
	 * Constructor used for scaler duplex elements
	 * 
	 * @param addressRange the address of the scaler duplex element within the packet byte
	 *                     order
	 */
	protected MCCommsElement(int address) {
		this.addressRange = Range.closed(address, address);
		this.valueBuffer = ByteBuffer.allocate(1);
		this.isUnsigned = false;
		this.scalerChannel = null;
		this.channel = null;
	}

	/**
	 * Static constructor for an unscaled numerical element with no bound
	 * {@link Channel}; usually a command or address field
	 * 
	 * @param startAddress the index of the first byte of this element within a
	 *                     {@link MCCommsPacket} buffer
	 * @param numBytes     the number of bytes within a {@link MCCommsPacket} buffer
	 *                     this element uses
	 * @param value        the number value to assign to the new element
	 * @param isUnsigned   Whether to treat values as unsigned during buffer
	 *                     operations
	 * @return a new MCCommsElement with a 1.0 scaling factor and no bound
	 *         {@link Channel}
	 * @throws OpenemsException if an abnormal buffer length is specified by
	 *                          {@code numBytes}
	 */
	public static MCCommsElement newUnscaledNumberInstance(int startAddress, int numBytes, Number value,
			boolean isUnsigned) throws OpenemsException {
		MCCommsElement element = new MCCommsElement(Range.closed(startAddress, (startAddress + numBytes - 1)),
				isUnsigned, null, null);
		switch (numBytes) {
		case 0:
			throw new OpenemsException("Zero length buffer");
		case 1:
			element.valueBuffer.put(0, value.byteValue());
			return element;
		case 2:
			element.valueBuffer.putShort(0, value.shortValue());
			return element;
		case 4:
			element.valueBuffer.putInt(0, value.intValue());
			return element;
		case 8:
			element.valueBuffer.putLong(0, value.longValue());
			return element;
		default:
			throw new OpenemsException("Abnormal buffer length (not a power of 2, or longer than 8 bytes)");
		}
	}

	/**
	 * Static constructor to create an MCCommsElement bound to a specified
	 * {@link Channel}
	 * 
	 * @param startAddress  the index of the first byte of this element within a
	 *                      {@link MCCommsPacket} buffer
	 * @param numBytes      the number of bytes within a {@link MCCommsPacket} buffer
	 *                      this element uses
	 * @param scalerChannel {@link Channel} carrying the scaling factor values for this element
	 * @param channel       {@link Channel} to bind to this element
	 * @return a new MCCommsElement instance with bound scaler and value channels
	 */
	public static MCCommsElement newInstanceFromChannel(int startAddress, int numBytes, Channel<Double> scalerChannel, Channel<?> channel) {
		return new MCCommsElement(Range.closed(startAddress, (startAddress + numBytes - 1)), true, scalerChannel, channel);
	}

	/**
	 * Method to assign raw byte values to this element
	 * 
	 * @param bytes the bytes to assign to the internal {@link ByteBuffer}
	 * @return the current instance
	 * @throws OpenemsException if the internal {@link ByteBuffer} has a capacity
	 *                          not equal the number of supplied bytes
	 */
	MCCommsElement setBytes(byte[] bytes) throws OpenemsException {
		if (bytes.length != valueBuffer.capacity()) {
			throw new OpenemsException("Byte array does not meet required length");
		}
		valueBuffer.position(0);
		valueBuffer.put(bytes);
		return this;
	}

	/**
	 * @return a copy of the backing array for the internal {@link ByteBuffer}
	 */
	byte[] getBytes() {
		return Arrays.copyOf(valueBuffer.array(), valueBuffer.capacity());
	}

	/**
	 * @return the address range of this element within the packet byte order
	 */
	Range<Integer> getAddressRange() {
		return addressRange;
	}

	/**
	 * Sets the address range of this element within the packet byte order
	 * 
	 * @param addressRange the desired address range
	 * @return the current instance
	 */
	public MCCommsElement setAddressRange(Range<Integer> addressRange) {
		this.addressRange = addressRange;
		return this;
	}

	/**
	 * @return the buffer containing raw byte values for this element
	 */
	public ByteBuffer getValueBuffer() {
		valueBuffer.position(0);
		return valueBuffer;
	}

	/**
	 * @return Whether to treat values as unsigned during buffer operations
	 */
	public boolean isUnsigned() {
		return isUnsigned;
	}

	/**
	 * Sets whether to treat values as unsigned during buffer operations
	 * 
	 * @param unsigned true if this instance must treat values as unsigned
	 * @return the current instance
	 */
	public MCCommsElement setUnsigned(boolean unsigned) {
		isUnsigned = unsigned;
		return this;
	}

	/**
	 * @return amount by which to scale values when pulling and pushing values to
	 *         the buffer
	 */
	public double getScaleFactor() {
		if (scalerChannel != null) {
			return scalerChannel.value().get().doubleValue();
		} else {
			return 1.0;
		}
	}
	
	/**
	 * @return the channel bound to the current element, which may be null
	 */
	public Channel<?> getBoundChannel() {
		return this.channel;
	}

	/**
	 * Reads the element value from the internal {@link ByteBuffer}, scales it, and
	 * pushes it to the bound {@link Channel}
	 * 
	 * @throws OpenemsException if the {@link io.openems.common.types.OpenemsType}
	 *                          of the bound {@link Channel}, or there is no channel
	 *                          bound to the current instance
	 */
	public void assignValueToChannel() throws OpenemsException {
		if (Optional.ofNullable(channel).isPresent()) {
			switch (channel.getType()) {
			case BOOLEAN:
				channel.setNextValue(getBufferScaledValue().intValue() != 0);
				break;
			case SHORT:
				channel.setNextValue(getBufferScaledValue().shortValue());
				break;
			case INTEGER:
				channel.setNextValue(getBufferScaledValue().intValue());
				break;
			case LONG:
				channel.setNextValue(getBufferScaledValue().longValue());
				break;
			case FLOAT:
				channel.setNextValue(getBufferScaledValue().floatValue());
				break;
			case DOUBLE:
				channel.setNextValue(getBufferScaledValue().doubleValue());
			default:
				throw new OpenemsException("Type not supported: " + channel.getType().name());
			}
		} else {
			throw new OpenemsException("No channel mapping for the current element");
		}
	}

	/**
	 * Gets a value from the internal {@link ByteBuffer} and applies the scaling
	 * factor
	 * 
	 * @see MCCommsElement#scaleFactor
	 * @return the scaled numerical value of this element instance
	 * @throws OpenemsException if the buffer has an abnormal length or an attempt
	 *                          is made to treat a 64-bit value as unsigned
	 */
	private Number getBufferScaledValue() throws OpenemsException { // TODO check if floating points are too expensive
		switch (valueBuffer.capacity()) {
		case 0:
			throw new OpenemsException("Zero length buffer");
		case 1:
			if (isUnsigned) {
				return UnsignedBytes.toInt(valueBuffer.get(0)) * getScaleFactor();
			}
			return valueBuffer.get(0) * getScaleFactor();
		case 2:
			if (isUnsigned) {
				return Short.toUnsignedInt(valueBuffer.getShort(0)) * getScaleFactor();
			}
			return valueBuffer.getShort(0);
		case 4:
			if (isUnsigned) {
				return UnsignedInts.toLong(valueBuffer.getInt(0)) * getScaleFactor();
			}
			return valueBuffer.getInt(0) * getScaleFactor();
		case 8:
			if (isUnsigned) {
				throw new OpenemsException("Unsigned values not supported for 64-bit buffers");
			}
			return valueBuffer.getLong(0) * getScaleFactor();
		default:
			throw new OpenemsException("Abnormal buffer length (not a power of 2, or longer than 8 bytes)");
		}
	}

	// no consideration given to cardinality of channel value TODO put this in
	// JavaDoc

	/**
	 * Pulls a {@link io.openems.edge.common.channel.value.Value} from the bound
	 * {@link Channel}, scales it if applicable, and pushes it to the internal
	 * {@link ByteBuffer}
	 * 
	 * @throws OpenemsException if the buffer has an abnormal length, the
	 *                          {@link Channel}
	 *                          {@link io.openems.common.types.OpenemsType} is not
	 *                          supported, or there is no {@link Channel} bound to
	 *                          the current element
	 */
	public void getValueFromChannel() throws OpenemsException {
		if (Optional.ofNullable(channel).isPresent()) {
			switch (channel.getType()) {
			case INTEGER:
			case SHORT:
			case LONG:
				long channelValue = (long) ((Long) channel.value().getOrError() / getScaleFactor());
				switch (valueBuffer.capacity()) {
				case 0:
					throw new OpenemsException("Zero length buffer");
				case 1:
					valueBuffer.put(0, (byte) channelValue);
					break;
				case 2:
					valueBuffer.putShort(0, (short) channelValue);
					break;
				case 4:
					valueBuffer.putInt(0, (int) channelValue);
					break;
				case 8:
					valueBuffer.putLong(0, channelValue);
					break;
				default:
					throw new OpenemsException("Abnormal buffer length (not a power of 2, or longer than 8 bytes)");
				}
				break;
			case BOOLEAN:
				Arrays.fill(valueBuffer.array(), ((Boolean) channel.value().getOrError()) ? ((byte) 1) : ((byte) 0));
				break;
			default:
				throw new OpenemsException("Type not supported: " + channel.getType().name());
			}
		} else {
			throw new OpenemsException("No channel mapping for the current element");
		}
	}
}
