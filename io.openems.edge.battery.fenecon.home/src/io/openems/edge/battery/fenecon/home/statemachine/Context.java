package io.openems.edge.battery.fenecon.home.statemachine;

import io.openems.edge.battery.fenecon.home.FeneconHomeBattery;

public class Context {
	protected final FeneconHomeBattery component;

	public Context(FeneconHomeBattery component) {
		super();
		this.component = component;
	}
}