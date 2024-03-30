package io.openems.edge.meter.tibber.pulse.smlparser;

public class ByteUtil {

	/**
	 * Converts a single byte into a string of bits.
	 *
	 * @param b the byte to convert.
	 * @return A string representation of the byte in bits.
	 */
	public static String toBits(byte b) {
		StringBuilder sb = new StringBuilder(8);
		for (int i = 7; i >= 0; i--) {
			sb.append((b & (1 << i)) == 0 ? '0' : '1');
		}
		return sb.toString();
	}

	private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

	/**
	 * Converts an array of bytes into a hexadecimal string.
	 *
	 * @param bytes the byte array to convert.
	 * @return A hexadecimal string representation of the byte array.
	 */
	public static String toHex(byte[] bytes) {
		return toHex(bytes, 0, bytes.length);
	}

	/**
	 * Converts a subset of an array of bytes into a hexadecimal string.
	 *
	 * @param bytes  the byte array to convert.
	 * @param offset the start index to convert from.
	 * @param length the number of bytes to convert.
	 * @return A hexadecimal string representation of the specified portion of the
	 *         byte array.
	 */
	public static String toHex(byte[] bytes, int offset, int length) {
		if (bytes == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (int i = 0; i < length; i++) {
			sb.append(HEX_CHARS[(bytes[offset + i] & 0xF0) >> 4]);
			sb.append(HEX_CHARS[(bytes[offset + i] & 0x0F)]);
		}
		return sb.toString();
	}

	/**
	 * Converts an 8-bit integer (byte) to a hexadecimal string.
	 *
	 * @param i the integer to convert.
	 * @return A hexadecimal string representation of the 8-bit integer.
	 */
	public static String int8ToHex(int i) {
		return toHex(new byte[] { (byte) (i & 0xFF) });
	}

	/**
	 * Converts a 16-bit integer (short) to a hexadecimal string.
	 *
	 * @param l the long integer to convert, with the value being in the range of a
	 *          short.
	 * @return A hexadecimal string representation of the 16-bit integer.
	 */
	public static String int16ToHex(long l) {
		return toHex(new byte[] { (byte) ((l >> 8) & 0xFF), (byte) (l & 0xFF) });
	}

	/**
	 * Converts a 24-bit integer to a hexadecimal string.
	 *
	 * @param l the long integer to convert, with the value being in the range of 24
	 *          bits.
	 * @return A hexadecimal string representation of the 24-bit integer.
	 */
	public static String int24ToHex(long l) {
		return toHex(new byte[] { (byte) ((l >> 16) & 0xFF), (byte) ((l >> 8) & 0xFF), (byte) (l & 0xFF) });
	}

	/**
	 * Converts a 32-bit integer to a hexadecimal string.
	 *
	 * @param l the long integer to convert, with the value being in the range of an
	 *          int.
	 * @return A hexadecimal string representation of the 32-bit integer.
	 */
	public static String int32ToHex(long l) {
		return toHex(new byte[] { (byte) ((l >> 24) & 0xFF), (byte) ((l >> 16) & 0xFF), (byte) ((l >> 8) & 0xFF),
				(byte) (l & 0xFF) });
	}

	/**
	 * Converts a 40-bit integer to a hexadecimal string.
	 *
	 * @param l the long integer to convert, with the value being in the range of 40
	 *          bits.
	 * @return A hexadecimal string representation of the 40-bit integer.
	 */
	public static String int40ToHex(long l) {
		return toHex(new byte[] { (byte) ((l >> 32) & 0xFF), (byte) ((l >> 24) & 0xFF), (byte) ((l >> 16) & 0xFF),
				(byte) ((l >> 8) & 0xFF), (byte) (l & 0xFF) });
	}

	/**
	 * Converts a 64-bit integer to a hexadecimal string.
	 *
	 * @param l the long integer to convert.
	 * @return A hexadecimal string representation of the 64-bit integer.
	 */
	public static String int64ToHex(long l) {
		return toHex(new byte[] { (byte) ((l >> 56) & 0xFF), (byte) ((l >> 48) & 0xFF), (byte) ((l >> 40) & 0xFF),
				(byte) ((l >> 32) & 0xFF), (byte) ((l >> 24) & 0xFF), (byte) ((l >> 16) & 0xFF),
				(byte) ((l >> 8) & 0xFF), (byte) (l & 0xFF) });
	}

	/**
	 * Returns the array of HEX characters.
	 * 
	 * @return An array of HEX characters.
	 */
	public static char[] getHexChars() {
		return HEX_CHARS.clone(); // Returns a copy to ensure immutability
	}

}