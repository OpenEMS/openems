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

	/**
	 * Is this {@link PersistencePriority} at least as high as the given
	 * {@link PersistencePriority}?.
	 *
	 * @param other the given {@link PersistencePriority}
	 * @return true if this is equal or higher than other
	 */
	public boolean isAtLeast(PersistencePriority other) {
		return this.value >= other.value;
	}

	/**
	 * Is this {@link PersistencePriority} at lower than the given
	 * {@link PersistencePriority}?.
	 *
	 * @param other the given {@link PersistencePriority}
	 * @return true if this is strictly lower than other
	 */
	public boolean isLowerThan(PersistencePriority other) {
		return this.value < other.value;
	}

}
