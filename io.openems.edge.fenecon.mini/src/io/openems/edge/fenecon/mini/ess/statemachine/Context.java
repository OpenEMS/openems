package io.openems.edge.fenecon.mini.ess.statemachine;

import io.openems.edge.fenecon.mini.ess.Config;
import io.openems.edge.fenecon.mini.ess.FeneconMiniEss;

public class Context {
	protected final FeneconMiniEss component;
	protected final Config config;
	protected final int setActivePower;
	protected final int setReactivePower;

	public Context(FeneconMiniEss component, Config config, int setActivePower, int setReactivePower) {
		this.component = component;
		this.config = config;
		this.setActivePower = setActivePower;
		this.setReactivePower = setReactivePower;
	}
}