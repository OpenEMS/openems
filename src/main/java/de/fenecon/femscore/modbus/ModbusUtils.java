package de.fenecon.femscore.modbus;

import net.wimpi.modbus.procimg.Register;

public class ModbusUtils {
	public static String registerToBit(Register r) {
		byte[] b = r.toBytes();
		return String.format("%8s", Integer.toBinaryString(b[0] & 0xFF)).replace(' ', '0') + " "
				+ String.format("%8s", Integer.toBinaryString(b[1] & 0xFF)).replace(' ', '0');
	}

	public static int registerTo16Int(Register r) { // ByteOrder.BIG_ENDIAN
		byte[] b = r.toBytes();
		return (short) ((b[1] & 0xff) | (b[0] << 8));
		// return ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getShort();
	}

	public static long registerTo16UInt(Register r) {
		return Integer.toUnsignedLong(r.getValue());
	}

	public static int registersTo32Int(Register r1, Register r2) {
		byte[] b1 = r1.toBytes();
		byte[] b2 = r2.toBytes();
		return ((b1[0] & 0xFF) << 24) | ((b1[1] & 0xFF) << 16) | ((b2[0] & 0xFF) << 8) | (b2[1] & 0xFF);
	}

	public static long registersTo32UInt(Register r1, Register r2) {
		Integer v = registersTo32Int(r1, r2);
		return Integer.toUnsignedLong(v);
	}

	public static boolean isBitSet(byte b, int position) {
		return ((b >> position) & 1) == 1;
	}
}
