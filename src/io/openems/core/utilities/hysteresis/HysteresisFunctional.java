package io.openems.core.utilities.hysteresis;

public interface HysteresisFunctional {

	void function(HysteresisState state, double multiplier);
}
