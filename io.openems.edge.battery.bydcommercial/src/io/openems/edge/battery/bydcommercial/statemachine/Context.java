package io.openems.edge.battery.bydcommercial.statemachine;

import io.openems.edge.battery.bydcommercial.BatteryBoxC130;
import io.openems.edge.battery.bydcommercial.Config;

public class Context {
	protected final BatteryBoxC130 component;
	protected final Config config;

	public Context(BatteryBoxC130 component, Config config) {
		super();
		this.component = component;
		this.config = config;
	}
}