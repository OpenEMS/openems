package io.openems.edge.battery.fenecon.home.statemachine;

import io.openems.edge.battery.fenecon.home.Config;
import io.openems.edge.battery.fenecon.home.FeneconHomeBattery;

public class Context {
	protected final FeneconHomeBattery component;
	protected final Config config;

	public Context(FeneconHomeBattery component, Config config) {
		super();
		this.component = component;
		this.config = config;
	}
}