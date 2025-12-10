package io.openems.edge.phoenixcontact.plcnext.gds.enums;

public enum PlcNextGdsDataType {
	FLOAT32(0, null), //
	FLOAT32_ARRAY_3(3, FLOAT32), //
	FLOAT64(0, null);

	private final int memberCount;
	private final PlcNextGdsDataType memberType;

	private PlcNextGdsDataType(int memberCount, PlcNextGdsDataType memberType) {
		this.memberCount = memberCount;
		this.memberType = memberType;
	}

	public boolean isArray() {
		return this.memberCount > 0;
	}

	public int getMemberCount() {
		return this.memberCount;
	}

	public PlcNextGdsDataType getMemberType() {
		return this.memberType;
	}
}
