package io.openems.edge.goodwe.charger.twostring;

/**
 * Defines the PV-Port of a GoodWe Charger Two-String.
 */
public enum PvPort {

	PV_1(35337, 35345, 35103, 35107), //
	PV_2(35337, 35345, 35107, 35103), //
	PV_3(35338, 35346, 35111, 35115), //
	PV_4(35338, 35346, 35115, 35111), //
	PV_5(35339, 35347, 35304, 35306), //
	PV_6(35339, 35347, 35306, 35304);

	public final int mpptPowerAddress;
	public final int mpptCurrentAddress;
	public final int pvStartAddress;
	public final int relatedPvStartAddress;

	private PvPort(int mpptPowerAddress, int mpptCurrentAddress, int pvStartAddress, int relatedPvStartAddress) {
		this.mpptPowerAddress = mpptPowerAddress;
		this.mpptCurrentAddress = mpptCurrentAddress;
		this.pvStartAddress = pvStartAddress;
		this.relatedPvStartAddress = relatedPvStartAddress;
	}
}
