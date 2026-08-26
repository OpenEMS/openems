package io.openems.edge.goodwe.update;

import java.util.Arrays;

public record GoodWeUpdateResponse(byte[] header, byte[] version, byte[] index, byte state, byte[] checksum) {

	/**
	 * Creates a response from the received bytes.
	 * 
	 * @param receivedBytes the received bytes
	 * @return the response or null if not parsable
	 */
	public static GoodWeUpdateResponse fromBytes(byte[] receivedBytes) {

		if (receivedBytes == null) {
			return null;
		}
		if (receivedBytes.length < 11) {
			return null;
		}

		// TODO: ease
		var state = Arrays.copyOfRange(receivedBytes, 10, 11);

		byte[] checksum = null;
		if (receivedBytes.length == 13 || receivedBytes.length == 12) {
			checksum = Arrays.copyOfRange(receivedBytes, 11, receivedBytes.length);
		}

		/*
		 * Format: Head Type Code Index State CheckSum
		 *
		 * -> 6Bytes(0xAA557f8002A9) 2Byte 2Byte 1 Bytes 2Bytes
		 */
		return new GoodWeUpdateResponse(//
				Arrays.copyOfRange(receivedBytes, 0, 6), //
				Arrays.copyOfRange(receivedBytes, 6, 8), //
				Arrays.copyOfRange(receivedBytes, 8, 10), //
				state[0], //
				checksum //
		);
	}

}
