package io.openems.edge.ess.mr.gridcon.enums;

/**
 * This enum signalizes the bit position of a command in the control word for a
 * gridcon. The index is based on 0.
 */
public enum PCSControlWordBitPosition {
	PLAY(0), //
	READY(1), //
	ACKNOWLEDGE(2), //
	STOP(3), //
	BLACKSTART_APPROVAL(4), //
	SYNC_APPROVAL(5), //
	ACTIVATE_SHORT_CIRCUIT_HANDLING(6), //
	/**
	 * 1 = current, 0 = voltage
	 */
	MODE_SELECTION(7), //
	TRIGGER_SIA(8), //
	ACTIVATE_HARMONIC_COMPENSATION(9), //
	/**
	 * 1 is the value for disable, 0 is the value for enable
	 */
	DISABLE_IPU_4(28), //
	/**
	 * 1 is the value for disable, 0 is the value for enable
	 */
	DISABLE_IPU_3(29), //
	/**
	 * 1 is the value for disable, 0 is the value for enable
	 */
	DISABLE_IPU_2(30), //
	/**
	 * 1 is the value for disable, 0 is the value for enable
	 */
	DISABLE_IPU_1(31);

	PCSControlWordBitPosition(int value) {
		this.bitPosition = value;
	}

	private int bitPosition;

	public int getBitPosition() {
		return bitPosition;
	}
}