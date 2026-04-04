package io.openems.edge.bridge.modbus.ascii;

/**
 * Replaces non-standard ABL eMH1 Modbus/ASCII frame-start bytes in a raw byte
 * buffer.
 *
 * <p>
 * <strong>Why this class exists separately from
 * {@link AblCompatibleSerialConnection}:</strong><br>
 * The replacement logic is pure byte manipulation with no dependency on
 * hardware or native libraries. However, {@link AblCompatibleSerialConnection}
 * extends {@code SerialConnection} from j2mod, whose class loading triggers the
 * jssc native serial-port library. Loading that native library fails in unit
 * test environments (CI, WSL, any machine without a physical serial port). By
 * keeping the testable logic here — in a plain class with no j2mod dependency —
 * it can be unit-tested in isolation without requiring any native libraries or
 * hardware.
 */
final class AblFrameStartReplacer {

	/** Standard Modbus/ASCII frame-start character. */
	static final byte STANDARD_FRAME_START = ':'; // 0x3A

	/** Non-standard frame-start character sent by the ABL eMH1. */
	static final byte ABL_FRAME_START = '>'; // 0x3E

	private AblFrameStartReplacer() {
	}

	/**
	 * Replaces any ABL eMH1 non-standard frame-start bytes ({@code >}, 0x3E)
	 * in the buffer for answers from the ABL hardware with the 
	 * standard Modbus/ASCII frame-start byte ({@code :}, 0x3A).
	 *
	 * @param buffer the byte buffer to modify in place
	 * @param count  the number of valid bytes in the buffer
	 */
	static void replace(byte[] buffer, int count) {
		for (var i = 0; i < count; i++) {
			if (buffer[i] == ABL_FRAME_START) {
				buffer[i] = STANDARD_FRAME_START;
			}
		}
	}
}
