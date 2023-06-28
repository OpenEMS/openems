package io.openems.edge.bridge.modbus.api.task;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.util.BitVector;

public class Utils {

	/**
	 * Convert a {@link BitVector} to a {@link Boolean} array.
	 * 
	 * @param v the {@link BitVector}
	 * @return the {@link Boolean} array
	 */
	public static Boolean[] toBooleanArray(BitVector v) {
		var bools = new Boolean[v.size()];
		for (var i = 0; i < v.size(); i++) {
			bools[i] = v.getBit(i);
		}
		return bools;
	}

	/**
	 * Convert a byte array to a {@link Boolean} array.
	 * 
	 * @param bytes the byte array
	 * @return the {@link Boolean} array
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
	 * Converts an array of {@link InputRegister}s to a String.
	 * 
	 * @param registers the {@link InputRegister} array
	 * @return the String
	 */
	public static String toBitString(InputRegister[] registers) {
		return Arrays.stream(registers).map(register -> {
			var bs = register.toBytes();

			return String.format("%8s", //
					Integer.toBinaryString(bs[0] & 0xFF)).replace(' ', '0') //
					+ " " //
					+ String.format("%8s", //
							Integer.toBinaryString(bs[1] & 0xFF)).replace(' ', '0');
		}).collect(Collectors.joining(" "));
	}

	/**
	 * Converts an array of bytes to a String.
	 * 
	 * @param bs the byte array
	 * @return the String
	 */
	public static String toBitString(byte[] bs) {
		return IntStream //
				.range(0, bs.length) //
				.map(idx -> bs[idx]) //
				.mapToObj(b -> String.format("%8s", //
						Integer.toBinaryString((byte) b & 0xFF)).replace(' ', '0'))
				.collect(Collectors.joining(" "));
	}
}
