package io.openems.edge.ess.power.api;

import io.openems.edge.common.type.Phase.SingleOrAllPhase;

/**
 * Represents a dummy inverter - e.g. for the two inactive phases of a
 * single-phase Ess.
 */
public class DummyInverter extends Inverter {

	public DummyInverter(String essId, SingleOrAllPhase phase) {
		super(essId, phase);
	}

}
