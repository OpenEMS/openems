package io.openems.edge.bridge.mccomms.Element;

import java.nio.ByteBuffer;

import com.google.common.collect.Range;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Channel;

public class MCCommsElement {
	private Range<Integer> addressRange;
	private ByteBuffer valueBuffer;
	private boolean isAbsolute;
	private double scaleFactor;
	private Channel channel;
	
	private MCCommsElement(Range<Integer> addressRange, boolean isAbsolute, double scaleFactor, Channel channel) {
		this.addressRange = addressRange;
		this.valueBuffer = ByteBuffer.allocate(8);
		this.isAbsolute = isAbsolute;
		this.scaleFactor = scaleFactor;
		this.channel = channel;
	}
	
	public static MCCommsElement newInstanceFromChannel(Channel channel) {
		return new MCCommsElement(Range.closed(0,0), true, 1.0, channel);
	}
	
	public MCCommsElement setBytes(byte[] bytes) {
		valueBuffer.position(Math.max(valueBuffer.capacity() - bytes.length, 0));
		valueBuffer.put(bytes, Math.max(bytes.length - valueBuffer.capacity(), 0), Math.min(valueBuffer.capacity(), bytes.length));
		return this;
	}
	
	public byte[] getBytes() {
		valueBuffer.position(valueBuffer.capacity() - (addressRange.upperEndpoint() - addressRange.lowerEndpoint() + 1));
		return valueBuffer.slice().array();
	}
	
	public Range<Integer> getAddressRange() {
		return addressRange;
	}
	
	public MCCommsElement setAddressRange(Range<Integer> addressRange) {
		this.addressRange = addressRange;
		return this;
	}
	
	public ByteBuffer getValueBuffer() {
		return valueBuffer;
	}
	
	public boolean isAbsolute() {
		return isAbsolute;
	}
	
	public MCCommsElement setAbsolute(boolean absolute) {
		isAbsolute = absolute;
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
		ByteBuffer tempBuffer;
		switch (channel.getType()) {
			case BOOLEAN:
				channel.setNextValue(valueBuffer.getLong(0) != 0);
				break;
			case SHORT:
				if (isAbsolute) {
					tempBuffer = ByteBuffer.allocate(2);
					tempBuffer.position(1);
					tempBuffer.put(getBytes());
					channel.setNextValue((short) (tempBuffer.getShort(0) * scaleFactor));
					break;
				} else {
					channel.setNextValue((short) (valueBuffer.getShort(6) * scaleFactor));
					break;
				}
			case INTEGER:
				if (isAbsolute) {
					tempBuffer = ByteBuffer.allocate(4);
					tempBuffer.position(2);
					tempBuffer.put(getBytes());
					channel.setNextValue((int) (tempBuffer.getInt(0) * scaleFactor));
					break;
				}
			case LONG:
				if (isAbsolute) {
					throw new OpenemsException("Absolute values not supported for Long type");
				}
				channel.setNextValue((long) (valueBuffer.getLong(0) * scaleFactor));
				break;
			default:
				throw new OpenemsException("Type not supported: " + channel.getType().name());
		}
	}
	
	public void getValueFromChannel() throws OpenemsException {
		switch (channel.getType()) {
			case INTEGER:
				valueBuffer.putLong(0, ((int) channel.value().getOrError()));
				break;
			case SHORT:
				valueBuffer.putLong(0, ((short) channel.value().getOrError()));
				break;
			case LONG:
				valueBuffer.putLong(0, ((long) channel.value().getOrError()));
				break;
			case BOOLEAN:
				valueBuffer.put(7, ((boolean) channel.value().getOrError()) ? ((byte) 1) : ((byte) 0));
				break;
			default:
				throw new OpenemsException("Type not supported: " + channel.getType().name());
		}
	}
}
