package io.openems.edge.battery.soltaro.single.versionb.statemachine;

import io.openems.edge.battery.soltaro.single.versionb.Config;
import io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionB;

public class Context {
	protected final SingleRackVersionB component;
	protected final Config config;

	public Context(SingleRackVersionB component, Config config) {
		super();
		this.component = component;
		this.config = config;
	}
}