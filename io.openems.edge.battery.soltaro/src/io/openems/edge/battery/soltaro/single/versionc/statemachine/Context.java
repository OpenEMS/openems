package io.openems.edge.battery.soltaro.single.versionc.statemachine;

import io.openems.edge.battery.soltaro.single.versionc.Config;
import io.openems.edge.battery.soltaro.single.versionc.SingleRackVersionC;

public class Context {
	protected final SingleRackVersionC component;
	protected final Config config;

	public Context(SingleRackVersionC component, Config config) {
		super();
		this.component = component;
		this.config = config;
	}
}