package io.openems.edge.bridge.can.linux;

public enum BaudRate {
	BAUD_125_KBPS(125),
	BAUD_250_KBPS(250),
	BAUD_500_KBPS(500),
	BAUD_1000_KBPS(1000);
	
	private int baud;
	
	BaudRate(int baud) {
		this.baud = baud;
	}
	
	/**
	 * Get the baud rate as integer in kb/s.
	 * @return the baud rate in kb/s.
	 */
	public int asInt() {
		return this.baud;
	}
}
