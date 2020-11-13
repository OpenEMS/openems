package io.openems.edge.ess.power.api;

/**
 * Represents a three-phase symmetric inverter.
 */
public class ThreePhaseInverter extends Inverter {

	public ThreePhaseInverter(String essId, boolean isHybridEss) {
		super(essId, Phase.ALL, isHybridEss);
	}

}
