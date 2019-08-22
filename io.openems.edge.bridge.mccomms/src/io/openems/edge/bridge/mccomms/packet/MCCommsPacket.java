package io.openems.edge.bridge.mccomms.packet;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.RangeMap;
import com.google.common.primitives.UnsignedBytes;
import io.openems.common.exceptions.OpenemsException;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;

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
		byteBuffer.put(0, (byte) 83); //start char
		for (int i = 1; i < 25; i++) {
			if (!Optional.ofNullable(elements.get(i)).isPresent()) {
				byteBuffer.put(i, (byte) 170); //0xAA for non-present elements
			} else {
				//put bytes from present elements
				byteBuffer.position(elements.get(i).getAddressRange().lowerEndpoint());
				byteBuffer.put(elements.get(i).getBytes());
				i = elements.get(i).getAddressRange().upperEndpoint() + 1; //shift i to byte address after end of current element
			}
		}
		byteBuffer.put(6, (byte) 15); //length
		byteBuffer.putShort(22, (short) calculateCRC(byteBuffer)); //crc
		byteBuffer.put(24, (byte) 83); //end char
		return byteBuffer.array();
	}
	
	public MCCommsPacket setBytes(byte[] bytes) throws OpenemsException {
		for (MCCommsElement element : elements.asMapOfRanges().values()) {
			element.setBytes(Arrays.copyOfRange(bytes, element.getAddressRange().lowerEndpoint(), element.getAddressRange().upperEndpoint()));
		}
		return this;
	}
	
	public MCCommsPacket updateElementChannels() throws OpenemsException {
		for (MCCommsElement element : elements.asMapOfRanges().values()) {
			element.assignValueToChannel();
		}
		return this;
	}
}
