package io.openems.impl.controller.supplybusswitch;

import java.util.List;

public class SupplyBusException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = -3868360492918860902L;

	public final List<Ess> activeEss;
	public final Supplybus supplybus;

	public SupplyBusException(String string, List<Ess> activeEss, Supplybus supplybus) {
		super(string);
		this.activeEss = activeEss;
		this.supplybus = supplybus;
	}

}
