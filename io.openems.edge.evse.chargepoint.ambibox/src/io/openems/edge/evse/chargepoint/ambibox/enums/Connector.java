package io.openems.edge.evse.chargepoint.ambibox.enums;

/**
 * Physical connector of the Ambibox charger; defines the Modbus register
 * block of the respective EV charger.
 */
public enum Connector {

	CONNECTOR_1(4000, 3000), //
	CONNECTOR_2(4200, 3100), //
	CONNECTOR_3(4400, 3200), //
	CONNECTOR_4(4600, 3300), //
	CONNECTOR_5(4800, 3400), //
	CONNECTOR_6(5000, 3500), //
	CONNECTOR_7(5200, 3600), //
	CONNECTOR_8(5400, 3700), //
	CONNECTOR_9(5600, 3800), //
	CONNECTOR_10(5800, 3900);

	/** Base address of the Input Register block. */
	public final int inputBaseAddress;
	/** Base address of the Holding Register block. */
	public final int holdingBaseAddress;

	private Connector(int inputBaseAddress, int holdingBaseAddress) {
		this.inputBaseAddress = inputBaseAddress;
		this.holdingBaseAddress = holdingBaseAddress;
	}
}
