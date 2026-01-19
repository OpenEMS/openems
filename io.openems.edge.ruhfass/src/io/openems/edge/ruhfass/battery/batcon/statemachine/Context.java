package io.openems.edge.ruhfass.battery.batcon.statemachine;

import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.ruhfass.battery.batcon.Batcon;
import io.openems.edge.ruhfass.battery.batcon.Config;

public class Context extends AbstractContext<Batcon> {
	protected final Batcon component;
	protected final Config config;

	public Context(Batcon component, Config config) {
		super(component);
		this.component = component;
		this.config = config;
	}
}