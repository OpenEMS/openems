package io.openems.edge.bridge.modbus.ascii;

import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;

/**
 * A {@link SerialConnection} subclass that transparently replaces the
 * non-standard ABL eMH1 response frame-start delimiter {@code >} (0x3E) with
 * the standard Modbus/ASCII frame-start delimiter {@code :} (0x3A).
 *
 * <p>
 * The ABL eMH1 EVCC wallbox deviates from the Modbus/ASCII specification by
 * sending {@code >} as the response frame-start character instead of the
 * required {@code :}. This class corrects that at the raw byte level so the
 * upstream j2mod parser works without modification.
 *
 * <p>
 * The replacement is safe for all standards-compliant devices because {@code >}
 * (0x3E) is not a valid character in any field of a Modbus/ASCII frame (valid
 * payload characters are {@code 0}–{@code 9}, {@code A}–{@code F}, CR, LF, and
 * {@code :}).
 */
class AblCompatibleSerialConnection extends SerialConnection {

	/** Standard Modbus/ASCII frame-start character. */
	private static final byte STANDARD_FRAME_START = ':'; // 0x3A

	/** Non-standard frame-start character sent by the ABL eMH1. */
	private static final byte ABL_FRAME_START = '>'; // 0x3E

	AblCompatibleSerialConnection(SerialParameters parameters) {
		super(parameters);
	}

	/**
	 * Reads bytes from the serial port, replacing any {@code >} (ABL eMH1
	 * non-standard frame start) with {@code :} (standard Modbus/ASCII frame start).
	 */
	@Override
	public int readBytes(byte[] buffer, int bytesToRead) {
		var result = super.readBytes(buffer, bytesToRead);
		replaceAblFrameStart(buffer, result);
		return result;
	}

	/**
	 * Replaces any ABL eMH1 non-standard frame-start bytes in the buffer with the
	 * standard Modbus/ASCII frame-start byte.
	 *
	 * @param buffer the byte buffer to modify in place
	 * @param count  the number of valid bytes in the buffer
	 */
	static void replaceAblFrameStart(byte[] buffer, int count) {
		for (var i = 0; i < count; i++) {
			if (buffer[i] == ABL_FRAME_START) {
				buffer[i] = STANDARD_FRAME_START;
			}
		}
	}
}
