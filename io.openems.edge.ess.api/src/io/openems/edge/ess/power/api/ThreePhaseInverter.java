package io.openems.edge.ess.power.api;

import io.openems.edge.common.type.Phase.SingleOrAllPhase;

/**
 * Represents a three-phase symmetric inverter.
 */
public class ThreePhaseInverter extends Inverter {

	public ThreePhaseInverter(String essId) {
		super(essId, SingleOrAllPhase.ALL);
	}

}
