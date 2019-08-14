package io.openems.edge.bridge.mccomms.packet;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.RangeMap;
import com.google.common.primitives.UnsignedBytes;
import io.openems.common.exceptions.OpenemsException;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class MCCommsPacket {
	private final RangeMap<Integer, MCCommsElement> elements;
	
	public MCCommsPacket(MCCommsElement ... elements) {
		ImmutableRangeMap.Builder<Integer, MCCommsElement> builder = ImmutableRangeMap.builder();
		for (MCCommsElement element : elements) {
			builder.put(element.getAddressRange(), element);
		}
		this.elements = builder.build();
	}
	
	private static int calculateCRC(ByteBuffer buffer) {
		int CRC = 0;
		for (int i = 0; i < 22; i++) {
			CRC += UnsignedBytes.toInt(buffer.get(i));
		}
		return CRC;
	}
	
	public static boolean checkCRC(ByteBuffer buffer) {
		return MCCommsPacket.calculateCRC(buffer) == Short.toUnsignedInt(buffer.getShort(22));
	}
	
	public byte[] getBytes() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(25);
			byteBuffer.put(0, (byte) 83);
			for (MCCommsElement element : elements.asMapOfRanges().values()) {
				byteBuffer.position(element.getAddressRange().lowerEndpoint());
				byteBuffer.put(element.getBytes());
			}
			byteBuffer.putShort(22, (short) calculateCRC(byteBuffer));
		byteBuffer.put(24, (byte) 83);
		return byteBuffer.array();
	}
	
	public MCCommsPacket setBytesAndUpdateChannels(byte[] bytes) throws OpenemsException {
		for (MCCommsElement element : elements.asMapOfRanges().values()) {
			element.setBytes(Arrays.copyOfRange(bytes, element.getAddressRange().lowerEndpoint(), element.getAddressRange().upperEndpoint())).assignValueToChannel();
		}
		return this;
	}
}
