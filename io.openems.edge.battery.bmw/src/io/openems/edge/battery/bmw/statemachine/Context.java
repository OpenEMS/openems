package io.openems.edge.battery.bmw.statemachine;

import io.openems.edge.battery.bmw.BmwBatteryImpl;
import io.openems.edge.battery.bmw.Config;

public class Context {

	protected final BmwBatteryImpl component;
	protected final Config config;

	public Context(BmwBatteryImpl component, Config config) {
		super();
		this.component = component;
		this.config = config;
	}
}
