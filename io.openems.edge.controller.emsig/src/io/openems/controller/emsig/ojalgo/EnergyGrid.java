package io.openems.controller.emsig.ojalgo;

import org.ojalgo.optimisation.Variable;

public class EnergyGrid {

	public static class Buy {
		public Variable power = null;
	}

	public static class Sell {
		public Variable power = null;
	}

	public final Sell sell = new Sell();
	public final Buy buy = new Buy();

	public Variable power;
	public Variable isBuy;

	public EnergyGrid() {

	}
}
