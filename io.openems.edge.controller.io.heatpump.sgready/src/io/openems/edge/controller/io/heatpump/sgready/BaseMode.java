package io.openems.edge.controller.io.heatpump.sgready;

public enum BaseMode {
	AUTOMATIC, LOCK, REGULAR, RECOMMENDATION, FORCE_ON;

	/**
	 * Gets the corresponding {@link Status} for this {@link BaseMode}.
	 * 
	 * @return the corresponding {@link Status} for this {@link BaseMode}
	 */
	public Status getStatus() {
		return switch (this) {
		case LOCK -> Status.LOCK;
		case REGULAR -> Status.REGULAR;
		case RECOMMENDATION -> Status.RECOMMENDATION;
		case FORCE_ON -> Status.FORCE_ON;
		case AUTOMATIC -> Status.UNDEFINED;
		};
	}

	/**
	 * Gets the corresponding {@link Mode} for this {@link BaseMode}.
	 * 
	 * @return the corresponding {@link Mode} for this {@link BaseMode}
	 */
	public Mode getMode() {
		return switch (this) {
		case AUTOMATIC -> Mode.AUTOMATIC;
		case LOCK, REGULAR, RECOMMENDATION, FORCE_ON -> Mode.MANUAL;
		};
	}
}
