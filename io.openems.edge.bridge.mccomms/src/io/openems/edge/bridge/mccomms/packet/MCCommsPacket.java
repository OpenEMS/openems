package io.openems.edge.bridge.mccomms.packet;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.RangeMap;
import com.google.common.primitives.UnsignedBytes;

import io.openems.common.exceptions.OpenemsException;

/**
 * Class for representing a valid MCComms frame and its internal elements
 * 
 * @see MCCommsElement
 */
public class MCCommsPacket {
	/**
	 * {@link RangeMap} for mapping which byte pertains to which
	 * {@link MCCommsElement}
	 */
	private final RangeMap<Integer, MCCommsElement> elements;

	/**
	 * Constructor
	 * 
	 * @param elements {@link MCCommsElement}s that must cull their values from the
	 *                 packet buffer. Does not need to be in order.
	 */
	public MCCommsPacket(MCCommsElement... elements) {
		ImmutableRangeMap.Builder<Integer, MCCommsElement> builder = ImmutableRangeMap.builder();
		for (MCCommsElement element : elements) {
			builder.put(element.getAddressRange(), element);
		}
		this.elements = builder.build();
	}

	/**
	 * Static method for calculating a CRC for a given buffer. The buffer is assumed
	 * to be 25 bytes long.
	 * 
	 * @param buffer the buffer from which to calculate
	 * @return the actual CRC for a given packet buffer
	 */
	private static int calculateCRC(ByteBuffer buffer) {
		int CRC = 0;
		for (int i = 1; i < 22; i++) { // 1st byte is excluded
			CRC += UnsignedBytes.toInt(buffer.get(i));
		}
		return CRC;
	}

	/**
	 * Public static method for checking the actual CRC value of a packet buffer
	 * against its reported CRC value
	 * 
	 * @param buffer the packet buffer for which to check the CRC value
	 * @return true if the CRC is valid, false otherwise
	 */
	public static boolean checkCRC(ByteBuffer buffer) {
		return MCCommsPacket.calculateCRC(buffer) == Short.toUnsignedInt(buffer.getShort(22));
	}

	/**
	 * @return the raw byte array for this packet
	 */
	public byte[] getBytes() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(25);
		byteBuffer.put(0, (byte) 83); // start char
		for (int i = 1; i < 25; i++) {
			if (!Optional.ofNullable(elements.get(i)).isPresent()) {
				byteBuffer.put(i, (byte) 170); // 0xAA for non-present elements
			} else {
				// put bytes from present elements
				byteBuffer.position(elements.get(i).getAddressRange().lowerEndpoint());
				byteBuffer.put(elements.get(i).getBytes());
				i = elements.get(i).getAddressRange().upperEndpoint(); // shift i to byte address at the end of current
																		// element
			}
		}
		byteBuffer.put(6, (byte) 15); // length
		byteBuffer.putShort(22, (short) calculateCRC(byteBuffer)); // crc
		byteBuffer.put(24, (byte) 69); // end char
		return byteBuffer.array();
	}

	/**
	 * Consumes a raw byte array buffer and assigns values from the buffer to the
	 * {@link MCCommsElement}s within this packet
	 * 
	 * @param bytes the buffer to be consumed. Assumed to be 25 bytes long.
	 * @return this instance
	 * @throws OpenemsException if an element cannot be assigned a value from the
	 *                          supplied buffer
	 */
	public MCCommsPacket setBytes(byte[] bytes) throws OpenemsException {
		for (MCCommsElement element : elements.asMapOfRanges().values()) {
			element.setBytes(Arrays.copyOfRange(bytes, element.getAddressRange().lowerEndpoint(),
					element.getAddressRange().upperEndpoint() + 1));
		}
		return this;
	}

	/**
	 *
	 * @return
	 * @throws OpenemsException
	 */
	public MCCommsPacket updateElementChannels() throws OpenemsException {
		for (MCCommsElement element : elements.asMapOfRanges().values()) {
			element.assignValueToChannel();
		}
		return this;
	}
}
