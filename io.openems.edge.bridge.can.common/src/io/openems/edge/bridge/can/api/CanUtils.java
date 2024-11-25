package io.openems.edge.bridge.can.api;

import com.google.common.io.BaseEncoding;

public class CanUtils {

	/**
	 * Turns a byte array into a boolean array.
	 *
	 * @param bytes the byte array
	 * @return the boolean array
	 */
	public static Boolean[] toBooleanArray(byte[] bytes) {
		var bools = new Boolean[bytes.length * 8];
		for (var i = 0; i < bytes.length * 8; i++) {
			var byteIndex = i / 8;
			bools[i] = (bytes[byteIndex] & (byte) (128 / Math.pow(2, i % 8))) != 0;
		}
		return bools;
	}

	/**
	 * Gets the least significant byte of a given short value.
	 *
	 * @param val the value as short
	 * @return the least significant byte
	 */
	public static byte getLsb(short val) {
		return (byte) (val & 0xff);
	}

	/**
	 * Gets the most significant byte of a given short value.
	 *
	 * @param val the value as short
	 * @return the most significant byte
	 */
	public static byte getMsb(short val) {
		return (byte) (val >> 8 & 0xff);
	}

	/**
	 * Gets the lowest byte of a given int value.
	 *
	 * @param val the value as int
	 * @return the lowest byte
	 */
	public static byte getLowestByte(int val) {
		return (byte) (val & 0xff);
	}

	/**
	 * Gets the second lowest byte of a given int value.
	 *
	 * @param val the value as int
	 * @return the second lowest byte
	 */
	public static byte getSecondLowestByte(int val) {
		return (byte) (val >> 8 & 0xff);
	}

	/**
	 * Gets the second highest byte of a given int value.
	 *
	 * @param val the value as int
	 * @return the second highest byte
	 */
	public static byte getSecondHighestByte(int val) {
		return (byte) (val >> 16 & 0xff);
	}

	/**
	 * Gets the highest byte of a given int value.
	 *
	 * @param val the value as int
	 * @return the highest byte
	 */
	public static byte getHighestByte(int val) {
		return (byte) (val >> 24 & 0xff);
	}

	/**
	 * Turns a byte array into a String.
	 *
	 * @param data the byte array
	 * @return '(hex)' appended by the base16 encoded byte array, or 'no data'
	 */
	public static String getHexInfo(byte[] data) {
		if (data == null || data.length <= 0) {
			return "no data";
		}
		var buf = new StringBuilder("(hex) ");
		var offset = buf.length();
		buf.append(new StringBuilder(BaseEncoding.base16().lowerCase().encode(data)));
		for (var i = data.length - 2; i >= 0; i--) {
			buf.insert(offset + 2 + i * 2, " ");
		}
		return buf.toString();
	}

}
