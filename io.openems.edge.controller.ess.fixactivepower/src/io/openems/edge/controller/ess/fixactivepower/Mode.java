package io.openems.edge.controller.ess.fixactivepower;

public enum Mode {

	MANUAL_ON, MANUAL_OFF;

	public static enum Config {
		MANUAL_ON, MANUAL_OFF, SMART;

		public Mode toMode() {
			return switch (this) {
			case MANUAL_ON -> Mode.MANUAL_ON;
			case MANUAL_OFF -> Mode.MANUAL_OFF;
			case SMART -> throw new UnsupportedOperationException();
			};
		}
	}

}