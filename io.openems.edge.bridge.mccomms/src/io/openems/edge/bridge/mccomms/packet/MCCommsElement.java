package io.openems.edge.bridge.mccomms.packet;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.google.common.collect.Range;

import com.google.common.primitives.UnsignedBytes;
import com.google.common.primitives.UnsignedInts;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Channel;

public class MCCommsElement {
	private Range<Integer> addressRange;
	private ByteBuffer valueBuffer;
	private boolean isUnsigned;
	private double scaleFactor;
	private Channel channel;
	
	private MCCommsElement(Range<Integer> addressRange, boolean isUnsigned, double scaleFactor, Channel channel) {
		this.addressRange = addressRange;
		this.valueBuffer = ByteBuffer.allocate(addressRange.upperEndpoint() - addressRange.lowerEndpoint() + 1);
		this.isUnsigned = isUnsigned;
		this.scaleFactor = scaleFactor;
		this.channel = channel;
	}
	
	public static MCCommsElement newInstanceFromChannel(int startAddress, int numBytes, Channel channel) {
		return new MCCommsElement(Range.closed(startAddress, (startAddress + numBytes -1)), true, 1.0, channel);
	}
	
	public static MCCommsElement newInstanceFromChannel(int startAddress, int numBytes, Channel channel, double scaleFactor) {
		return new MCCommsElement(Range.closed(startAddress, (startAddress + numBytes -1)), true, scaleFactor, channel);
	}
	
	public static MCCommsElement newInstanceFromChannel(int startAddress, int numBytes, Channel channel, double scaleFactor, boolean isUnsigned) {
		return new MCCommsElement(Range.closed(startAddress, (startAddress + numBytes -1)), isUnsigned, scaleFactor, channel);
	}
	
	MCCommsElement setBytes(byte[] bytes) throws OpenemsException {
		if (bytes.length != valueBuffer.capacity()) {
			throw new OpenemsException("Byte array does not meet required length");
		}
		valueBuffer.position(0);
		valueBuffer.put(bytes);
		return this;
	}
	
	byte[] getBytes() {
		return Arrays.copyOf(valueBuffer.array(), valueBuffer.capacity());
	}
	
	Range<Integer> getAddressRange() {
		return addressRange;
	}
	
	public MCCommsElement setAddressRange(Range<Integer> addressRange) {
		this.addressRange = addressRange;
		return this;
	}
	
	public ByteBuffer getValueBuffer() {
		valueBuffer.position(0);
		return valueBuffer;
	}
	
	public boolean isUnsigned() {
		return isUnsigned;
	}
	
	public MCCommsElement setUnsigned(boolean absolute) {
		isUnsigned = absolute;
		return this;
	}
	
	public double getScaleFactor() {
		return scaleFactor;
	}
	
	public MCCommsElement setScaleFactor(double scaleFactor) {
		this.scaleFactor = scaleFactor;
		return this;
	}
	
	public Channel getChannel() {
		return channel;
	}
	
	public MCCommsElement setChannel(Channel channel) {
		this.channel = channel;
		return this;
	}
	
	public void assignValueToChannel() throws OpenemsException {
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
	}
	
	private Number getBufferScaledValue() throws OpenemsException { //TODO check if floating points are too expensive
		switch (valueBuffer.capacity()) {
			case 0:
				throw new OpenemsException("Zero length buffer");
			case 1:
				if (isUnsigned) {
					return UnsignedBytes.toInt(valueBuffer.get(0)) * scaleFactor;
				}
				return valueBuffer.get(0)  * scaleFactor;
			case 2:
				if (isUnsigned) {
					return Short.toUnsignedInt(valueBuffer.getShort())  * scaleFactor;
				}
				return valueBuffer.getShort(0);
			case 4:
				if (isUnsigned) {
					return UnsignedInts.toLong(valueBuffer.getInt(0))  * scaleFactor;
				}
				return valueBuffer.getInt(0)  * scaleFactor;
			case 8:
				if (isUnsigned) {
					throw new OpenemsException("Absolute values not supported for 64-bit buffers");
				}
				return valueBuffer.getLong(0)  * scaleFactor;
			default:
				throw new OpenemsException("Abnormal buffer length (not a power of 2, or longer than 8 bytes)");
		}
	}
	
	//no consideration given to cardinality of channel value TODO put this in JavaDoc
	public void getValueFromChannel() throws OpenemsException {
		switch (channel.getType()) {
			case INTEGER:
			case SHORT:
			case LONG:
				long channelValue = (long) (((long) channel.value().getOrError()) * scaleFactor);
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
				Arrays.fill(valueBuffer.array(), ((boolean) channel.value().getOrError()) ? ((byte) 1) : ((byte) 0));
				break;
			default:
				throw new OpenemsException("Type not supported: " + channel.getType().name());
		}
	}
}
