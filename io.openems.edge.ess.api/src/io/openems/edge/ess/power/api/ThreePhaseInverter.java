package io.openems.edge.ess.power.api;

public class ThreePhaseInverter extends Inverter {

	public ThreePhaseInverter(String essId) {
		super(essId, Phase.ALL);
	}

}
