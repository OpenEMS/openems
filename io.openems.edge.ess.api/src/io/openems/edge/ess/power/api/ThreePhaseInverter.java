package io.openems.edge.ess.power.api;

/**
 * Represents a three-phase symmetric inverter.
 */
public class ThreePhaseInverter extends Inverter {

	public ThreePhaseInverter(String essId) {
		super(essId, Phase.ALL);
	}

}
