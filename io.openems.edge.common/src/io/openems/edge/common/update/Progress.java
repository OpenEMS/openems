package io.openems.edge.common.update;

public record Progress(int percentage, String title) {

	public Progress {
		assertPercentage(percentage);
	}

	public Progress(int percentage) {
		this(percentage, null);
	}

	/**
	 * Checks if the percentage are in a valid range.
	 *
	 * @param percentage the percentage to validate
	 */
	public static void assertPercentage(int percentage) {
		if (percentage < 0 || percentage > 100) {
			throw new IllegalArgumentException("Percentage must be >= 0 and <= 100.");
		}
	}

	@Override
	public String toString() {
		return String.format("%3d", this.percentage()) + "%" + (this.title() != null ? " " + this.title() : "");
	}
}
