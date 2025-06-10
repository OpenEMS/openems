package io.openems.edge.ess.power.api;

/**
 * Represents a dummy inverter - e.g. for the two inactive phases of a
 * single-phase Ess.
 */
public class DummyInverter extends Inverter {

	public DummyInverter(String essId, Phase phase) {
		super(essId, phase);
	}

}
