package io.openems.edge.controller.ess.timeofusetariff;

public enum Mode {
	OFF, AUTOMATIC,

	// Force Mode for debugging
	FORCE_DELAY_DISCHARGE, FORCE_CHARGE_GRID;
}
