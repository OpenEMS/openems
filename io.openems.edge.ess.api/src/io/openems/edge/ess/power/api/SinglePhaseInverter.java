package io.openems.edge.ess.power.api;

/**
 * Represents a Single-Phase Inverter, e.g. a single-phase Ess or part of a
 * three-phase Ess.
 */
public class SinglePhaseInverter extends Inverter {

	public SinglePhaseInverter(String essId, Phase phase) {
		super(essId, phase);
	}

}
