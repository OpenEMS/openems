package io.openems.common.channel;

public enum PersistencePriority {

	VERY_LOW(0), //
	LOW(1), //
	MEDIUM(2), //
	HIGH(3), //
	VERY_HIGH(4), //
	;

	private final int value;

	private PersistencePriority(int value) {
		this.value = value;
	}

	public boolean isAtLeast(PersistencePriority other) {
		return this.value >= other.value;
	}

	public boolean isLowerThan(PersistencePriority other) {
		return this.value < other.value;
	}

}
