package io.openems.edge.fenecon.mini.ess.statemachine;

import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.fenecon.mini.ess.Config;
import io.openems.edge.fenecon.mini.ess.FeneconMiniEss;

public class Context extends AbstractContext<FeneconMiniEss> {

	protected final Config config;
	protected final int setActivePower;
	protected final int setReactivePower;

	public Context(FeneconMiniEss parent, Config config, int setActivePower, int setReactivePower) {
		super(parent);
		this.config = config;
		this.setActivePower = setActivePower;
		this.setReactivePower = setReactivePower;
	}
}