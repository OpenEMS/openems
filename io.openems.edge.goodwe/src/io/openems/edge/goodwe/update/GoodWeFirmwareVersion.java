package io.openems.edge.goodwe.update;

public enum GoodWeFirmwareVersion {
	DSP("DSP", new byte[] { (byte) 0xFF, 0x01 }), //
	ARM("ARM", new byte[] { (byte) 0xFF, 0x02 });

	public final String text;
	public final byte[] hexCode;

	private GoodWeFirmwareVersion(String text, byte[] hexCode) {
		this.text = text;
		this.hexCode = hexCode;
	}

}