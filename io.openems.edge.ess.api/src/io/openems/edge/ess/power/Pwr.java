package io.openems.edge.ess.power;

enum Pwr {
	ACTIVE("Active Power", 0), REACTIVE("Reactive Power", 1);

	private final String note;
	private final int offset;

	Pwr(String note, int offset) {
		this.note = note;
		this.offset = offset;
	}

	@Override
	public String toString() {
		return this.note;
	}

	public int getOffset() {
		return offset;
	}
}