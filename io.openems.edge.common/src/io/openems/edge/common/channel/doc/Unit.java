package io.openems.edge.common.channel.doc;

public enum Unit {
	/* No Unit */
	NONE,
	/* Generic */
	PERCENT,
	/* Active Power */
	W,
	/* Reactive Power */
	VAR,
	/* Apparent Power */
	VA,
	/* Voltage */
	VOLT, MILLIVOLT(VOLT, -3);

	private final Unit baseUnit;
	private final int scaleFactor;

	private Unit() {
		this.baseUnit = null;
		this.scaleFactor = 0;
	}

	private Unit(Unit baseUnit, int scaleFactor) {
		this.baseUnit = baseUnit;
		this.scaleFactor = scaleFactor;
	}

	public Unit getBaseUnit() {
		return baseUnit;
	}

	public int getScaleFactor() {
		return scaleFactor;
	}
}
