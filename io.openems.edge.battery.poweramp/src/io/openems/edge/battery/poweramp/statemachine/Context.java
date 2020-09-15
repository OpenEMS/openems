package io.openems.edge.battery.poweramp.statemachine;

import io.openems.edge.battery.poweramp.Config;
import io.openems.edge.battery.poweramp.PowerAmpATL;

public class Context {
	protected final PowerAmpATL component;
	protected final Config config;

	public Context(PowerAmpATL component, Config config) {
		super();
		this.component = component;
		this.config = config;
	}
}