package io.openems.edge.goodwe.update;

public enum GoodWeFirmwareType {
	DSP("DSP", new byte[] { (byte) 0xFF, 0x01 }), //
	ARM("ARM", new byte[] { (byte) 0xFF, 0x02 }), //
	STS("STS", new byte[] { (byte) 0xFF, 0x09 }), //
	;

	public final String text;
	public final byte[] hexCode;

	private GoodWeFirmwareType(String text, byte[] hexCode) {
		this.text = text;
		this.hexCode = hexCode;
	}

}