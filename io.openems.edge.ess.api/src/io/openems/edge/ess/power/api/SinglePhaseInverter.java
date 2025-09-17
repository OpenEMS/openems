package io.openems.edge.ess.power.api;

import io.openems.edge.common.type.Phase.SinglePhase;

/**
 * Represents a Single-Phase Inverter, e.g. a single-phase Ess or part of a
 * three-phase Ess.
 */
public class SinglePhaseInverter extends Inverter {

	public SinglePhaseInverter(String essId, SinglePhase singlePhase) {
		super(essId, singlePhase.toSingleOrAllPhase);
	}
}
