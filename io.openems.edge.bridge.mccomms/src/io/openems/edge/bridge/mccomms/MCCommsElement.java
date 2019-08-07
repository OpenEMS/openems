package io.openems.edge.bridge.mccomms;

import java.nio.ByteBuffer;

import com.google.common.collect.Range;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;

public class MCCommsElement {
	private Range<Integer> addressRange;
	private ByteBuffer valueBuffer;
	private boolean isAbsolute;
	private double scaleFactor;
	
	
	private MCCommsElement(Range<Integer> addressRange, boolean isAbsolute) throws OpenemsException {
		this.addressRange = addressRange;
		this.isAbsolute = isAbsolute;
		this.valueBuffer = ByteBuffer.allocate(8);
		this.scaleFactor = 1.0;
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
	
	public void assignValueToChannel(Channel channel) throws OpenemsException {
		OpenemsType type = channel.getType();
		ByteBuffer tempBuffer;
		switch (type) {
			case BOOLEAN:
				channel.setNextValue(valueBuffer.get(7) > 0);
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
			break;
			case INTEGER:
				if (isAbsolute) {
					tempBuffer = ByteBuffer.allocate(4);
					tempBuffer.position(2);
					tempBuffer.put(getBytes());
					channel.setNextValue((int) (tempBuffer.getInt(0) * scaleFactor));
					break;
				}
			case LONG:
				throw new OpenemsException("Absolute values not supported for Long type");
				break;
			case FLOAT:
			case DOUBLE:
			case STRING:
				throw new OpenemsException("Type not supported: " + type.name());
				break;
		}
		
		if (isAbsolute) {
			valueBuffer.position()
		}
		else {}
	}
}
